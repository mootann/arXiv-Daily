package com.mootann.arxivdaily.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mootann.arxivdaily.config.GitHubConfig;
import com.mootann.arxivdaily.dto.GitHubRepositoryInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;

/**
 * GitHub API客户端
 * 用于获取GitHub仓库信息（stars、forks等）
 */
@Slf4j
@Component
public class GitHubClient {
    
    private final ObjectMapper objectMapper;
    private final GitHubConfig gitHubConfig;
    
    public GitHubClient(@Autowired GitHubConfig gitHubConfig, @Autowired ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(url);
            
            httpGet.setHeader("Accept", "application/vnd.github.v3+json");
            httpGet.setHeader("User-Agent", "ArXiv-Daily/1.0");
            
            if (gitHubConfig.getAccessToken() != null && !gitHubConfig.getAccessToken().isEmpty()) {
                httpGet.setHeader("Authorization", "Bearer " + gitHubConfig.getAccessToken());
            }
            
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                int statusCode = httpResponse.getCode();
                String responseString = EntityUtils.toString(httpResponse.getEntity());
                
                log.info("GitHub API响应状态码: {}", statusCode);
                log.debug("GitHub API响应内容: {}", responseString);
                
                if (statusCode == 200) {
                    try {
                        GitHubRepositoryInfo info = objectMapper.readValue(responseString, GitHubRepositoryInfo.class);
                        log.info("成功解析仓库信息: stars={}, forks={}", info.getStarsCount(), info.getForksCount());
                        return info;
                    } catch (Exception parseException) {
                        log.error("解析GitHub API响应失败: {}", parseException.getMessage(), parseException);
                        log.error("原始响应内容: {}", responseString);
                        return null;
                    }
                } else {
                    log.error("GitHub API返回错误: status={}, response={}", statusCode, responseString);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("获取GitHub仓库信息失败", e);
            return null;
        }
    }
    
    /**
     * 获取仓库的Stars数量
     * @return Stars数量
     */
    public Integer getStarsCount() {
        GitHubRepositoryInfo info = getRepositoryInfo();
        return info != null ? info.getStarsCount() : null;
    }
    
    /**
     * 获取仓库的Fork数量
     * @return Fork数量
     */
    public Integer getForksCount() {
        GitHubRepositoryInfo info = getRepositoryInfo();
        return info != null ? info.getForksCount() : null;
    }
    
    /**
     * 创建HTTP客户端
     * @return CloseableHttpClient实例
     */
    private CloseableHttpClient createHttpClient() throws Exception {
        if (gitHubConfig.getProxyEnabled() != null && gitHubConfig.getProxyEnabled() 
            && gitHubConfig.getProxyHost() != null && gitHubConfig.getProxyPort() != null) {
            TrustStrategy trustStrategy = (chain, authType) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, trustStrategy).build();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE);
            
            HttpHost proxy = new HttpHost(gitHubConfig.getProxyHost(), gitHubConfig.getProxyPort());
            
            return HttpClients.custom()
                .setProxy(proxy)
                .setConnectionManager(
                    PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build())
                .build();
        } else {
            return HttpClients.createDefault();
        }
    }
}
