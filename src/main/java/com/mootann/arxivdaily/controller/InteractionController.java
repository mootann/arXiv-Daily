package com.mootann.arxivdaily.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.dto.UserFollowCategoryDTO;
import com.mootann.arxivdaily.repository.model.ArxivPaper;
import com.mootann.arxivdaily.repository.model.PaperComment;
import com.mootann.arxivdaily.repository.model.UserCollect;
import com.mootann.arxivdaily.service.ArxivService;
import com.mootann.arxivdaily.service.InteractionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1")
public class InteractionController {

    private final InteractionService interactionService;

    private final ArxivService arxivService;

    // 关注分类相关
    @GetMapping("/user/follow/categories")
    public ResponseEntity<ApiResponse<List<UserFollowCategoryDTO>>> getFollowedCategories() {
        return ResponseEntity.ok(ApiResponse.success(interactionService.getUserFollowedCategories()));
    }

    @PostMapping("/user/follow/category")
    public ResponseEntity<ApiResponse<Void>> followCategory(@RequestParam String category) {
        interactionService.followCategory(category);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/user/follow/category/{category}")
    public ResponseEntity<ApiResponse<Void>> unfollowCategory(@PathVariable String category) {
        interactionService.unfollowCategory(category);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 用户收藏相关
    @GetMapping("/user/collect/papers")
    public ResponseEntity<ApiResponse<IPage<ArxivPaper>>> getCollectedPapers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<UserCollect> collects = interactionService.getUserCollects(page, size);
        
        // 转换 IDs 到 Papers
        List<String> arxivIds = collects.getRecords().stream()
                .map(UserCollect::getArxivId)
                .collect(Collectors.toList());
        List<ArxivPaper> papers = arxivService.getPapersByArxivIds(arxivIds);
        
        // 构造新的 Page 对象
        IPage<ArxivPaper> paperPage = new Page<>(collects.getCurrent(), collects.getSize(), collects.getTotal());
        paperPage.setRecords(papers);
        paperPage.setPages(collects.getPages());
        
        return ResponseEntity.ok(ApiResponse.success(paperPage)); 
    }

    @PostMapping("/user/collect/paper")
    public ResponseEntity<ApiResponse<Void>> collectPaper(@RequestParam String arxivId) {
        interactionService.collectPaper(arxivId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/user/collect/paper/{arxivId}")
    public ResponseEntity<ApiResponse<Void>> uncollectPaper(@PathVariable String arxivId) {
        interactionService.uncollectPaper(arxivId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 用户点赞相关
    @PostMapping("/user/like/paper")
    public ResponseEntity<ApiResponse<Void>> likePaper(@RequestParam String arxivId) {
        interactionService.likePaper(arxivId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/user/like/paper/{arxivId}")
    public ResponseEntity<ApiResponse<Void>> unlikePaper(@PathVariable String arxivId) {
        interactionService.unlikePaper(arxivId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 评论相关
    @GetMapping("/paper/comment/{arxivId}")
    public ResponseEntity<ApiResponse<IPage<PaperComment>>> getComments(
            @PathVariable String arxivId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<PaperComment> comments = interactionService.getComments(arxivId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/paper/comment")
    public ResponseEntity<ApiResponse<Void>> addComment(@RequestBody CommentRequest request) {
        interactionService.addComment(request.getArxivId(), request.getContent());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Data
    public static class CommentRequest {
        private String arxivId;
        private String content;
    }
}
