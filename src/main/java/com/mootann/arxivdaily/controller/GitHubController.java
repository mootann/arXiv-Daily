package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.dto.GitHubRepositoryInfo;
import com.mootann.arxivdaily.service.GitHubService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub控制器
 * 提供GitHub仓库信息相关的API接口
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/github")
public class GitHubController {
    
    private final GitHubService gitHubService;
    
    /**
     * 获取仓库完整信息
     * @return 仓库信息
     */
    @GetMapping("/repository")
    public ResponseEntity<ApiResponse<GitHubRepositoryInfo>> getRepositoryInfo() {
        try {
            GitHubRepositoryInfo info = gitHubService.getRepositoryInfo();
            if (info != null) {
                return ResponseEntity.ok(ApiResponse.success(info));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("获取仓库信息失败"));
            }
        } catch (Exception e) {
            log.error("获取仓库信息异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取仓库信息异常: " + e.getMessage()));
        }
    }
}
