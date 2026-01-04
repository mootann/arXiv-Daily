package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.dto.CategoryCountDTO;
import com.mootann.arxivdaily.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.dto.ApiResponse;
import com.mootann.arxivdaily.model.ArxivPaper;
import com.mootann.arxivdaily.service.ArxivService;
import com.mootann.arxivdaily.task.ArxivSyncTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.time.LocalDate;

/**
 * arXiv论文控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/arxiv")
public class ArxivController {
    
    private static final int MAX_RESULTS_LIMIT = 1000;
    
    @Autowired
    private ArxivService arxivService;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ArxivSyncTask dailyArxivSyncTask;
    
    private Integer validateMaxResults(Integer maxResults) {
        if (maxResults == null || maxResults <= 0) {
            return 10;
        }
        return Math.min(maxResults, MAX_RESULTS_LIMIT);
    }
    
    /**
     * 根据arXiv ID获取论文
     * GET /api/arxiv/paper/{arxivId}
     */
    @GetMapping("/paper/{arxivId}")
    public ResponseEntity<ApiResponse<ArxivPaperDTO>> getPaperById(@PathVariable String arxivId) throws UnsupportedEncodingException {
        log.info("收到获取论文请求: {}", arxivId);
        ArxivPaperDTO paper = arxivService.getPaperById(arxivId);
        if (paper != null) {
            return ResponseEntity.ok(ApiResponse.success(paper));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 批量获取论文
     * POST /api/arxiv/papers/batch
     */
    @PostMapping("/papers/batch")
    public ResponseEntity<ApiResponse<List<ArxivPaperDTO>>> getPapersByIds(@RequestBody List<String> arxivIds) throws UnsupportedEncodingException {
        log.info("收到批量获取论文请求，数量: {}", arxivIds.size());
        List<ArxivPaperDTO> papers = arxivService.getPapersByIds(arxivIds);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }
    
    /**
     * 搜索论文
     * POST /api/arxiv/search
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchPapers(@RequestBody ArxivSearchRequest request) {
        log.info("收到搜索论文请求: {}, 来源: {}", request.getQuery(), request.getSource());
        if ("db".equalsIgnoreCase(request.getSource())) {
            int page = 1;
            try {
                if (request.getStart() != null) {
                    page = Integer.parseInt(request.getStart()) / request.getMaxResults() + 1;
                }
            } catch (Exception e) {
                // ignore
            }
            // 对于搜索请求，query字段可能包含前缀如 all: cat: 等，需要处理一下或者是直接传给searchPapersFromDb
            // searchPapersFromDb期望的是关键词，而request.getQuery()可能是 "all:keyword"
            // 简单起见，这里假设直接传关键词，或者在service层处理。
            // 实际上ArxivSearchRequest的query通常是构造好的查询字符串。
            // 如果是高级搜索，可能需要解析。
            // 这里为了简单，我们假设db搜索只支持简单的关键词搜索，或者去掉前缀
            String keyword = request.getQuery();
            if (keyword.startsWith("all:")) keyword = keyword.substring(4);
            
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchPapersFromDb(keyword, page, request.getMaxResults())));
        }
        ArxivSearchResponse response = arxivService.searchPapers(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 按分类搜索论文
     * GET /api/arxiv/category/{category}?maxResults=10&source=api&page=1
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") Integer maxResults,
            @RequestParam(defaultValue = "api") String source,
            @RequestParam(defaultValue = "1") Integer page) {
        log.info("收到按分类搜索请求: {}, 来源: {}, 页码: {}", category, source, page);
        maxResults = validateMaxResults(maxResults);
        if ("db".equalsIgnoreCase(source)) {
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchByCategoryFromDb(category, page, maxResults)));
        }
        ArxivSearchResponse response = arxivService.searchByCategory(category, maxResults, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 按关键词搜索论文
     * GET /api/arxiv/keyword/{keyword}?maxResults=10&source=api&page=1
     */
    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByKeyword(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "10") Integer maxResults,
            @RequestParam(defaultValue = "api") String source,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到按关键词搜索请求: {}, 来源: {}, 页码: {}, hasGithub: {}", keyword, source, page, hasGithub);
        maxResults = validateMaxResults(maxResults);
        if ("db".equalsIgnoreCase(source)) {
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchPapersFromDb(keyword, page, maxResults, hasGithub)));
        }
        ArxivSearchResponse response = arxivService.searchByKeyword(keyword, maxResults, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 按作者搜索论文
     * GET /api/arxiv/author/{author}?maxResults=10
     */
    @GetMapping("/author/{author}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByAuthor(
            @PathVariable String author,
            @RequestParam(defaultValue = "10") Integer maxResults) {
        log.info("收到按作者搜索请求: {}", author);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByAuthor(author, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按分类和日期范围搜索论文
     * GET /api/arxiv/category/{category}/date-range?startDate=2025-01-01&endDate=2025-01-31&maxResults=10&source=api&page=1&hasGithub=true
     */
    @GetMapping("/category/{category}/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategoryAndDateRange(
            @PathVariable String category,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(defaultValue = "api") String source,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Boolean hasGithub) throws UnsupportedEncodingException {
        log.info("收到按分类和日期范围搜索请求: {}, 日期范围: {} 到 {}, 最大结果数: {}, 来源: {}, 页码: {}, hasGithub: {}", category, startDate, endDate, maxResults, source, page, hasGithub);
        // 对于日期范围查询，不在controller层限制maxResults，由client层根据日期范围自动计算
        if ("db".equalsIgnoreCase(source)) {
            // 数据库查询保留原有限制
            maxResults = validateMaxResults(maxResults);
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchByCategoryAndDateRangeFromDb(category, startDate, endDate, page, maxResults, hasGithub)));
        }
        // API查询时，将maxResults传递给service层，由ArxivClient根据日期范围自动计算最大值
        ArxivSearchResponse response = arxivService.searchByCategoryAndDateRange(category, startDate, endDate, maxResults, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按分类和指定日期之后搜索论文
     * GET /api/arxiv/category/{category}/from-date?startDate=2025-01-01&maxResults=10
     */
    @GetMapping("/category/{category}/from-date")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategoryFromDate(
            @PathVariable String category,
            @RequestParam String startDate,
            @RequestParam(defaultValue = "10") Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按分类和日期之后搜索请求: {}, 日期从: {}", category, startDate);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByCategoryFromDate(category, startDate, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按分类和指定日期之前搜索论文
     * GET /api/arxiv/category/{category}/to-date?endDate=2025-01-31&maxResults=10
     */
    @GetMapping("/category/{category}/to-date")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategoryToDate(
            @PathVariable String category,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按分类和日期之前搜索请求: {}, 日期到: {}", category, endDate);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByCategoryToDate(category, endDate, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按日期范围搜索论文
     * GET /api/arxiv/date-range?startDate=2025-01-01&endDate=2025-01-31&maxResults=10&source=api&page=1&hasGithub=true
     */
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(defaultValue = "api") String source,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Boolean hasGithub) throws UnsupportedEncodingException {
        log.info("收到按日期范围搜索请求: {} 到 {}, 最大结果数: {}, 来源: {}, 页码: {}, hasGithub: {}", startDate, endDate, maxResults, source, page, hasGithub);
        // 对于日期范围查询，不在controller层限制maxResults，由client层根据日期范围自动计算
        if ("db".equalsIgnoreCase(source)) {
            // 数据库查询保留原有限制
            maxResults = validateMaxResults(maxResults);
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchByDateRangeFromDb(startDate, endDate, page, maxResults, hasGithub)));
        }
        // API查询时，将maxResults传递给service层，由ArxivClient根据日期范围自动计算最大值
        ArxivSearchResponse response = arxivService.searchByDateRange(startDate, endDate, maxResults, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按指定日期搜索论文
     * GET /api/arxiv/date?date=2025-01-01&maxResults=10
     */
    @GetMapping("/date")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByDate(
            @RequestParam String date,
            @RequestParam(required = false) Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按日期搜索请求: {}, 最大结果数: {}", date, maxResults);
        // 对于单日查询，不在controller层限制maxResults，由client层自动计算（单日最大1000）
        ArxivSearchResponse response = arxivService.searchByDate(date, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取最近N天的论文
     * GET /api/arxiv/recent?days=7&maxResults=10
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchRecentPapers(
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到获取最近论文请求，天数: {}, 最大结果数: {}", days, maxResults);
        // 对于最近N天查询，不在controller层限制maxResults，由client层根据天数自动计算（天数*1000）
        ArxivSearchResponse response = arxivService.searchRecentPapers(days, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取最近N天指定分类的论文
     * GET /api/arxiv/category/{category}/recent?days=7&maxResults=10&source=api&page=1&hasGithub=true
     */
    @GetMapping("/category/{category}/recent")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchRecentPapersByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "7") Integer days,
            @RequestParam(required = false) Integer maxResults,
            @RequestParam(defaultValue = "api") String source,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(required = false) Boolean hasGithub) throws UnsupportedEncodingException {
        log.info("收到获取最近分类论文请求: {}, 天数: {}, 最大结果数: {}, 来源: {}, 页码: {}, hasGithub: {}", category, days, maxResults, source, page, hasGithub);
        // 对于最近N天查询，不在controller层限制maxResults，由client层根据天数自动计算（天数*1000）
        if ("db".equalsIgnoreCase(source)) {
            // 数据库查询保留原有限制
            maxResults = validateMaxResults(maxResults);
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(days);
            return ResponseEntity.ok(ApiResponse.success(arxivService.searchByCategoryAndDateRangeFromDb(category, start.toString(), end.toString(), page, maxResults, hasGithub)));
        }
        // API查询时，将maxResults传递给service层，由ArxivClient根据天数自动计算最大值
        ArxivSearchResponse response = arxivService.searchRecentPapersByCategory(category, days, maxResults, page);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按多个分类搜索论文
     * POST /api/arxiv/categories/multiple
     */
    @PostMapping("/categories/multiple")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByMultipleCategories(
            @RequestBody List<String> categories,
            @RequestParam(defaultValue = "10") Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按多个分类搜索请求: {}", categories);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByMultipleCategories(categories, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按多个分类和日期范围搜索论文
     * POST /api/arxiv/categories/multiple/date-range
     */
    @PostMapping("/categories/multiple/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByMultipleCategoriesAndDateRange(
            @RequestBody List<String> categories,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按多个分类和日期范围搜索请求: {}, 日期范围: {} 到 {}", categories, startDate, endDate);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByMultipleCategoriesAndDateRange(categories, startDate, endDate, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 按分类和关键词搜索论文
     * GET /api/arxiv/category/{category}/keyword/{keyword}?maxResults=10
     */
    @GetMapping("/category/{category}/keyword/{keyword}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategoryAndKeyword(
            @PathVariable String category,
            @PathVariable String keyword,
            @RequestParam(defaultValue = "10") Integer maxResults) throws UnsupportedEncodingException {
        log.info("收到按分类和关键词搜索请求: {}, 关键词: {}", category, keyword);
        maxResults = validateMaxResults(maxResults);
        ArxivSearchResponse response = arxivService.searchByCategoryAndKeyword(category, keyword, maxResults);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 数据库查询接口 ====================

    /**
     * 获取数据库中各分类的论文数量统计
     * GET /api/arxiv/database/stats/categories
     */
    @GetMapping("/database/stats/categories")
    public ResponseEntity<ApiResponse<List<CategoryCountDTO>>> getCategoryCountsFromDatabase(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("收到获取数据库分类统计请求, startDate: {}, endDate: {}", startDate, endDate);
        List<CategoryCountDTO> counts;
        if (startDate != null && endDate != null) {
            try {
                counts = arxivService.getCategoryCountsFromDatabase(LocalDate.parse(startDate), LocalDate.parse(endDate));
            } catch (Exception e) {
                log.error("解析日期失败", e);
                counts = arxivService.getCategoryCountsFromDatabase();
            }
        } else {
            counts = arxivService.getCategoryCountsFromDatabase();
        }
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    /**
     * 获取数据库中最新的论文发布日期
     * GET /api/arxiv/database/latest-date
     */
    @GetMapping("/database/latest-date")
    public ResponseEntity<ApiResponse<String>> getLatestPublishedDate() {
        log.info("收到获取最新论文发布日期请求");
        LocalDate latestDate = arxivService.getLatestPublishedDate();
        if (latestDate != null) {
            return ResponseEntity.ok(ApiResponse.success(latestDate.toString()));
        } else {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    /**
     * 分页查询所有论文
     * GET /api/arxiv/database/papers?page=1&size=10
     */
    @GetMapping("/database/papers")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> getPapersFromDatabase(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库查询论文请求，页码: {}, 每页数量: {}, hasGithub: {}", page, size, hasGithub);
        Page<ArxivPaper> papers = arxivService.getPapersFromDatabase(page, size, hasGithub);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据关键词查询论文
     * GET /api/arxiv/database/search?keyword=xxx&page=1&size=10
     */
    @GetMapping("/database/search")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> searchPapersFromDatabase(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库搜索论文请求，关键词: {}, 页码: {}, 每页数量: {}, hasGithub: {}", keyword, page, size, hasGithub);
        Page<ArxivPaper> papers = arxivService.searchPapersFromDatabase(keyword, page, size, hasGithub);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据分类查询论文
     * GET /api/arxiv/database/category/{category}?page=1&size=10
     */
    @GetMapping("/database/category/{category}")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> getPapersByCategoryFromDatabase(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库查询分类论文请求: {}, 页码: {}, 每页数量: {}, hasGithub: {}", category, page, size, hasGithub);
        Page<ArxivPaper> papers = arxivService.getPapersByCategoryFromDatabase(category, page, size, hasGithub);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据日期范围查询论文
     * GET /api/arxiv/database/date-range?startDate=2025-01-01&endDate=2025-01-31&page=1&size=10&hasGithub=true
     */
    @GetMapping("/database/date-range")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> getPapersByDateRangeFromDatabase(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库查询日期范围论文请求: {} 到 {}, 页码: {}, 每页数量: {}, hasGithub: {}", startDate, endDate, page, size, hasGithub);
        // 使用searchByDateRangeFromDb复用逻辑，但它返回的是ArxivSearchResponse，这里需要返回Page<ArxivPaper>
        // 所以我们应该直接调用Service的getPage方法
        Page<ArxivPaper> papers;
        if (hasGithub == null) {
            papers = arxivService.getPapersByDateRangeFromDatabase(startDate, endDate, page, size);
        } else {
            // 需要在Service中公开支持hasGithub的Page返回方法
            // 目前searchByDateRangeFromDb内部调用了private或者protected方法，或者我们需要新增一个公开方法
            // 检查ArxivService发现 getPapersByDateRangeFromDatabase 还没有重载支持hasGithub
            // 我们之前修改了 searchByDateRangeFromDb (public) -> 内部逻辑
            // 让我们先去 ArxivService 增加一个支持 hasGithub 的 getPapersByDateRangeFromDatabase
             papers = arxivService.getPapersByDateRangeFromDatabase(startDate, endDate, page, size, hasGithub);
        }
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据分类和日期范围查询论文
     * GET /api/arxiv/database/category/{category}/date-range?startDate=2025-01-01&endDate=2025-01-31&page=1&size=10&hasGithub=true
     */
    @GetMapping("/database/category/{category}/date-range")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> getPapersByCategoryAndDateRangeFromDatabase(
            @PathVariable String category,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库查询分类和日期范围论文请求: {}, {} 到 {}, 页码: {}, 每页数量: {}, hasGithub: {}", category, startDate, endDate, page, size, hasGithub);
        Page<ArxivPaper> papers = arxivService.getPapersByCategoryAndDateRangeFromDatabase(category, startDate, endDate, page, size, hasGithub);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据分类和关键词查询论文
     * GET /api/arxiv/database/category/{category}/keyword/{keyword}?page=1&size=10
     */
    @GetMapping("/database/category/{category}/keyword/{keyword}")
    public ResponseEntity<ApiResponse<Page<ArxivPaper>>> getPapersByCategoryAndKeywordFromDatabase(
            @PathVariable String category,
            @PathVariable String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Boolean hasGithub) {
        log.info("收到从数据库查询分类和关键词论文请求: {}, 关键词: {}, 页码: {}, 每页数量: {}, hasGithub: {}", category, keyword, page, size, hasGithub);
        Page<ArxivPaper> papers = arxivService.getPapersByCategoryAndKeywordFromDatabase(category, keyword, page, size, hasGithub);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 根据arXiv ID获取论文
     * GET /api/arxiv/database/paper/{arxivId}
     */
    @GetMapping("/database/paper/{arxivId}")
    public ResponseEntity<ApiResponse<ArxivPaper>> getPaperFromDatabase(@PathVariable String arxivId) {
        log.info("收到从数据库获取论文请求: {}", arxivId);
        ArxivPaper paper = arxivService.getPaperFromDatabase(arxivId);
        if (paper != null) {
            return ResponseEntity.ok(ApiResponse.success(paper));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 批量获取论文
     * POST /api/arxiv/database/papers/batch
     */
    @PostMapping("/database/papers/batch")
    public ResponseEntity<ApiResponse<List<ArxivPaper>>> getPapersByIdsFromDatabase(@RequestBody List<String> arxivIds) {
        log.info("收到从数据库批量获取论文请求，数量: {}", arxivIds.size());
        List<ArxivPaper> papers = arxivService.getPapersByIdsFromDatabase(arxivIds);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    /**
     * 清除论文列表缓存（流式刷新后调用）
     * POST /api/arxiv/cache/clear
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<ApiResponse<String>> clearPapersCache() {
        log.info("收到清除论文缓存请求");
        arxivService.clearPapersCache();
        return ResponseEntity.ok(ApiResponse.success("缓存已清除"));
    }

    /**
     * 清除指定分类的缓存
     * POST /api/arxiv/cache/clear/{category}
     */
    @PostMapping("/cache/clear/{category}")
    public ResponseEntity<ApiResponse<String>> clearCategoryCache(@PathVariable String category) {
        log.info("收到清除分类缓存请求: {}", category);
        arxivService.clearCategoryCache(category);
        return ResponseEntity.ok(ApiResponse.success("分类缓存已清除: " + category));
    }

    /**
     * 获取论文PDF
     * GET /api/arxiv/pdf/{arxivId}
     */
    @GetMapping("/pdf/{arxivId}")
    public ResponseEntity<byte[]> getPdfByArxivId(@PathVariable String arxivId) {
        log.info("收到获取PDF请求: {}", arxivId);
        try {
            String pdfUrl = "https://arxiv.org/pdf/" + arxivId + ".pdf";
            byte[] pdfData = webClient.get()
                    .uri(pdfUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            
            if (pdfData != null) {
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"" + arxivId + ".pdf\"")
                    .body(pdfData);
            } else {
                log.error("下载PDF失败: {}", pdfUrl);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("获取PDF异常: {}", arxivId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动触发每日arXiv论文同步任务
     * POST /api/v1/arxiv/sync/trigger
     */
    @PostMapping("/sync/trigger")
    public ResponseEntity<ApiResponse<String>> triggerDailySync() {
        log.info("收到手动触发每日同步任务请求");
        try {
            // 异步执行同步任务，避免阻塞请求
            new Thread(() -> dailyArxivSyncTask.manualSync()).start();
            return ResponseEntity.ok(ApiResponse.success("每日同步任务已启动"));
        } catch (Exception e) {
            log.error("手动触发同步任务失败", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error(500, "触发同步任务失败: " + e.getMessage()));
        }
    }
}