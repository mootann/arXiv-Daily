package com.mootann.arxivdaily.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.dto.CategoryCountDTO;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivPaperQueryRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.repository.model.ArxivPaper;
import com.mootann.arxivdaily.service.ArxivService;
import com.mootann.arxivdaily.task.ArxivSyncTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * arXiv论文控制器
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/arxiv/database")
public class ArxivController {

    private final ArxivService arxivService;

    // 获取数据库中各分类的论文数量统计
    @GetMapping("/stats/categories")
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

    // 分页查询接口
    @PostMapping("/papers")
    public ResponseEntity<ApiResponse<IPage<ArxivPaper>>> searchQuery(@RequestBody ArxivPaperQueryRequest request) {
        log.info("收到查询请求: {}", request);
        IPage<ArxivPaper> papers = arxivService.searchQuery(request);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    // 按分类获取论文
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<IPage<ArxivPaper>>> getPapersByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean hasGithub) {
        
        ArxivPaperQueryRequest request = new ArxivPaperQueryRequest();
        request.setCategory(Collections.singletonList(category));
        request.setPage(page);
        request.setSize(size);
        request.setHasGithub(hasGithub);
        
        IPage<ArxivPaper> papers = arxivService.searchQuery(request);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    // 搜索论文
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<IPage<ArxivPaper>>> searchPapers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean hasGithub) {
        
        ArxivPaperQueryRequest request = new ArxivPaperQueryRequest();
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setHasGithub(hasGithub);
        
        IPage<ArxivPaper> papers = arxivService.searchQuery(request);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    // 按日期范围获取论文
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<IPage<ArxivPaper>>> getPapersByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        ArxivPaperQueryRequest request = new ArxivPaperQueryRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        
        IPage<ArxivPaper> papers = arxivService.searchQuery(request);
        return ResponseEntity.ok(ApiResponse.success(papers));
    }

    //  根据arXiv ID获取论文
    @GetMapping("/paper/{arxivId}")
    public ResponseEntity<ApiResponse<ArxivPaper>> getPaperFromDatabase(@PathVariable String arxivId) {
        log.info("收到从数据库获取论文请求: {}", arxivId);
        ArxivPaper paper = arxivService.getPaperFromDatabase(arxivId);
        if (paper != null) {
            return ResponseEntity.ok(ApiResponse.success(paper));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}