package com.mootann.arxivdaily.task;

import com.mootann.arxivdaily.client.ArxivClient;
import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.dto.arxiv.ArxivSearchResponse;
import com.mootann.arxivdaily.service.ArxivService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 每日定时同步arXiv论文任务
 * 每天凌晨1点自动获取当天发布的所有arxiv论文
 */
@Slf4j
@Component
public class DailyArxivSyncTask {

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private ArxivClient arxivClient;

    @Autowired
    private ArxivService arxivService;

    // arXiv API单次请求最大返回结果数限制
    private static final int API_MAX_RESULTS_PER_REQUEST = 100;
    // 请求间隔时间（毫秒），arXiv API建议至少3秒
    private static final long REQUEST_INTERVAL_MS = 3000;

    /**
     * 每日凌晨1点执行定时任务
     * cron表达式：0 0 1 * * ? 表示每天凌晨1点0分0秒执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void syncDailyArxivPapers() {
        log.info("========== 开始执行每日arXiv论文同步任务 ==========");
        
        try {
            // 获取当天日期
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            log.info("准备获取 {} 发布的所有arxiv论文", todayStr);
            
            // 获取当天发布的所有论文
            List<ArxivPaperDTO> allPapers = fetchAllPapersForDate(todayStr);
            
            if (allPapers.isEmpty()) {
                log.warn("未获取到今天新发布论文");
            } else {
                log.info("成功获取 {} 篇论文，准备保存到数据库", allPapers.size());
                
                // 保存到数据库
                int savedCount = arxivService.savePapersToDatabase(allPapers);
                
                log.info("本次同步完成: 获取 {} 篇论文，保存 {} 篇新论文到数据库", 
                    allPapers.size(), savedCount);

                redisClient.deleteByPattern(RedisClient.PAPERS_PREFIX + "*");
                log.info("已清除论文列表缓存");
            }
            
        } catch (Exception e) {
            log.error("每日arXiv论文同步任务执行失败", e);
        }
        
        log.info("========== 每日arXiv论文同步任务执行完成 ==========");
    }

    /**
     * 获取指定日期发布的所有论文
     * 通过多次分页请求获取所有论文
     * 
     * @param date 日期，格式：YYYY-MM-DD
     * @return 该日期发布的所有论文列表
     */
    private List<ArxivPaperDTO> fetchAllPapersForDate(String date) {
        List<ArxivPaperDTO> allPapers = new ArrayList<>();
        int currentStart = 0;
        boolean hasMoreResults = true;
        int batchNumber = 1;

        log.info("开始获取 {} 的所有论文，不限制数量", date);

        while (hasMoreResults) {
            try {
                log.info("第 {} 批请求: 起始位置={}, 每批数量={}", 
                    batchNumber, currentStart, API_MAX_RESULTS_PER_REQUEST);

                // 调用ArxivClient获取指定日期的论文
                ArxivSearchResponse response = arxivClient.searchByDateRange(
                    date, 
                    date, 
                    API_MAX_RESULTS_PER_REQUEST, 
                    (currentStart / API_MAX_RESULTS_PER_REQUEST) + 1
                );

                if (response == null || response.getPapers() == null || response.getPapers().isEmpty()) {
                    log.info("第 {} 批请求返回空结果，停止获取", batchNumber);
                    hasMoreResults = false;
                    break;
                }

                // 累加本批次获取的论文
                int currentBatchSize = response.getPapers().size();
                allPapers.addAll(response.getPapers());
                log.info("第 {} 批请求成功: 本批获取 {} 篇，累计获取 {} 篇", 
                    batchNumber, currentBatchSize, allPapers.size());

                // 更新起始位置
                currentStart += currentBatchSize;

                // 判断是否还有更多结果
                // 如果本批次返回的数量小于请求的数量，说明已经获取完所有结果
                if (currentBatchSize < API_MAX_RESULTS_PER_REQUEST) {
                    log.info("本批次返回数量 ({}) 小于请求数量 ({}), 已获取所有论文", 
                        currentBatchSize, API_MAX_RESULTS_PER_REQUEST);
                    hasMoreResults = false;
                } else if (response.getTotalResults() > 0 && currentStart >= response.getTotalResults()) {
                    log.info("已获取全部论文: 累计={}, 总数={}", currentStart, response.getTotalResults());
                    hasMoreResults = false;
                }

                batchNumber++;

                // 遵守arXiv API的请求频率限制（每次请求间隔至少3秒）
                if (hasMoreResults) {
                    log.debug("等待 {} 毫秒以满足API请求频率限制", REQUEST_INTERVAL_MS);
                    Thread.sleep(REQUEST_INTERVAL_MS);
                }

            } catch (InterruptedException e) {
                log.error("请求过程中被中断", e);
                Thread.currentThread().interrupt();
                hasMoreResults = false;
            } catch (Exception e) {
                log.error("第 {} 批请求失败", batchNumber, e);
                // 遇到错误时，停止继续获取
                hasMoreResults = false;
            }
        }

        log.info("完成获取 {} 的所有论文，共 {} 篇", date, allPapers.size());
        return allPapers;
    }

    /**
     * 手动触发同步任务（用于测试）
     * 可以通过Controller调用此方法手动触发同步
     */
    public void manualSync() {
        log.info("手动触发每日arXiv论文同步任务");
        syncDailyArxivPapers();
    }
}
