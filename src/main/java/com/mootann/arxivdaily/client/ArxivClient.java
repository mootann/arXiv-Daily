package com.mootann.arxivdaily.client;

import com.mootann.arxivdaily.converter.ArxivEntryMapper;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.util.QueryUtil;
import com.mootann.arxivdaily.xml.ArxivEntry;
import com.mootann.arxivdaily.xml.ArxivFeed;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.mootann.arxivdaily.util.QueryUtil.*;
import static java.time.temporal.ChronoUnit.DAYS;

/**
 * arXiv API客户端
 * 用于与arXiv API进行交互，获取论文信息
 */
@Slf4j
@Component
@AllArgsConstructor
public class ArxivClient {
    
    private static final String ARXIV_API_BASE_URL = "https://export.arxiv.org/api/query";
    private static final String ARXIV_BASE_URL = "https://arxiv.org";
    // 请求间隔时间（毫秒）
    private static final long REQUEST_INTERVAL_MS = 3000;
    // 每日获取论文的最大数量限制
    private static final int DAILY_MAX_RESULTS = 1000;
    // arXiv API单次请求最大返回结果数限制
    private static final int API_MAX_RESULTS_PER_REQUEST = 100;
    // 上次请求的时间
    private static long lastRequestTime = 0;

    private final WebClient webClient;

    private final ArxivEntryMapper arxivEntryMapper;

    /**
     * 搜索论文
     * @param request 搜索请求参数
     * @return 搜索响应结果
     */
    public ArxivSearchResponse searchPapers(ArxivSearchRequest request) {
        // 只查询cs主类下的子类
        String query = filterToCs(request.getQuery());

        log.info("搜索请求参数: query={}, maxResults={}, start={}, sortBy={}, sortOrder={}",
                query, request.getMaxResults(), request.getStart(),
                request.getSortBy(), request.getSortOrder());

        // 执行请求前确保遵守频率限制
        ensureRequestInterval();

        // 使用 UriComponentsBuilder 自动处理 URL 编码
        URI requestUrl = UriComponentsBuilder.fromHttpUrl(ARXIV_API_BASE_URL)
                .queryParam("search_query", UriUtils.encodeQueryParam(query, "UTF-8"))
                .queryParam("max_results", Math.min(request.getMaxResults(), 2000)) // arXiv 最大 2000
                .queryParam("start", request.getStart())
                .queryParam("sortBy", "submittedDate")
                .queryParam("sortOrder", "descending")
                .build(true)
                .toUri();

        log.info("构建的请求URL: {}", requestUrl);
        
        // 使用WebClient执行请求
        try {
            String responseString = webClient.get()
                .uri(requestUrl)
                .header("User-Agent", "ArXiv-Daily/1.0")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (responseString != null && !responseString.isEmpty()) {
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
        String filteredQuery = filterToCs(request.getQuery());
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
     * 根据多个分类和日期范围搜索论文
     * @param categories 分类代码列表，例如：["cs.AI", "cs.LG", "cs.CV"]
     * @param startDate 开始日期，格式：YYYY-MM-DD
     * @param endDate 结束日期，格式：YYYY-MM-DD
     * @return 搜索结果
     */
    public ArxivSearchResponse searchQuery(List<String> categories, String startDate, String endDate) {
        // 计算允许的最大结果数
        Integer maxResults;
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            Integer days = QueryUtil.calculateDaysBetweenDates(startDate, endDate);
            maxResults = days * DAILY_MAX_RESULTS;
        } else {
            // 如果没有日期范围，设置一个默认值
            maxResults = DAILY_MAX_RESULTS;
        }
        
        // 转换日期格式为arXiv API要求的格式（YYYYMMDD）
        String query = QueryUtil.getQuery(categories, startDate, endDate);

        ArxivSearchRequest request = new ArxivSearchRequest(query, maxResults, "0", null, null, null, null);
        return searchPapers(request);
    }

    // 解析arXiv API响应
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
}