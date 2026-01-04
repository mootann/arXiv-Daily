package com.mootann.arxivdaily.client;

import com.mootann.arxivdaily.config.ArxivProxyConfig;
import com.mootann.arxivdaily.converter.ArxivEntryMapper;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.xml.ArxivEntry;
import com.mootann.arxivdaily.xml.ArxivFeed;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * arXiv API客户端
 * 用于与arXiv API进行交互，获取论文信息
 */
@Slf4j
@Component
public class ArxivClient {
    
    private static final String ARXIV_API_BASE_URL = "https://export.arxiv.org/api/query";
    private static final String ARXIV_BASE_URL = "https://arxiv.org";
    // 请求间隔时间（毫秒），arXiv API建议至少3秒
    private static final long REQUEST_INTERVAL_MS = 3000;
    // 每日获取论文的最大数量限制
    private static final int DAILY_MAX_RESULTS = 1000;
    // arXiv API单次请求最大返回结果数限制
    private static final int API_MAX_RESULTS_PER_REQUEST = 100;
    // 上次请求的时间
    private static long lastRequestTime = 0;

    private final WebClient webClient;
    private final ArxivProxyConfig proxyConfig;
    
    @Autowired
    private ArxivEntryMapper arxivEntryMapper;
    
    public ArxivClient(WebClient webClient, ArxivProxyConfig proxyConfig) {
        this.webClient = webClient;
        this.proxyConfig = proxyConfig;
        log.info("arXiv代理配置: enabled={}, host={}, port={}", 
            proxyConfig.getEnabled(), proxyConfig.getHost(), proxyConfig.getPort());
    }
    
    /**
     * 根据arXiv ID获取单篇论文信息
     * @param arxivId arXiv ID，例如：2301.12345
     * @return 论文信息
     */
    public ArxivPaperDTO getPaperById(String arxivId) throws UnsupportedEncodingException {
        String query = "id:" + arxivId;
        ArxivSearchRequest request = new ArxivSearchRequest(query, 1, "0", null, null, null, null);
        ArxivSearchResponse response = searchPapers(request);
        
        if (response != null && !response.getPapers().isEmpty()) {
            return response.getPapers().get(0);
        }
        return null;
    }
    
    /**
     * 批量获取论文信息
     * @param arxivIds arXiv ID列表
     * @return 论文信息列表
     */
    public List<ArxivPaperDTO> getPapersByIds(List<String> arxivIds) throws UnsupportedEncodingException {
        String query = String.join(" OR ", arxivIds.stream()
                .map(id -> "id:" + id)
                .toList());
        ArxivSearchRequest request = new ArxivSearchRequest(query, arxivIds.size(), "0", null, null, null, null);
        ArxivSearchResponse response = searchPapers(request);
        
        return response != null ? response.getPapers() : new ArrayList<>();
    }
    
    /**
     * 搜索论文
     * @param request 搜索请求参数
     * @return 搜索响应结果
     */
    public ArxivSearchResponse searchPapers(ArxivSearchRequest request) throws UnsupportedEncodingException {
        log.info("===== 开始搜索arXiv论文 =====");
        log.info("搜索请求参数: query={}, maxResults={}, start={}, sortBy={}, sortOrder={}", 
            request.getQuery(), request.getMaxResults(), request.getStart(), 
            request.getSortBy(), request.getSortOrder());
        
        // 过滤：只查询cs和eess主类下的子类
        String filteredQuery = filterToCsAndEess(request.getQuery());
        log.info("过滤后的查询语句: {}", filteredQuery);
        
        // 执行请求前确保遵守频率限制
        ensureRequestInterval();
        
        // 构建API请求URL
        StringBuilder urlBuilder = new StringBuilder(ARXIV_API_BASE_URL);
        urlBuilder.append("?search_query=").append(URLEncoder.encode(filteredQuery, StandardCharsets.UTF_8));
        
        if (request.getMaxResults() != null && request.getMaxResults() > 0) {
            urlBuilder.append("&max_results=").append(request.getMaxResults());
        } else {
            urlBuilder.append("&max_results=10");
        }
        
        if (request.getStart() != null) {
            urlBuilder.append("&start=").append(request.getStart());
        }
        
        String requestUrl = urlBuilder.toString();
        log.info("构建的请求URL: {}", requestUrl);
        
        // 使用WebClient执行请求
        try {
            String responseString = webClient.get()
                .uri(requestUrl)
                .header("User-Agent", "ArXiv-Daily/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (responseString != null && responseString.length() > 0) {
                log.info("响应内容长度: {} 字符", responseString.length());
                return parseResponse(responseString);
            } else {
                log.error("API返回空响应");
                return null;
            }
        } catch (Exception e) {
            log.error("搜索arXiv论文失败", e);
            return null;
        }
    }

    /**
     * 搜索论文（支持分页获取超过100条结果）
     * @param request 搜索请求参数
     * @return 搜索响应结果
     */
    public ArxivSearchResponse searchPapersWithPagination(ArxivSearchRequest request) {
        log.info("===== 开始搜索arXiv论文（支持分页） =====");
        log.info("搜索请求参数: query={}, maxResults={}, start={}, sortBy={}, sortOrder={}", 
            request.getQuery(), request.getMaxResults(), request.getStart(), 
            request.getSortBy(), request.getSortOrder());

        // 过滤：只查询cs和eess主类下的子类
        String filteredQuery = filterToCsAndEess(request.getQuery());
        log.info("过滤后的查询语句: {}", filteredQuery);

        Integer userMaxResults = request.getMaxResults();
        if (userMaxResults == null || userMaxResults <= 0) {
            userMaxResults = 10;
        }

        Integer userStart = 0;
        if (request.getStart() != null && !request.getStart().isEmpty()) {
            try {
                userStart = Integer.parseInt(request.getStart());
            } catch (NumberFormatException e) {
                log.warn("起始位置格式不正确，使用默认值0: {}", request.getStart());
                userStart = 0;
            }
        }

        // 初始化响应对象
        ArxivSearchResponse finalResponse = new ArxivSearchResponse();
        List<ArxivPaperDTO> allPapers = new ArrayList<>();

        // 计算需要发送的请求次数
        int resultsNeeded = userMaxResults;
        int currentStart = userStart;
        int totalResults = 0;

        while (resultsNeeded > 0) {
            // 计算本次请求的最大结果数（最多100）
            int currentMaxResults = Math.min(resultsNeeded, API_MAX_RESULTS_PER_REQUEST);

            log.info("分页请求: 当前批次需要获取 {} 条，起始位置: {}", currentMaxResults, currentStart);

            // 构建本次请求
            ArxivSearchRequest currentRequest = new ArxivSearchRequest(
                filteredQuery,
                currentMaxResults,
                String.valueOf(currentStart),
                request.getSortBy(),
                request.getSortOrder(),
                request.getSource(),
                request.getHasGithub()
            );

            // 执行单次请求
            ArxivSearchResponse currentResponse = executeSingleRequest(currentRequest);

            if (currentResponse == null) {
                log.error("分页请求失败，终止获取");
                break;
            }

            // 保存总结果数（第一次请求时）
            if (totalResults == 0) {
                totalResults = currentResponse.getTotalResults();
                finalResponse.setTotalResults(totalResults);
            }

            // 累加论文列表
            if (currentResponse.getPapers() != null && !currentResponse.getPapers().isEmpty()) {
                allPapers.addAll(currentResponse.getPapers());
                log.info("成功获取 {} 条论文，累计获取 {} 条", 
                    currentResponse.getPapers().size(), allPapers.size());

                // 更新起始位置和剩余需要获取的数量
                currentStart += currentResponse.getPapers().size();
                resultsNeeded -= currentResponse.getPapers().size();

                // 检查是否已获取足够数量或已获取所有结果
                if (allPapers.size() >= userMaxResults || 
                    currentResponse.getPapers().size() < currentMaxResults ||
                    currentStart >= totalResults) {
                    log.info("分页获取完成: 已获取 {} 条，需要 {} 条", allPapers.size(), userMaxResults);
                    break;
                }
            } else {
                log.warn("本次请求返回空结果，终止获取");
                break;
            }

            // 每次请求之间需要遵守频率限制
            ensureRequestInterval();
        }

        // 设置最终响应
        finalResponse.setStartIndex(userStart);
        finalResponse.setItemsPerPage(allPapers.size());
        finalResponse.setPapers(allPapers);

        log.info("===== 分页搜索完成 =====");
        log.info("最终结果: 总数={}, 获取数量={}", 
            finalResponse.getTotalResults(), finalResponse.getPapers().size());

        return finalResponse;
    }

    /**
     * 执行单次HTTP请求
     * @param request 请求参数
     * @return 响应结果
     */
    private ArxivSearchResponse executeSingleRequest(ArxivSearchRequest request) {
        try {
            // 构建API请求URL
            StringBuilder urlBuilder = new StringBuilder(ARXIV_API_BASE_URL);
            urlBuilder.append("?search_query=").append(java.net.URLEncoder.encode(request.getQuery(), "UTF-8"));

            if (request.getMaxResults() != null && request.getMaxResults() > 0) {
                urlBuilder.append("&max_results=").append(request.getMaxResults());
            } else {
                urlBuilder.append("&max_results=10");
            }

            if (request.getStart() != null) {
                urlBuilder.append("&start=").append(request.getStart());
            }

            if (request.getSortBy() != null) {
                urlBuilder.append("&sortBy=").append(request.getSortBy());
            }

            if (request.getSortOrder() != null) {
                urlBuilder.append("&sortOrder=").append(request.getSortOrder());
            }

            String requestUrl = urlBuilder.toString();
            log.debug("请求URL: {}", requestUrl);

            // 使用WebClient执行请求
            String responseString = webClient.get()
                .uri(requestUrl)
                .header("User-Agent", "ArXiv-Daily/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (responseString != null) {
                return parseResponse(responseString);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("执行单次请求失败", e);
            return null;
        }
    }

    /**
     * 根据arXiv分类搜索论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param maxResults 最大结果数
     * @param page 页码（从1开始）
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategory(String category, Integer maxResults, Integer page) throws UnsupportedEncodingException {
        String query = "cat:" + category;
        int startIndex = (page - 1) * maxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, String.valueOf(startIndex), null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据arXiv分类搜索论文（使用默认结果数）
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategory(String category) throws UnsupportedEncodingException {
        return searchByCategory(category, 10, 1);
    }

    /**
     * 根据关键词搜索论文
     * @param keyword 关键词
     * @param maxResults 最大结果数
     * @param page 页码（从1开始）
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByKeyword(String keyword, Integer maxResults, Integer page) throws UnsupportedEncodingException {
        String query = "all:" + keyword;
        int startIndex = (page - 1) * maxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, String.valueOf(startIndex), null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据arXiv分类和日期范围搜索论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @param page 页码（从1开始）
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndDateRange(String category, String startDate, String endDate, Integer maxResults, Integer page) throws UnsupportedEncodingException {
        // 计算日期范围的天数
        Integer days = calculateDaysBetweenDates(startDate, endDate);
        // 计算允许的最大结果数
        Integer calculatedMaxResults = calculateMaxResults(days, maxResults);
        log.info("按分类 {} 和日期范围 {} 至 {} 搜索论文，天数: {}, 计算后的最大结果数: {}", category, startDate, endDate, days, calculatedMaxResults);

        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedStartDate = formatDateForArxiv(startDate);
        String formattedEndDate = formatDateForArxiv(endDate);
        String query = String.format("cat:%s AND submittedDate:[%s TO %s]", category, formattedStartDate, formattedEndDate);
        int startIndex = (page - 1) * calculatedMaxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, calculatedMaxResults, String.valueOf(startIndex), null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据arXiv分类和日期范围搜索论文（使用默认结果数）
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndDateRange(String category, String startDate, String endDate) throws UnsupportedEncodingException {
        return searchByCategoryAndDateRange(category, startDate, endDate, 10, 1);
    }

    /**
     * 根据arXiv分类和指定日期之后搜索论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryFromDate(String category, String startDate, Integer maxResults) throws UnsupportedEncodingException {
        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedStartDate = formatDateForArxiv(startDate);
        String query = String.format("cat:%s AND submittedDate:[%s TO *]", category, formattedStartDate);
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据arXiv分类和指定日期之前搜索论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryToDate(String category, String endDate, Integer maxResults) throws UnsupportedEncodingException {
        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedEndDate = formatDateForArxiv(endDate);
        String query = String.format("cat:%s AND submittedDate:[* TO %s]", category, formattedEndDate);
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据发布日期范围搜索论文
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @param page 页码（从1开始）
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDateRange(String startDate, String endDate, Integer maxResults, Integer page) throws UnsupportedEncodingException {
        // 计算日期范围的天数
        Integer days = calculateDaysBetweenDates(startDate, endDate);
        // 计算允许的最大结果数
        Integer calculatedMaxResults = calculateMaxResults(days, maxResults);
        log.info("按日期范围 {} 至 {} 搜索论文，天数: {}, 计算后的最大结果数: {}", startDate, endDate, days, calculatedMaxResults);

        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedStartDate = formatDateForArxiv(startDate);
        String formattedEndDate = formatDateForArxiv(endDate);
        String query = String.format("submittedDate:[%s TO %s]", formattedStartDate, formattedEndDate);
        int startIndex = (page - 1) * calculatedMaxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, calculatedMaxResults, String.valueOf(startIndex), null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据发布日期范围搜索论文（使用默认结果数）
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDateRange(String startDate, String endDate) throws UnsupportedEncodingException {
        return searchByDateRange(startDate, endDate, 10, 1);
    }

    /**
     * 根据指定日期搜索论文（精确日期）
     * @param date 日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByDate(String date, Integer maxResults) throws UnsupportedEncodingException {
        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedDate = formatDateForArxiv(date);
        String query = "submittedDate:" + formattedDate;
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 获取最近N天的论文
     * @param days 天数
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchRecentPapers(int days, Integer maxResults) throws UnsupportedEncodingException {
        // 计算允许的最大结果数
        Integer calculatedMaxResults = calculateMaxResults(days, maxResults);
        log.info("获取最近 {} 天的论文，计算后的最大结果数: {}", days, calculatedMaxResults);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // 使用YYYYMMDD格式
        String startDateStr = startDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.BASIC_ISO_DATE);

        String query = String.format("submittedDate:[%s TO %s]", startDateStr, endDateStr);
        ArxivSearchRequest request = new ArxivSearchRequest(query, calculatedMaxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 获取最近N天指定分类的论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param days 天数
     * @param maxResults 最大结果数
     * @param page 页码（从1开始）
     * @return 搜索结果
     */
    public ArxivSearchResponse searchRecentPapersByCategory(String category, int days, Integer maxResults, Integer page) throws UnsupportedEncodingException {
        // 计算允许的最大结果数
        Integer calculatedMaxResults = calculateMaxResults(days, maxResults);
        log.info("获取最近 {} 天 {} 分类的论文，计算后的最大结果数: {}", days, category, calculatedMaxResults);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // 使用YYYYMMDD格式
        String startDateStr = startDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        String endDateStr = endDate.format(DateTimeFormatter.BASIC_ISO_DATE);

        String query = String.format("cat:%s AND submittedDate:[%s TO %s]", category, startDateStr, endDateStr);
        int startIndex = (page - 1) * calculatedMaxResults;
        ArxivSearchRequest request = new ArxivSearchRequest(query, calculatedMaxResults, String.valueOf(startIndex), null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据多个分类搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByMultipleCategories(List<String> categories, Integer maxResults) throws UnsupportedEncodingException {
        String categoryQuery = String.join(" OR ", categories.stream()
                .map(cat -> "cat:" + cat)
                .toList());
        ArxivSearchRequest request = new ArxivSearchRequest(categoryQuery, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据多个分类和日期范围搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByMultipleCategoriesAndDateRange(List<String> categories, String startDate, String endDate, Integer maxResults) throws UnsupportedEncodingException {
        String categoryQuery = String.join(" OR ", categories.stream()
                .map(cat -> "cat:" + cat)
                .toList());
        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String formattedStartDate = formatDateForArxiv(startDate);
        String formattedEndDate = formatDateForArxiv(endDate);
        String dateQuery = String.format("submittedDate:[%s TO %s]", formattedStartDate, formattedEndDate);
        String query = String.format("(%s) AND %s", categoryQuery, dateQuery);
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    /**
     * 根据分类和关键词搜索论文
     * @param category 分类代码，例如：cs.AI, cs.LG, cs.CV
     * @param keyword 关键词
     * @param maxResults 最大结果数
     * @return 搜索结果
     */
    public ArxivSearchResponse searchByCategoryAndKeyword(String category, String keyword, Integer maxResults) throws UnsupportedEncodingException {
        String query = String.format("cat:%s AND all:%s", category, keyword);
        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }
    
    /**
     * 解析arXiv API响应（使用JAXB）
     * @param responseString 响应字符串
     * @return 解析后的搜索结果
     */
    private ArxivSearchResponse parseResponse(String responseString) {
        try {
            log.info("开始使用JAXB解析arXiv API响应...");
            
            // 检查是否为错误响应
            if (responseString.contains("https://arxiv.org/api/errors")) {
                log.error("arXiv API返回错误响应");
                // 返回一个空结果而不是null
                ArxivSearchResponse emptyResponse = new ArxivSearchResponse();
                emptyResponse.setTotalResults(0);
                emptyResponse.setStartIndex(0);
                emptyResponse.setItemsPerPage(0);
                emptyResponse.setPapers(new ArrayList<>());
                return emptyResponse;
            }
            
            // 使用JAXB解析XML
            JAXBContext jaxbContext = JAXBContext.newInstance(ArxivFeed.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(responseString);
            ArxivFeed feed = (ArxivFeed) unmarshaller.unmarshal(reader);
            
            // 使用MapStruct转换为DTO并构建URLs
            List<ArxivPaperDTO> papers = new ArrayList<>();
            if (feed.getEntries() != null) {
                for (ArxivEntry entry : feed.getEntries()) {
                    ArxivPaperDTO paper = arxivEntryMapper.entryToDTO(entry);
                    if (paper != null && paper.getArxivId() != null) {
                        // 构建URLs
                        buildPaperUrls(paper);
                        papers.add(paper);
                        log.debug("成功转换论文 {} - {}", papers.size(), paper.getTitle());
                    }
                }
            }
            
            log.info("JAXB解析完成 - totalResults={}, startIndex={}, itemsPerPage={}, papers={}", 
                feed.getTotalResults(), feed.getStartIndex(), feed.getItemsPerPage(), papers.size());
            
            ArxivSearchResponse response = new ArxivSearchResponse();
            response.setTotalResults(feed.getTotalResults() != null ? feed.getTotalResults() : 0);
            response.setStartIndex(feed.getStartIndex() != null ? feed.getStartIndex() : 0);
            response.setItemsPerPage(feed.getItemsPerPage() != null ? feed.getItemsPerPage() : 0);
            response.setPapers(papers);
            
            return response;
        } catch (Exception e) {
            log.error("JAXB解析arXiv响应失败", e);
            return null;
        }
    }
    
    /**
     * 构建论文相关的URLs
     */
    private void buildPaperUrls(ArxivPaperDTO paper) {
        if (paper.getArxivId() != null) {
            paper.setPdfUrl(ARXIV_BASE_URL + "/pdf/" + paper.getArxivId() + ".pdf");
            paper.setLatexUrl(ARXIV_BASE_URL + "/e-print/" + paper.getArxivId());
            paper.setArxivUrl(ARXIV_BASE_URL + "/abs/" + paper.getArxivId());
        }
    }
    
    /**
     * 将日期格式化为arXiv API要求的格式
     * @param date 日期字符串，格式：YYYY-MM-DD
     * @return 格式化后的日期字符串，格式：YYYYMMDD
     */
    private String formatDateForArxiv(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }
        try {
            // 如果日期已经是YYYYMMDD格式，直接返回
            if (date.matches("\\d{8}")) {
                return date;
            }
            // 将YYYY-MM-DD格式转换为YYYYMMDD格式
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            return localDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            log.error("日期格式化失败: {}", date, e);
            // 如果转换失败，尝试移除所有破折号
            return date.replace("-", "");
        }
    }

    /**
     * 过滤查询语句，只查询cs和eess主类下的子类
     * @param originalQuery 原始查询语句
     * @return 过滤后的查询语句
     */
    private String filterToCsAndEess(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return "(cat:cs.* OR cat:eess.*)";
        }
        
        // 如果查询语句已经包含分类过滤，检查是否只包含cs或eess
        if (originalQuery.contains("cat:")) {
            // 检查查询中是否只包含cs或eess的分类
            String lowerQuery = originalQuery.toLowerCase();
            boolean hasValidCategory = false;
            
            // 提取所有分类
            Pattern pattern = Pattern.compile("cat:([a-zA-Z]+\\.[a-zA-Z]+)");
            Matcher matcher = pattern.matcher(originalQuery);
            
            Set<String> validCategories = new java.util.HashSet<>();
            Set<String> allCategories = new java.util.HashSet<>();
            
            while (matcher.find()) {
                String category = matcher.group(1);
                allCategories.add(category);
                if (category.startsWith("cs.") || category.startsWith("eess.")) {
                    validCategories.add(category);
                }
            }
            
            // 如果查询中只包含cs或eess的分类，则保持原样
            if (allCategories.equals(validCategories) && !validCategories.isEmpty()) {
                return originalQuery;
            }
            
            // 如果查询中包含其他分类，需要过滤
            if (!validCategories.isEmpty()) {
                // 构建只包含有效分类的查询
                String filteredQuery = originalQuery;
                for (String cat : allCategories) {
                    if (!validCategories.contains(cat)) {
                        filteredQuery = filteredQuery.replace("cat:" + cat, "");
                    }
                }
                // 清理多余的AND和OR
                filteredQuery = filteredQuery.replaceAll("\\s+(AND|OR)\\s+(AND|OR)\\s+", " $1 ");
                filteredQuery = filteredQuery.replaceAll("^\\s*(AND|OR)\\s+", "");
                filteredQuery = filteredQuery.replaceAll("\\s*(AND|OR)\\s*$", "");
                return filteredQuery.trim();
            }
            
            // 如果查询中没有有效的分类，添加cs和eess的过滤
            return "(" + originalQuery + ") AND (cat:cs.* OR cat:eess.*)";
        }
        
        // 如果查询中没有分类过滤，添加cs和eess的过滤
        return "(" + originalQuery + ") AND (cat:cs.* OR cat:eess.*)";
    }

    /**
     * 确保请求间隔时间
     * arXiv API要求请求间隔至少3秒，以免被限流
     */
    private synchronized void ensureRequestInterval() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRequest = currentTime - lastRequestTime;
        
        if (timeSinceLastRequest < REQUEST_INTERVAL_MS) {
            long sleepTime = REQUEST_INTERVAL_MS - timeSinceLastRequest;
            log.debug("等待 {} 毫秒以满足请求频率限制", sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("请求频率限制中断", e);
            }
        }
        
        lastRequestTime = System.currentTimeMillis();
    }

    /**
     * 计算允许的最大结果数
     * 单日获取上限为1000，日期范围和最近N天上限为天数*1000
     * @param days 天数（null表示单日）
     * @param userMaxResults 用户请求的最大结果数（null表示使用默认值）
     * @return 允许的最大结果数
     */
    private Integer calculateMaxResults(Integer days, Integer userMaxResults) {
        if (days == null || days <= 0) {
            // 单日获取，最大1000
            int maxAllowed = DAILY_MAX_RESULTS;
            // 如果用户没有指定maxResults，则使用最大允许值
            return userMaxResults != null ? Math.min(userMaxResults, maxAllowed) : maxAllowed;
        } else {
            // 日期范围或最近N天，最大为天数*1000
            int maxAllowed = days * DAILY_MAX_RESULTS;
            // 如果用户没有指定maxResults，则使用最大允许值
            return userMaxResults != null ? Math.min(userMaxResults, maxAllowed) : maxAllowed;
        }
    }

    /**
     * 计算两个日期之间的天数
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 天数
     */
    private Integer calculateDaysBetweenDates(String startDate, String endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        } catch (Exception e) {
            log.warn("日期解析失败: startDate={}, endDate={}, 将返回默认值1", startDate, endDate, e);
            return 1;
        }
    }
}