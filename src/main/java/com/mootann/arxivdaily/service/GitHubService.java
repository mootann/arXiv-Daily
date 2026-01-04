package com.mootann.arxivdaily.service;

import com.mootann.arxivdaily.client.GitHubClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.dto.GitHubRepositoryInfo;
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
public class GitHubService {
    
    @Autowired
    private GitHubClient gitHubClient;

    @Autowired
    private RedisClient redisClient;
    
    /**
     * 获取仓库信息（带缓存）
     * @return 仓库信息
     */
    public GitHubRepositoryInfo getRepositoryInfo() {
        String cacheKey = RedisClient.GITHUB_REPO_INFO;

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof GitHubRepositoryInfo) {
            log.info("从Redis缓存获取GitHub仓库信息");
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
    
    /**
     * 获取Stars数量（带缓存）
     * @return Stars数量
     */
    public Integer getStarsCount() {
        String cacheKey = RedisClient.GITHUB_STARS;

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof Integer) {
            log.info("从Redis缓存获取GitHub仓库Stars数量");
            return (Integer) cachedValue;
        }

        // 缓存未命中，调用GitHubClient获取
        log.info("获取GitHub仓库Stars数量");
        Integer stars = gitHubClient.getStarsCount();

        if (stars != null) {
            // 保存到Redis缓存，过期时间1天
            redisClient.set(cacheKey, stars, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            log.info("GitHub仓库Stars数量已缓存到Redis");
        }

        return stars;
    }
    
    /**
     * 获取Fork数量（带缓存）
     * @return Fork数量
     */
    public Integer getForksCount() {
        String cacheKey = RedisClient.GITHUB_FORKS;

        // 先从Redis缓存获取
        Object cachedValue = redisClient.get(cacheKey);
        if (cachedValue instanceof Integer) {
            log.info("从Redis缓存获取GitHub仓库Fork数量");
            return (Integer) cachedValue;
        }

        // 缓存未命中，调用GitHubClient获取
        log.info("获取GitHub仓库Fork数量");
        Integer forks = gitHubClient.getForksCount();

        if (forks != null) {
            // 保存到Redis缓存，过期时间1天
            redisClient.set(cacheKey, forks, RedisClient.ONE_DAY_HOURS, TimeUnit.HOURS);
            log.info("GitHub仓库Fork数量已缓存到Redis");
        }

        return forks;
    }
}
