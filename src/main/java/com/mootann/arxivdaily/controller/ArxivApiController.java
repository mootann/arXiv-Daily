package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchRequest;
import com.mootann.arxivdaily.repository.dto.CategoryCountDTO;
import com.mootann.arxivdaily.repository.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.model.ArxivPaper;
import com.mootann.arxivdaily.service.ArxivService;
import com.mootann.arxivdaily.task.ArxivSyncTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.time.LocalDate;

/**
 * arXiv论文控制器
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/arxiv")
public class ArxivApiController {

    private final ArxivService arxivService;

    private final WebClient webClient;

    // 按分类搜索论文
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategory(@PathVariable String category) {
        log.info("收到按分类搜索请求: {}", category);
        ArxivSearchResponse response = arxivService.searchQuery(Collections.singletonList(category), null, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // 按关键词搜索论文
    @GetMapping("/keyword/{keyword}")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByKeyword(@PathVariable String keyword) {
        log.info("收到按关键词搜索请求: {}", keyword);
        ArxivSearchResponse response = arxivService.searchByKeyword(keyword);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 按分类和日期范围搜索论文
    @GetMapping("/category/{category}/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByCategoryAndDateRange(
            @PathVariable String category,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("收到按分类和日期范围搜索请求: {}, 日期范围: {} 到 {}", category, startDate, endDate);
        ArxivSearchResponse response = arxivService.searchQuery(Collections.singletonList(category), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 按日期范围搜索论文
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("收到按日期范围搜索请求: {} 到 {}", startDate, endDate);
        ArxivSearchResponse response = arxivService.searchQuery(null, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 按指定日期搜索论文
    @GetMapping("/date")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByDate(
            @RequestParam String date) {
        log.info("收到按日期搜索请求: {}", date);
        ArxivSearchResponse response = arxivService.searchQuery(null, date, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 获取最近N天的论文
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchRecentPapers(@RequestParam(defaultValue = "7") Integer days) {
        log.info("收到获取最近论文请求，天数: {}", days);
        String startDate = LocalDate.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ArxivSearchResponse response = arxivService.searchQuery(null, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 获取最近N天指定分类的论文
    @GetMapping("/category/{category}/recent")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchRecentPapersByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "7") Integer days) {
        log.info("收到获取最近分类论文请求: {}, 天数: {}", category, days);
        String startDate = LocalDate.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        ArxivSearchResponse response = arxivService.searchQuery(Collections.singletonList(category), startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 按多个分类搜索论文
    @PostMapping("/categories/multiple")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByMultipleCategories(@RequestBody List<String> categories) {
        log.info("收到按多个分类搜索请求: {}", categories);
        ArxivSearchResponse response = arxivService.searchQuery(categories, null, null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 按多个分类和日期范围搜索论文
    @PostMapping("/categories/multiple/date-range")
    public ResponseEntity<ApiResponse<ArxivSearchResponse>> searchByMultipleCategoriesAndDateRange(
            @RequestBody List<String> categories,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("收到按多个分类和日期范围搜索请求: {}, 日期范围: {} 到 {}", categories, startDate, endDate);
        ArxivSearchResponse response = arxivService.searchQuery(categories, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 获取论文PDF
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
}