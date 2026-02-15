package com.mootann.arxivdaily.service;

import com.mootann.arxivdaily.client.GitHubClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.repository.dto.GitHubRepositoryInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * GitHub服务
 * 处理GitHub相关的业务逻辑
 */
@Slf4j
@Service
@AllArgsConstructor
public class GitHubService {
    
    private final GitHubClient gitHubClient;

    private final RedisClient redisClient;
    
    /**
     * 获取仓库信息（带缓存）
     * @return 仓库信息
     */
    public GitHubRepositoryInfo getRepositoryInfo() {
        String cacheKey = RedisClient.GITHUB_REPO_INFO;

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof GitHubRepositoryInfo) {
            log.info("从Redis缓存获取GitHub仓库信息 stars:{}, forks:{}", ((GitHubRepositoryInfo) cachedValue).getStarsCount(), ((GitHubRepositoryInfo) cachedValue).getForksCount());
            return (GitHubRepositoryInfo) cachedValue;
        }

        // 缓存未命中，调用GitHubClient获取
        log.info("获取GitHub仓库信息");
        GitHubRepositoryInfo info = gitHubClient.getRepositoryInfo();

        if (info != null) {
            // 保存到Redis缓存，过期时间1天
            redisClient.set(cacheKey, info, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            log.info("GitHub仓库信息已缓存到Redis");
        }

        return info;
    }
}
