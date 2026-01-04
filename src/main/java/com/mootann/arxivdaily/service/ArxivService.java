package com.mootann.arxivdaily.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mootann.arxivdaily.client.ArxivClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.converter.ArxivPaperStructMapper;
import com.mootann.arxivdaily.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.dto.CategoryCountDTO;
import com.mootann.arxivdaily.dto.PageCacheDTO;
import com.mootann.arxivdaily.model.ArxivPaper;
import com.mootann.arxivdaily.repository.ArxivPaperRepository;
import com.mootann.arxivdaily.util.GitHubUrlExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ArxivService {

    @Autowired
    private ArxivClient arxivClient;

    @Autowired
    private ArxivPaperRepository arxivPaperRepository;

    @Autowired
    private ArxivPaperStructMapper arxivPaperStructMapper;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 根据arXiv ID获取论文
     * @param arxivId arXiv ID
     * @return 论文信息
     */
    public ArxivPaperDTO getPaperById(String arxivId) {
        log.info("获取论文信息: {}", arxivId);

        // 先从Redis缓存获取
        String cacheKey = RedisClient.ARXIV_PAPERS_PREFIX + arxivId;
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof ArxivPaperDTO) {
            log.info("从Redis缓存获取论文: {}", arxivId);
            return (ArxivPaperDTO) cachedValue;
        }

        // 尝试从数据库获取（包含GitHub URL）
        ArxivPaper paperEntity = arxivPaperRepository.findByArxivId(arxivId).orElse(null);
        if (paperEntity != null) {
            log.info("从数据库获取论文: {}", arxivId);
            ArxivPaperDTO paper = arxivPaperStructMapper.toDto(paperEntity);
            // 保存到Redis缓存，过期时间1天
            redisClient.set(cacheKey, paper, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            return paper;
        }

        // 数据库未找到，调用arxivClient获取
        ArxivPaperDTO paper = arxivClient.getPaperById(arxivId);

        if (paper != null) {
            savePapersToDatabase(List.of(paper));
            
            // 重新从数据库获取以包含提取的GitHub URL
            paperEntity = arxivPaperRepository.findByArxivId(arxivId).orElse(null);
            if (paperEntity != null) {
                paper = arxivPaperStructMapper.toDto(paperEntity);
            }
            
            // 保存到Redis缓存，过期时间1天
            redisClient.set(cacheKey, paper, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            log.info("论文已缓存到Redis: {}", arxivId);
        }

        return paper;
    }
    
    /**
     * 批量获取论文信息
     * @param arxivIds arXiv ID列表
     * @return 论文信息列表
     */
    public List<ArxivPaperDTO> getPapersByIds(List<String> arxivIds) {
        log.info("批量获取论文信息，数量: {}", arxivIds.size());

        List<ArxivPaperDTO> result = new ArrayList<>();
        List<String> uncachedIds = new ArrayList<>();

        // 先从Redis批量获取
        for (String arxivId : arxivIds) {
            String cacheKey = RedisClient.ARXIV_PAPERS_PREFIX + arxivId;
            Object cachedValue = redisClient.get(cacheKey);
            if (cachedValue instanceof ArxivPaperDTO) {
                result.add((ArxivPaperDTO) cachedValue);
            } else {
                uncachedIds.add(arxivId);
            }
        }

        log.info("Redis缓存命中: {}, 未命中: {}", result.size(), uncachedIds.size());

        // 批量获取未缓存的论文
        if (!uncachedIds.isEmpty()) {
            List<ArxivPaperDTO> uncachedPapers = arxivClient.getPapersByIds(uncachedIds);

            if (uncachedPapers != null && !uncachedPapers.isEmpty()) {
                savePapersToDatabase(uncachedPapers);

                // 保存到Redis缓存
                for (ArxivPaperDTO paper : uncachedPapers) {
                    String cacheKey = RedisClient.ARXIV_PAPERS_PREFIX + paper.getArxivId();
                    redisClient.set(cacheKey, paper, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
                }

                result.addAll(uncachedPapers);
            }
        }

        return result;
    }
    
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
    
    /**
     * 按分类搜索最新论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategory(String category, Integer maxResults, Integer page) {
        String query = "cat:" + category;
        int startIndex = (page - 1) * maxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, String.valueOf(startIndex), "submittedDate", "descending", null, null);
        return searchPapers(request);
    }

    /**
     * 按关键词搜索论文
     * @param keyword 关键词
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByKeyword(String keyword, Integer maxResults, Integer page) {
        String query = "all:" + keyword;
        int startIndex = (page - 1) * maxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, String.valueOf(startIndex), null, null, "api", null);
        return searchPapers(request);
    }
    
    /**
     * 按作者搜索论文
     * @param author 作者姓名
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByAuthor(String author, Integer maxResults) {
        String query = "au:" + author;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, "api", null);
        return searchPapers(request);
    }

    /**
     * 按分类和日期范围搜索论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndDateRange(String category, String startDate, String endDate, Integer maxResults, Integer page) {
        log.info("按分类和日期范围搜索论文: {}, 日期范围: {} 到 {}, 页码: {}", category, startDate, endDate, page);
        ArxivSearchResponse response = arxivClient.searchByCategoryAndDateRange(category, startDate, endDate, maxResults, page);

        if (response != null && response.getPapers() != null) {
            int savedCount = savePapersToDatabase(response.getPapers());
            log.info("保存了 {} 篇新论文到数据库", savedCount);
        }

        return response;
    }

    /**
     * 按分类和日期范围搜索论文（使用默认结果数）
     * @param category 分类，例如：cs.AI, cs.LG
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndDateRange(String category, String startDate, String endDate) {
        return searchByCategoryAndDateRange(category, startDate, endDate, 10, 1);
    }

    /**
     * 按分类和指定日期之后搜索论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryFromDate(String category, String startDate, Integer maxResults) {
        log.info("按分类和日期之后搜索论文: {}, 日期从: {}", category, startDate);
        ArxivSearchResponse response = arxivClient.searchByCategoryFromDate(category, startDate, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 按分类和指定日期之前搜索论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryToDate(String category, String endDate, Integer maxResults) {
        log.info("按分类和日期之前搜索论文: {}, 日期到: {}", category, endDate);
        ArxivSearchResponse response = arxivClient.searchByCategoryToDate(category, endDate, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 根据日期范围搜索论文
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDateRange(String startDate, String endDate, Integer maxResults, Integer page) {
        log.info("按日期范围搜索论文: {} 到 {}, 页码: {}", startDate, endDate, page);
        ArxivSearchResponse response = arxivClient.searchByDateRange(startDate, endDate, maxResults, page);

        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }

        return response;
    }

    /**
     * 根据日期范围搜索论文（使用默认结果数）
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDateRange(String startDate, String endDate) {
        return searchByDateRange(startDate, endDate, 10, 1);
    }

    /**
     * 根据指定日期搜索论文（精确日期）
     * @param date 日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDate(String date, Integer maxResults) {
        log.info("按日期搜索论文: {}", date);
        ArxivSearchResponse response = arxivClient.searchByDate(date, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 获取最近N天的论文
     * @param days 天数
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchRecentPapers(int days, Integer maxResults) {
        log.info("获取最近 {} 天的论文", days);
        ArxivSearchResponse response = arxivClient.searchRecentPapers(days, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 获取最近N天指定分类的论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param days 天数
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchRecentPapersByCategory(String category, int days, Integer maxResults, Integer page) {
        log.info("获取最近 {} 天 {} 分类的论文, 页码: {}", days, category, page);
        ArxivSearchResponse response = arxivClient.searchRecentPapersByCategory(category, days, maxResults, page);

        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }

        return response;
    }

    /**
     * 按多个分类搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByMultipleCategories(List<String> categories, Integer maxResults) {
        log.info("按多个分类搜索论文: {}", categories);
        ArxivSearchResponse response = arxivClient.searchByMultipleCategories(categories, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 按多个分类和日期范围搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByMultipleCategoriesAndDateRange(List<String> categories, String startDate, String endDate, Integer maxResults) {
        log.info("按多个分类和日期范围搜索论文: {}, 日期范围: {} 到 {}", categories, startDate, endDate);
        ArxivSearchResponse response = arxivClient.searchByMultipleCategoriesAndDateRange(categories, startDate, endDate, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 按分类和关键词搜索论文
     * @param category 分类，例如：cs.AI, cs.LG
     * @param keyword 关键词
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndKeyword(String category, String keyword, Integer maxResults) {
        log.info("按分类和关键词搜索论文: {}, 关键词: {}", category, keyword);
        ArxivSearchResponse response = arxivClient.searchByCategoryAndKeyword(category, keyword, maxResults);
        
        if (response != null && response.getPapers() != null) {
            savePapersToDatabase(response.getPapers());
        }
        
        return response;
    }

    /**
     * 保存论文列表到数据库（避免重复）
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
            if (!arxivPaperRepository.existsByArxivId(dto.getArxivId())) {
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
            arxivPaperRepository.saveAll(papersToSave);
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

    // ==================== 数据库查询方法 ====================

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
        List<Object[]> counts = arxivPaperRepository.countByCategory();
        List<CategoryCountDTO> result = counts.stream()
                .map(obj -> new CategoryCountDTO((String) obj[0], (Long) obj[1]))
                .collect(Collectors.toList());
        
        // 使用 pg_class.reltuples 快速获取总数预估
        Long totalEstimate = arxivPaperRepository.estimateTotalCount();
        
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
        List<Object[]> counts = arxivPaperRepository.countByCategoryAndPublishedDateBetween(startDate, endDate);
        List<CategoryCountDTO> result = counts.stream()
                .map(obj -> new CategoryCountDTO((String) obj[0], (Long) obj[1]))
                .collect(Collectors.toList());
        
        // 计算总数
        long totalCount = result.stream().mapToLong(CategoryCountDTO::getCount).sum();
        
        // 将总数作为一个特殊的分类 "All" 加入结果
        result.add(new CategoryCountDTO("All", totalCount));
        
        // 存入缓存，过期时间1小时
        redisClient.set(cacheKey, result, 1, TimeUnit.HOURS);
        
        return result;
    }

    /**
     * 分页查询所有论文
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersFromDatabase(int page, int size) {
        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("page:%d:size:%d", page, size);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取论文列表，页码: {}, 每页数量: {}", page, size);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            // 将缓存的数据转换为Page对象
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询论文，页码: {}, 每页数量: {}", page, size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result = arxivPaperRepository.findAll(pageable);

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("论文列表已缓存到Redis，页码: {}", page);
        } else {
            log.info("查询结果为空，不缓存，页码: {}", page);
        }

        return result;
    }

    /**
     * 根据关键词查询论文（标题或摘要）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> searchPapersFromDatabase(String keyword, int page, int size) {
        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("search:%s:page:%d:size:%d", keyword, page, size);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取搜索结果，关键词: {}, 页码: {}", keyword, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库搜索论文，关键词: {}, 页码: {}, 每页数量: {}", keyword, page, size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result = arxivPaperRepository.searchByTitleOrSummary(keyword, pageable);

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("搜索结果已缓存到Redis，关键词: {}", keyword);
        } else {
            log.info("搜索结果为空，不缓存，关键词: {}", keyword);
        }

        return result;
    }

    /**
     * 根据分类查询论文
     * @param category 分类
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByCategoryFromDatabase(String category, int page, int size) {
        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("category:%s:page:%d:size:%d", category, page, size);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取分类论文，分类: {}, 页码: {}", category, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询分类论文: {}, 页码: {}, 每页数量: {}", category, page, size);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        // 处理未分类的情况，查询primaryCategory为NULL的记录
        if ("UNCATEGORIZED".equals(category)) {
            result = arxivPaperRepository.findByPrimaryCategoryIsNull(pageable);
        } else {
            result = arxivPaperRepository.findByPrimaryCategory(category, pageable);
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("分类论文已缓存到Redis，分类: {}", category);
        } else {
            log.info("分类论文查询结果为空，不缓存，分类: {}", category);
        }

        return result;
    }

    /**
     * 分页查询所有论文（支持按githubUrl筛选）
     * @param page 页码
     * @param size 每页数量
     * @param hasGithub 是否有GitHub URL
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersFromDatabase(int page, int size, Boolean hasGithub) {
        if (hasGithub == null) {
            return getPapersFromDatabase(page, size);
        }

        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("page:%d:size:%d:github:%b", page, size, hasGithub);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取论文列表（带GitHub筛选），页码: {}, hasGithub: {}", page, hasGithub);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询论文（带GitHub筛选），页码: {}, 每页数量: {}, hasGithub: {}", page, size, hasGithub);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        if (hasGithub) {
            result = arxivPaperRepository.findByGithubUrlIsNotNull(pageable);
        } else {
            result = arxivPaperRepository.findByGithubUrlIsNull(pageable);
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("论文列表（带GitHub筛选）已缓存到Redis，页码: {}", page);
        } else {
            log.info("论文列表（带GitHub筛选）查询结果为空，不缓存，页码: {}", page);
        }

        return result;
    }

    /**
     * 根据关键词查询论文（标题或摘要）（支持按githubUrl筛选）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @param hasGithub 是否有GitHub URL
     * @return 论文分页结果
     */
    public Page<ArxivPaper> searchPapersFromDatabase(String keyword, int page, int size, Boolean hasGithub) {
        if (hasGithub == null) {
            return searchPapersFromDatabase(keyword, page, size);
        }

        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("search:%s:page:%d:size:%d:github:%b", keyword, page, size, hasGithub);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取搜索结果（带GitHub筛选），关键词: {}, 页码: {}, hasGithub: {}", keyword, page, hasGithub);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库搜索论文（带GitHub筛选），关键词: {}, 页码: {}, 每页数量: {}, hasGithub: {}", keyword, page, size, hasGithub);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        if (hasGithub) {
            result = arxivPaperRepository.searchByTitleOrSummaryAndGithubUrlIsNotNull(keyword, pageable);
        } else {
            result = arxivPaperRepository.searchByTitleOrSummaryAndGithubUrlIsNull(keyword, pageable);
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("搜索结果（带GitHub筛选）已缓存到Redis，关键词: {}", keyword);
        } else {
            log.info("搜索结果（带GitHub筛选）为空，不缓存，关键词: {}", keyword);
        }

        return result;
    }

    /**
     * 根据分类查询论文（支持按githubUrl筛选）
     * @param category 分类
     * @param page 页码
     * @param size 每页数量
     * @param hasGithub 是否有GitHub URL
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByCategoryFromDatabase(String category, int page, int size, Boolean hasGithub) {
        if (hasGithub == null) {
            return getPapersByCategoryFromDatabase(category, page, size);
        }

        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("category:%s:page:%d:size:%d:github:%b", category, page, size, hasGithub);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取分类论文（带GitHub筛选），分类: {}, 页码: {}, hasGithub: {}", category, page, hasGithub);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询分类论文（带GitHub筛选）: {}, 页码: {}, 每页数量: {}, hasGithub: {}", category, page, size, hasGithub);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        // 处理未分类的情况
        if ("UNCATEGORIZED".equals(category)) {
            if (hasGithub) {
                result = arxivPaperRepository.findByPrimaryCategoryIsNullAndGithubUrlIsNotNull(pageable);
            } else {
                result = arxivPaperRepository.findByPrimaryCategoryIsNullAndGithubUrlIsNull(pageable);
            }
        } else {
            if (hasGithub) {
                result = arxivPaperRepository.findByPrimaryCategoryAndGithubUrlIsNotNull(category, pageable);
            } else {
                result = arxivPaperRepository.findByPrimaryCategoryAndGithubUrlIsNull(category, pageable);
            }
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("分类论文（带GitHub筛选）已缓存到Redis，分类: {}", category);
        } else {
            log.info("分类论文（带GitHub筛选）查询结果为空，不缓存，分类: {}", category);
        }

        return result;
    }

    /**
     * 根据日期范围查询论文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByDateRangeFromDatabase(String startDate, String endDate, int page, int size) {
        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("date:%s:%s:page:%d:size:%d", startDate, endDate, page, size);

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取日期范围论文，日期: {} 到 {}, 页码: {}", startDate, endDate, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询日期范围论文: {} 到 {}, 页码: {}, 每页数量: {}", startDate, endDate, page, size);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result = arxivPaperRepository.findByPublishedDateBetween(start, end, pageable);

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("日期范围论文已缓存到Redis，日期: {} 到 {}", startDate, endDate);
        } else {
            log.info("日期范围论文查询结果为空，不缓存，日期: {} 到 {}", startDate, endDate);
        }

        return result;
    }

    /**
     * 根据日期范围查询论文（支持按githubUrl筛选）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @param hasGithub 是否有GitHub URL
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByDateRangeFromDatabase(String startDate, String endDate, int page, int size, Boolean hasGithub) {
        if (hasGithub == null) {
            return getPapersByDateRangeFromDatabase(startDate, endDate, page, size);
        }

        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("date:%s:%s:github:%b:page:%d:size:%d", startDate, endDate, hasGithub, page, size);
        
        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取日期范围论文（带GitHub筛选），日期: {} 到 {}, hasGithub: {}, 页码: {}", startDate, endDate, hasGithub, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询日期范围论文（带GitHub筛选）: {} 到 {}, hasGithub: {}, 页码: {}, 每页数量: {}", startDate, endDate, hasGithub, page, size);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        if (hasGithub) {
            result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNotNull(start, end, pageable);
        } else {
            result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNull(start, end, pageable);
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("日期范围论文（带GitHub筛选）已缓存到Redis，日期: {} 到 {}, hasGithub: {}", startDate, endDate, hasGithub);
        } else {
            log.info("日期范围论文（带GitHub筛选）查询结果为空，不缓存，日期: {} 到 {}, hasGithub: {}", startDate, endDate, hasGithub);
        }

        return result;
    }

    /**
     * 根据分类和日期范围查询论文
     * @param category 分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByCategoryAndDateRangeFromDatabase(String category, String startDate, String endDate, int page, int size) {
        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("category:%s:date:%s:%s:page:%d:size:%d", category, startDate, endDate, page, size);
        
        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取分类和日期范围论文，分类: {}, 日期: {} 到 {}, 页码: {}", category, startDate, endDate, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询分类和日期范围论文: {}, {} 到 {}, 页码: {}, 每页数量: {}", category, startDate, endDate, page, size);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        // 处理未分类的情况
        if ("UNCATEGORIZED".equals(category)) {
            result = arxivPaperRepository.findByPrimaryCategoryIsNullAndPublishedDateBetween(start, end, pageable);
        } else {
            result = arxivPaperRepository.findByPrimaryCategoryAndPublishedDateBetween(category, start, end, pageable);
        }

        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("分类和日期范围论文已缓存到Redis，分类: {}, 日期: {} 到 {}", category, startDate, endDate);
        } else {
            log.info("分类和日期范围论文查询结果为空，不缓存，分类: {}, 日期: {} 到 {}", category, startDate, endDate);
        }

        return result;
    }

    /**
     * 根据分类和日期范围查询论文（支持按githubUrl筛选）
     * @param category 分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @param hasGithub 是否有GitHub URL
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByCategoryAndDateRangeFromDatabase(String category, String startDate, String endDate, int page, int size, Boolean hasGithub) {
        if (hasGithub == null) {
            return getPapersByCategoryAndDateRangeFromDatabase(category, startDate, endDate, page, size);
        }

        String cacheKey = RedisClient.PAPERS_PREFIX + String.format("category:%s:date:%s:%s:github:%b:page:%d:size:%d", category, startDate, endDate, hasGithub, page, size);
        
        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof PageCacheDTO) {
            log.info("从Redis缓存获取分类和日期范围论文（带GitHub筛选），分类: {}, 日期: {} 到 {}, hasGithub: {}, 页码: {}", category, startDate, endDate, hasGithub, page);
            PageCacheDTO<?> cacheDTO = (PageCacheDTO<?>) cachedValue;
            return convertCacheDTOToPage(cacheDTO);
        }

        log.info("从数据库查询分类和日期范围论文（带GitHub筛选）: {}, {} 到 {}, hasGithub: {}, 页码: {}, 每页数量: {}", category, startDate, endDate, hasGithub, page, size);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        Page<ArxivPaper> result;
        
        // 处理未分类的情况
        if ("UNCATEGORIZED".equals(category)) {
            if (hasGithub) {
                result = arxivPaperRepository.findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNotNull(start, end, pageable);
            } else {
                result = arxivPaperRepository.findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNull(start, end, pageable);
            }
        } else {
            if (hasGithub) {
                result = arxivPaperRepository.findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNotNull(category, start, end, pageable);
            } else {
                result = arxivPaperRepository.findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNull(category, start, end, pageable);
            }
        }

        // 保存到Redis缓存，过期时间30分钟
        // 只有当查询结果不为空时才缓存，避免缓存空结果导致后续无法获取新数据
        if (result.hasContent()) {
            PageCacheDTO<ArxivPaper> cacheDTO = convertPageToCacheDTO(result);
            redisClient.set(cacheKey, cacheDTO, RedisClient.THIRTY_MINUTES, TimeUnit.MINUTES);
            log.info("分类和日期范围论文（带GitHub筛选）已缓存到Redis，分类: {}, 日期: {} 到 {}, hasGithub: {}", category, startDate, endDate, hasGithub);
        } else {
            log.info("分类和日期范围论文（带GitHub筛选）查询结果为空，不缓存，分类: {}, 日期: {} 到 {}, hasGithub: {}", category, startDate, endDate, hasGithub);
        }

        return result;
    }

    /**
     * 根据分类和关键词查询论文
     * @param category 分类
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    public Page<ArxivPaper> getPapersByCategoryAndKeywordFromDatabase(String category, String keyword, int page, int size) {
        return getPapersByCategoryAndKeywordFromDatabase(category, keyword, page, size, null);
    }

    public Page<ArxivPaper> getPapersByCategoryAndKeywordFromDatabase(String category, String keyword, int page, int size, Boolean hasGithub) {
        log.info("从数据库查询分类和关键词论文: {}, 关键词: {}, 页码: {}, 每页数量: {}, hasGithub: {}", category, keyword, page, size, hasGithub);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
        
        Page<ArxivPaper> papersByCategory;
        // 处理未分类的情况
        if ("UNCATEGORIZED".equals(category)) {
            if (hasGithub == null) {
                papersByCategory = arxivPaperRepository.findByPrimaryCategoryIsNull(pageable);
            } else if (hasGithub) {
                papersByCategory = arxivPaperRepository.findByPrimaryCategoryIsNullAndGithubUrlIsNotNull(pageable);
            } else {
                papersByCategory = arxivPaperRepository.findByPrimaryCategoryIsNullAndGithubUrlIsNull(pageable);
            }
        } else {
            if (hasGithub == null) {
                papersByCategory = arxivPaperRepository.findByPrimaryCategory(category, pageable);
            } else if (hasGithub) {
                papersByCategory = arxivPaperRepository.findByPrimaryCategoryAndGithubUrlIsNotNull(category, pageable);
            } else {
                papersByCategory = arxivPaperRepository.findByPrimaryCategoryAndGithubUrlIsNull(category, pageable);
            }
        }
        
        String lowerKeyword = keyword.toLowerCase();
        List<ArxivPaper> filtered = papersByCategory.getContent().stream()
            .filter(p -> p.getTitle().toLowerCase().contains(lowerKeyword) || p.getSummary().toLowerCase().contains(lowerKeyword))
            .toList();
        
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    /**
     * 根据arXiv ID获取论文
     * @param arxivId arXiv ID
     * @return 论文信息
     */
    public ArxivPaper getPaperFromDatabase(String arxivId) {
        log.info("从数据库获取论文: {}", arxivId);
        return arxivPaperRepository.findByArxivId(arxivId).orElse(null);
    }

    /**
     * 批量查询论文
     * @param arxivIds arXiv ID列表
     * @return 论文列表
     */
    public List<ArxivPaper> getPapersByIdsFromDatabase(List<String> arxivIds) {
        log.info("从数据库批量查询论文，数量: {}", arxivIds.size());
        return arxivPaperRepository.findByArxivIds(arxivIds);
    }

    /**
     * 将Page对象转换为ArxivSearchResponse
     */
    private ArxivSearchResponse convertPageToResponse(Page<ArxivPaper> page) {
        ArxivSearchResponse response = new ArxivSearchResponse();
        response.setTotalResults((int) page.getTotalElements());
        response.setStartIndex((int) page.getPageable().getOffset());
        response.setItemsPerPage(page.getSize());
        response.setPapers(arxivPaperStructMapper.toDtoList(page.getContent()));
        return response;
    }

    public ArxivSearchResponse searchPapersFromDb(String keyword, int page, int size) {
        Page<ArxivPaper> result = searchPapersFromDatabase(keyword, page, size);
        return convertPageToResponse(result);
    }

    public ArxivSearchResponse searchPapersFromDb(String keyword, int page, int size, Boolean hasGithub) {
        Page<ArxivPaper> result = searchPapersFromDatabase(keyword, page, size, hasGithub);
        return convertPageToResponse(result);
    }

    public ArxivSearchResponse searchByCategoryFromDb(String category, int page, int size) {
        Page<ArxivPaper> result = getPapersByCategoryFromDatabase(category, page, size);
        return convertPageToResponse(result);
    }

    public ArxivSearchResponse searchByCategoryFromDb(String category, int page, int size, Boolean hasGithub) {
        Page<ArxivPaper> result = getPapersByCategoryFromDatabase(category, page, size, hasGithub);
        return convertPageToResponse(result);
    }

    public ArxivSearchResponse searchByCategoryAndDateRangeFromDb(String category, String startDate, String endDate, int page, int size, Boolean hasGithub) {
        // 先查询指定日期范围的论文
        Page<ArxivPaper> result = getPapersByCategoryAndDateRangeFromDatabase(category, startDate, endDate, page, size, hasGithub);
        
        String actualStartDate = startDate;
        String actualEndDate = endDate;
        
        // 如果没有数据且是第一页,则尝试查询数据库中最后一天的论文
        if (!result.hasContent() && page == 1) {
            log.info("指定日期范围 {} 到 {} 没有数据,尝试查询数据库中最后一天的论文", startDate, endDate);
            LocalDate latestDate = getLatestPublishedDate();
            if (latestDate != null) {
                log.info("数据库中最新的论文发布日期: {}", latestDate);
                actualStartDate = latestDate.toString();
                actualEndDate = latestDate.toString();
                result = getPapersByCategoryAndDateRangeFromDatabase(category, actualStartDate, actualEndDate, page, size, hasGithub);
            }
        }
        
        ArxivSearchResponse response = convertPageToResponse(result);
        response.setActualStartDate(actualStartDate);
        response.setActualEndDate(actualEndDate);
        
        // 添加分类统计信息
        try {
            LocalDate start = LocalDate.parse(actualStartDate);
            LocalDate end = LocalDate.parse(actualEndDate);
            List<CategoryCountDTO> categoryCounts = getCategoryCountsFromDatabase(start, end);
            response.setCategoryCounts(categoryCounts);
        } catch (Exception e) {
            log.error("获取分类统计信息失败", e);
        }
        
        return response;
    }

    public ArxivSearchResponse searchByDateRangeFromDb(String startDate, String endDate, int page, int size) {
        // 先查询指定日期范围的论文
        Page<ArxivPaper> result = getPapersByDateRangeFromDatabase(startDate, endDate, page, size);
        
        String actualStartDate = startDate;
        String actualEndDate = endDate;
        
        // 如果没有数据且是第一页,则尝试查询数据库中最后一天的论文
        if (!result.hasContent() && page == 1) {
            log.info("指定日期范围 {} 到 {} 没有数据,尝试查询数据库中最后一天的论文", startDate, endDate);
            LocalDate latestDate = getLatestPublishedDate();
            if (latestDate != null) {
                log.info("数据库中最新的论文发布日期: {}", latestDate);
                actualStartDate = latestDate.toString();
                actualEndDate = latestDate.toString();
                result = getPapersByDateRangeFromDatabase(actualStartDate, actualEndDate, page, size);
            }
        }
        
        ArxivSearchResponse response = convertPageToResponse(result);
        response.setActualStartDate(actualStartDate);
        response.setActualEndDate(actualEndDate);
        
        // 添加分类统计信息
        try {
            LocalDate start = LocalDate.parse(actualStartDate);
            LocalDate end = LocalDate.parse(actualEndDate);
            List<CategoryCountDTO> categoryCounts = getCategoryCountsFromDatabase(start, end);
            response.setCategoryCounts(categoryCounts);
        } catch (Exception e) {
            log.error("获取分类统计信息失败", e);
        }
        
        return response;
    }

    // 支持hasGithub参数的重载方法
    public ArxivSearchResponse searchByDateRangeFromDb(String startDate, String endDate, Integer page, Integer size, Boolean hasGithub) {
        String actualStartDate = startDate;
        String actualEndDate = endDate;
        Page<ArxivPaper> result;
        
        if (hasGithub == null) {
            result = getPapersByDateRangeFromDatabase(startDate, endDate, page, size);
        } else {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
            
            if (hasGithub) {
                result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNotNull(start, end, pageable);
            } else {
                result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNull(start, end, pageable);
            }
        }
        
        // 如果没有数据且是第一页,则尝试查询数据库中最后一天的论文
        if (!result.hasContent() && page == 1) {
            log.info("指定日期范围 {} 到 {} 没有数据,尝试查询数据库中最后一天的论文", startDate, endDate);
            LocalDate latestDate = getLatestPublishedDate();
            if (latestDate != null) {
                log.info("数据库中最新的论文发布日期: {}", latestDate);
                actualStartDate = latestDate.toString();
                actualEndDate = latestDate.toString();
                
                if (hasGithub == null) {
                    result = getPapersByDateRangeFromDatabase(actualStartDate, actualEndDate, page, size);
                } else {
                    Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedDate"));
                    if (hasGithub) {
                        result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNotNull(latestDate, latestDate, pageable);
                    } else {
                        result = arxivPaperRepository.findByPublishedDateBetweenAndGithubUrlIsNull(latestDate, latestDate, pageable);
                    }
                }
            }
        }
        
        ArxivSearchResponse response = convertPageToResponse(result);
        response.setActualStartDate(actualStartDate);
        response.setActualEndDate(actualEndDate);
        
        // 添加分类统计信息
        try {
            LocalDate start = LocalDate.parse(actualStartDate);
            LocalDate end = LocalDate.parse(actualEndDate);
            List<CategoryCountDTO> categoryCounts = getCategoryCountsFromDatabase(start, end);
            response.setCategoryCounts(categoryCounts);
        } catch (Exception e) {
            log.error("获取分类统计信息失败", e);
        }
        
        return response;
    }

    /**
     * 清除论文列表相关的缓存
     * 用于流式刷新后，确保查询能获取到最新数据
     */
    public void clearPapersCache() {
        try {
            // 清除分页列表缓存
            redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "page:*");

            // 清除搜索缓存
            redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "search:*");

            // 清除分类缓存
            redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "category:*");

            // 清除日期范围缓存
            redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "date:*");

            log.info("已清除所有论文列表相关缓存");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
        }
    }

    /**
     * 清除指定分类相关的缓存
     * @param category 分类
     */
    public void clearCategoryCache(String category) {
        try {
            redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "category:" + category + ":*");
            log.info("已清除分类 {} 相关缓存", category);
        } catch (Exception e) {
            log.error("清除分类缓存失败: {}", category, e);
        }
    }

    /**
     * 获取数据库中最新的论文发布日期
     * @return 最新的发布日期,如果数据库为空则返回null
     */
    public LocalDate getLatestPublishedDate() {
        log.info("查询数据库中最新的论文发布日期");
        LocalDate latestDate = arxivPaperRepository.findLatestPublishedDate();
        log.info("数据库中最新的论文发布日期: {}", latestDate);
        return latestDate;
    }
    
    /**
     * 将Page对象转换为PageCacheDTO
     * @param page Page对象
     * @return PageCacheDTO对象
     */
    private PageCacheDTO<ArxivPaper> convertPageToCacheDTO(Page<ArxivPaper> page) {
        return new PageCacheDTO<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
    
    /**
     * 将PageCacheDTO转换为Page对象
     * @param cacheDTO PageCacheDTO对象
     * @return Page对象
     */
    private Page<ArxivPaper> convertCacheDTOToPage(PageCacheDTO<?> cacheDTO) {
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
        
        // 创建Pageable对象，使用Sort保持与原查询一致
        Pageable pageable = PageRequest.of(cacheDTO.getPage(), cacheDTO.getSize(), Sort.by(Sort.Direction.DESC, "publishedDate"));
        return new PageImpl<>(content, pageable, cacheDTO.getTotalElements());
    }
}