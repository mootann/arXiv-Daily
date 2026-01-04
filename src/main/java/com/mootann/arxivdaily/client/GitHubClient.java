package com.mootann.arxivdaily.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mootann.arxivdaily.config.GitHubConfig;
import com.mootann.arxivdaily.dto.GitHubRepositoryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Security.TrustStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import javax.net.ssl.SSLContext;

/**
 * GitHub API客户端
 * 用于获取GitHub仓库信息（stars、forks等）
 */
@Slf4j
@Component
public class GitHubClient {
    
    private final WebClient webClient;
    private final GitHubConfig gitHubConfig;
    
    public GitHubClient(@Autowired GitHubConfig gitHubConfig, @Autowired WebClient webClient) {
        this.webClient = webClient;
        this.gitHubConfig = gitHubConfig;
        log.info("GitHub配置: repository={}, apiBaseUrl={}, proxyEnabled={}", 
            gitHubConfig.getRepository(), gitHubConfig.getApiBaseUrl(), gitHubConfig.getProxyEnabled());
    }
    
    /**
     * 获取仓库信息
     * @return 仓库信息
     */
    public GitHubRepositoryInfo getRepositoryInfo() {
        if (gitHubConfig.getRepository() == null || gitHubConfig.getRepository().isEmpty()) {
            log.error("GitHub仓库地址未配置");
            return null;
        }
        
        String url = String.format("%s/repos/%s", gitHubConfig.getApiBaseUrl(), gitHubConfig.getRepository());
        log.info("获取GitHub仓库信息: {}", url);
        
        try {
            String responseString = webClient.get()
                .uri(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ArXiv-Daily/1.0")
                .headers(headers -> {
                    if (gitHubConfig.getAccessToken() != null && !gitHubConfig.getAccessToken().isEmpty()) {
                        headers.setBearerAuth(gitHubConfig.getAccessToken());
                    }
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.info("GitHub API响应内容: {}", responseString);
            
            if (responseString != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                GitHubRepositoryInfo info = objectMapper.readValue(responseString, GitHubRepositoryInfo.class);
                log.info("成功解析仓库信息: stars={}, forks={}", info.getStarsCount(), info.getForksCount());
                return info;
            } else {
                log.error("GitHub API返回空响应");
                return null;
            }
        } catch (Exception e) {
            log.error("获取GitHub仓库信息失败", e);
            return null;
        }
    }
}
