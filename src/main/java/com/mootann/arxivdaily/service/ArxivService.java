package com.mootann.arxivdaily.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mootann.arxivdaily.client.ArxivClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.converter.ArxivPaperStructMapper;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivPaperQueryRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.repository.dto.CategoryCountDTO;
import com.mootann.arxivdaily.repository.dto.PageCacheDTO;
import com.mootann.arxivdaily.repository.model.ArxivPaper;
import com.mootann.arxivdaily.repository.mapper.ArxivPaperMapper;
import com.mootann.arxivdaily.util.GitHubUrlExtractor;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * arXiv论文服务
 */
@Slf4j
@Service
@AllArgsConstructor
public class ArxivService {

    private final ArxivClient arxivClient;

    private final ArxivPaperMapper arxivPaperMapper;

    private final ArxivPaperStructMapper arxivPaperStructMapper;

    private final RedisClient redisClient;

    private final ObjectMapper objectMapper;
    
    // 循环依赖，使用@Autowired延迟注入或setter注入
    @Autowired
    private InteractionService interactionService;


    /**
     * 搜索论文（支持分页获取超过100条结果）
     * @param request 搜索请求
     * @return 搜索结果
     */
    public ArxivSearchResponse searchPapers(ArxivSearchRequest request) {
        log.info("搜索论文，关键词: {}", request.getQuery());
        ArxivSearchResponse response = arxivClient.searchPapersWithPagination(request);

        if (response != null && response.getPapers() != null) {
            int savedCount = savePapersToDatabase(response.getPapers());
            log.info("搜索结果中保存了 {} 篇新论文到数据库", savedCount);
        }

        return response;
    }

    public List<ArxivPaper> getPapersByArxivIds(List<String> arxivIds) {
        if (arxivIds == null || arxivIds.isEmpty()) {
            return new ArrayList<>();
        }
        QueryWrapper<ArxivPaper> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("arxiv_id", arxivIds);
        List<ArxivPaper> papers = arxivPaperMapper.selectList(queryWrapper);
        papers.forEach(interactionService::fillInteractionInfo);
        return papers;
    }

    /**
     * 按关键词搜索论文
     * @param keyword 关键词
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByKeyword(String keyword) {
        String query = "all:" + keyword;
        ArxivSearchRequest request = new ArxivSearchRequest(query, null, null, null, null, "api", null);
        return searchPapers(request);
    }

    /**
     * 搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchQuery(List<String> categories, String startDate, String endDate) {
        log.info("搜索论文: {}, 日期范围: {} 到 {}", categories, startDate, endDate);
        ArxivSearchResponse response = arxivClient.searchQuery(categories, startDate, endDate);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 保存论文列表到数据库
     * @param papers 论文列表
     * @return 保存的论文数量
     */
    @Transactional
    public int savePapersToDatabase(List<ArxivPaperDTO> papers) {
        if (papers == null || papers.isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        List<ArxivPaper> papersToSave = new ArrayList<>();

        for (ArxivPaperDTO dto : papers) {
            if (!arxivPaperMapper.existsByArxivId(dto.getArxivId())) {
                ArxivPaper paper = arxivPaperStructMapper.toEntity(dto);
                
                String githubUrl = GitHubUrlExtractor.extractGitHubUrl(dto.getSummary());
                paper.setGithubUrl(githubUrl);
                
                if (githubUrl != null) {
                    log.info("从摘要中提取到GitHub URL: arxivId={}, githubUrl={}", dto.getArxivId(), githubUrl);
                }
                
                papersToSave.add(paper);
                savedCount++;
            }
        }

        if (!papersToSave.isEmpty()) {
            for (ArxivPaper paper : papersToSave) {
                arxivPaperMapper.insert(paper);
            }
            savedCount = papersToSave.size();
            log.info("成功保存 {} 篇论文到数据库", savedCount);

            // 同步到Redis缓存
            for (ArxivPaperDTO dto : papers) {
                String cacheKey = RedisClient.ARXIV_PAPERS_PREFIX + dto.getArxivId();
                redisClient.set(cacheKey, dto, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            }
            log.info("成功同步 {} 篇论文到Redis缓存", papers.size());

        }

        return savedCount;
    }

    /**
     * 统计数据库中各分类的论文数量
     * @return 分类和数量列表
     */
    public List<CategoryCountDTO> getCategoryCountsFromDatabase() {
        String cacheKey = RedisClient.PAPERS_PREFIX + "category_counts_dto";
        
        // 尝试从缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof List) {
            try {
                // 如果是 LinkedHashMap 列表（Redis 反序列化常见情况），转换为 DTO
                List<?> list = (List<?>) cachedValue;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    return list.stream()
                            .map(m -> objectMapper.convertValue(m, CategoryCountDTO.class))
                            .collect(Collectors.toList());
                }
                return (List<CategoryCountDTO>) cachedValue;
            } catch (Exception e) {
                log.warn("从缓存转换 CategoryCountDTO 失败，将重新查询数据库", e);
            }
        }

        log.info("从数据库查询分类统计数据");
        // 获取各分类统计
        List<CategoryCountDTO> result = new ArrayList<>();
        // 使用MySQL的COUNT查询
        Long totalCount = arxivPaperMapper.selectCount(null);
        
        // 获取各分类的数量
        List<ArxivPaper> allPapers = arxivPaperMapper.selectList(null);
        Map<String, Long> categoryMap = allPapers.stream()
                .collect(Collectors.groupingBy(p -> p.getPrimaryCategory() != null ? p.getPrimaryCategory() : "UNCATEGORIZED", Collectors.counting()));
        
        for (Map.Entry<String, Long> entry : categoryMap.entrySet()) {
            result.add(new CategoryCountDTO(entry.getKey(), entry.getValue()));
        }
        
        // 获取总数预估
        Long totalEstimate = totalCount;
        
        // 如果预估值为 0 且分类统计有数据，尝试计算分类总和作为预估值
        if ((totalEstimate == null || totalEstimate <= 0) && !result.isEmpty()) {
            totalEstimate = result.stream().mapToLong(CategoryCountDTO::getCount).sum();
        }
        
        // 将总数作为一个特殊的分类 "All" 加入结果
        result.add(new CategoryCountDTO("All", totalEstimate != null ? totalEstimate : 0L));
        
        // 存入缓存，过期时间1小时
        redisClient.set(cacheKey, result, 1, TimeUnit.HOURS);
        
        return result;
    }

    /**
     * 根据日期范围统计数据库中各分类的论文数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类和数量列表
     */
    public List<CategoryCountDTO> getCategoryCountsFromDatabase(LocalDate startDate, LocalDate endDate) {
        String cacheKey = RedisClient.PAPERS_PREFIX + "category_counts_dto:" + startDate + ":" + endDate;
        
        // 尝试从缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof List) {
            try {
                // 如果是 LinkedHashMap 列表（Redis 反序列化常见情况），转换为 DTO
                List<?> list = (List<?>) cachedValue;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    return list.stream()
                            .map(m -> objectMapper.convertValue(m, CategoryCountDTO.class))
                            .collect(Collectors.toList());
                }
                return (List<CategoryCountDTO>) cachedValue;
            } catch (Exception e) {
                log.warn("从缓存转换 CategoryCountDTO 失败，将重新查询数据库", e);
            }
        }

        log.info("从数据库查询日期范围内的分类统计数据: {} - {}", startDate, endDate);
        // 获取各分类统计
        List<ArxivPaper> papersInRange = arxivPaperMapper.selectList(
                new QueryWrapper<ArxivPaper>().between("published_date", startDate, endDate));
        Map<String, Long> categoryMap = papersInRange.stream()
                .collect(Collectors.groupingBy(p -> p.getPrimaryCategory() != null ? p.getPrimaryCategory() : "UNCATEGORIZED", Collectors.counting()));
        
        List<CategoryCountDTO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : categoryMap.entrySet()) {
            result.add(new CategoryCountDTO(entry.getKey(), entry.getValue()));
        }
        
        // 计算总数
        long totalCount = result.stream().mapToLong(CategoryCountDTO::getCount).sum();
        
        // 将总数作为一个特殊的分类 "All" 加入结果
        result.add(new CategoryCountDTO("All", totalCount));
        
        // 存入缓存，过期时间1小时
        redisClient.set(cacheKey, result, 1, TimeUnit.HOURS);
        
        return result;
    }

    /**
     * 根据arXiv ID获取论文
     * @param arxivId arXiv ID
     * @return 论文信息
     */
    public ArxivPaper getPaperFromDatabase(String arxivId) {
        log.info("从数据库获取论文: {}", arxivId);
        ArxivPaper paper = arxivPaperMapper.findByArxivId(arxivId);
        if (paper != null) {
            interactionService.fillInteractionInfo(paper);
        }
        return paper;
    }

    /**
     * 论文分页查询接口
     * 支持根据分类、关键词、日期范围等条件进行分页查询
     * @param request 查询请求参数
     * @return 论文分页结果
     */
    public IPage<ArxivPaper> searchQuery(ArxivPaperQueryRequest request) {
        // 参数处理
        List<String> categoryList = request.getCategory();
        String keyword = request.getKeyword();
        String startDate = request.getStartDate();
        String endDate = request.getEndDate();
        Integer page = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        Integer size = request.getSize() != null && request.getSize() > 0 ? request.getSize() : 10;
        Boolean hasGithub = request.getHasGithub();

        // 构建缓存key
        StringBuilder cacheKeyBuilder = new StringBuilder(RedisClient.PAPERS_PREFIX);
        if (categoryList != null && !categoryList.isEmpty()) {
            cacheKeyBuilder.append("category:").append(String.join(",", categoryList)).append(":");
        }
        if (keyword != null && !keyword.isEmpty()) {
            cacheKeyBuilder.append("search:").append(keyword).append(":");
        }
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            cacheKeyBuilder.append("date:").append(startDate).append(":").append(endDate).append(":");
        }
        if (hasGithub != null) {
            cacheKeyBuilder.append("github:").append(hasGithub).append(":");
        }
        cacheKeyBuilder.append("page:").append(page).append(":size:").append(size);
        String cacheKey = cacheKeyBuilder.toString();

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO<?> cacheDTO) {
            log.info("从Redis缓存获取查询结果，缓存key: {}", cacheKey);
            IPage<ArxivPaper> pageResult = convertCacheDTOToIPage(cacheDTO);
            if (pageResult.getRecords() != null) {
                pageResult.getRecords().forEach(interactionService::fillInteractionInfo);
            }
            return pageResult;
        }

        // 构建查询条件
        QueryWrapper<ArxivPaper> queryWrapper = new QueryWrapper<>();

        // 处理分类条件（支持多分类查询）
        if (categoryList != null && !categoryList.isEmpty()) {
            // 检查是否包含UNCATEGORIZED
            boolean hasUncategorized = categoryList.contains("UNCATEGORIZED");
            List<String> normalCategories = categoryList.stream()
                    .filter(c -> !"UNCATEGORIZED".equals(c))
                    .collect(Collectors.toList());

            if (hasUncategorized && normalCategories.isEmpty()) {
                // 只有UNCATEGORIZED，查询primary_category为NULL的记录
                queryWrapper.isNull("primary_category");
            } else if (hasUncategorized && !normalCategories.isEmpty()) {
                // 同时包含UNCATEGORIZED和其他分类
                queryWrapper.and(w -> w.isNull("primary_category").or().in("primary_category", normalCategories));
            } else {
                // 只有普通分类
                queryWrapper.in("primary_category", normalCategories);
            }
        }

        // 处理关键词条件
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(w -> w.like("title", keyword).or().like("summary", keyword));
        }

        // 处理日期范围条件
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                queryWrapper.between("published_date", start, end);
            } catch (Exception e) {
                log.warn("日期格式解析失败，startDate: {}, endDate: {}", startDate, endDate);
            }
        }

        // 处理GitHub筛选条件
        if (hasGithub != null) {
            if (hasGithub) {
                queryWrapper.isNotNull("github_url");
            } else {
                queryWrapper.isNull("github_url");
            }
        }

        // 按发布日期倒序
        queryWrapper.orderByDesc("published_date");

        log.info("从数据库执行查询，分类: {}, 关键词: {}, 日期范围: {}至{}, hasGithub: {}, 页码: {}, 每页数量: {}",
                categoryList, keyword, startDate, endDate, hasGithub, page, size);

        // 执行分页查询
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        IPage<ArxivPaper> result = arxivPaperMapper.selectPage(pageObj, queryWrapper);

        // 只有当查询结果不为空时才缓存
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertIPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("查询结果已缓存到Redis");
        } else {
            log.info("查询结果为空，不缓存");
        }

        if (result.getRecords() != null) {
            result.getRecords().forEach(interactionService::fillInteractionInfo);
        }

        return result;
    }
    
    /**
     * 将IPage对象转换为PageCacheDTO
     * @param page IPage对象
     * @return PageCacheDTO对象
     */
    private PageCacheDTO<ArxivPaper> convertIPageToCacheDTO(IPage<ArxivPaper> page) {
        return new PageCacheDTO<>(
            page.getRecords(),
            (int) page.getCurrent(),
            (int) page.getSize(),
            page.getTotal(),
            (int) page.getPages()
        );
    }

    /**
     * 将PageCacheDTO转换为IPage对象
     * @param cacheDTO PageCacheDTO对象
     * @return IPage对象
     */
    private IPage<ArxivPaper> convertCacheDTOToIPage(PageCacheDTO<?> cacheDTO) {
        // 使用ObjectMapper将内容转换为ArxivPaper列表
        List<ArxivPaper> content = cacheDTO.getContent().stream()
            .map(item -> {
                if (item instanceof ArxivPaper) {
                    return (ArxivPaper) item;
                } else {
                    // 使用ObjectMapper进行类型转换
                    return objectMapper.convertValue(item, ArxivPaper.class);
                }
            })
            .collect(Collectors.toList());

        // 创建IPage对象
        Page<ArxivPaper> page = new Page<>(cacheDTO.getPage(), cacheDTO.getSize(), cacheDTO.getTotalElements());
        page.setRecords(content);
        return page;
    }
}