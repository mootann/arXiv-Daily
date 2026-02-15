package com.mootann.arxivdaily.task;

import com.mootann.arxivdaily.client.GitHubClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.repository.dto.GitHubRepositoryInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * GitHub仓库信息同步定时任务
 * 定期从GitHub API获取仓库信息并更新到Redis缓存
 */
@Slf4j
@Component
@AllArgsConstructor
public class GithubSyncTask {

    private final GitHubClient gitHubClient;

    private final RedisClient redisClient;

    // 每天凌晨1点0分0秒执行
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncGitHubRepositoryInfo() {
        log.info("========== 开始执行GitHub仓库信息同步任务 ==========");
        
        try {
            GitHubRepositoryInfo info = gitHubClient.getRepositoryInfo();
            
            if (info != null) {
                redisClient.set(
                    RedisClient.GITHUB_REPO_INFO, 
                    info, 
                    RedisClient.ONE_DAY_HOURS, 
                    TimeUnit.HOURS
                );
                log.info("GitHub仓库信息已更新到缓存: stars={}, forks={}", 
                    info.getStarsCount(), info.getForksCount());
            } else {
                log.warn("未能获取到GitHub仓库信息");
            }
            
        } catch (Exception e) {
            log.error("GitHub仓库信息同步任务执行失败", e);
        }
        
        log.info("========== GitHub仓库信息同步任务执行完成 ==========");
    }
}