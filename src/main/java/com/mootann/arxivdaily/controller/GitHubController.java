package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.dto.ApiResponse;
import com.mootann.arxivdaily.dto.GitHubRepositoryInfo;
import com.mootann.arxivdaily.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * GitHub控制器
 * 提供GitHub仓库信息相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/github")
public class GitHubController {
    
    @Autowired
    private GitHubService gitHubService;
    
    /**
     * 获取仓库完整信息
     * @return 仓库信息
     */
    @GetMapping("/repository")
    public ApiResponse<GitHubRepositoryInfo> getRepositoryInfo() {
        try {
            GitHubRepositoryInfo info = gitHubService.getRepositoryInfo();
            if (info != null) {
                return ApiResponse.success(info);
            } else {
                return ApiResponse.error("获取仓库信息失败");
            }
        } catch (Exception e) {
            log.error("获取仓库信息异常", e);
            return ApiResponse.error("获取仓库信息异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取Stars和Forks数量
     * @return Stars和Forks数量
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Integer>> getRepositoryStats() {
        try {
            Map<String, Integer> stats = new HashMap<>();
            stats.put("stars", gitHubService.getStarsCount());
            stats.put("forks", gitHubService.getForksCount());
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取仓库统计信息异常", e);
            return ApiResponse.error("获取仓库统计信息异常: " + e.getMessage());
        }
    }
    
    /**
     * 仅获取Stars数量
     * @return Stars数量
     */
    @GetMapping("/stars")
    public ApiResponse<Integer> getStarsCount() {
        try {
            Integer stars = gitHubService.getStarsCount();
            if (stars != null) {
                return ApiResponse.success(stars);
            } else {
                return ApiResponse.error("获取Stars数量失败");
            }
        } catch (Exception e) {
            log.error("获取Stars数量异常", e);
            return ApiResponse.error("获取Stars数量异常: " + e.getMessage());
        }
    }
    
    /**
     * 仅获取Fork数量
     * @return Fork数量
     */
    @GetMapping("/forks")
    public ApiResponse<Integer> getForksCount() {
        try {
            Integer forks = gitHubService.getForksCount();
            if (forks != null) {
                return ApiResponse.success(forks);
            } else {
                return ApiResponse.error("获取Fork数量失败");
            }
        } catch (Exception e) {
            log.error("获取Fork数量异常", e);
            return ApiResponse.error("获取Fork数量异常: " + e.getMessage());
        }
    }
}
