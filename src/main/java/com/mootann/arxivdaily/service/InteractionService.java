package com.mootann.arxivdaily.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mootann.arxivdaily.repository.dto.UserFollowCategoryDTO;
import com.mootann.arxivdaily.repository.mapper.*;
import com.mootann.arxivdaily.repository.model.*;
import com.mootann.arxivdaily.util.SpringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class InteractionService {

    private final UserCollectMapper userCollectMapper;
    private final UserLikeMapper userLikeMapper;
    private final PaperCommentMapper paperCommentMapper;
    private final UserMapper userMapper;
    private final UserFollowCategoryMapper userFollowCategoryMapper;

    // 关注分类
    @Transactional
    public void followCategory(String category) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        log.info("User {} follows category {}", userId, category);

        QueryWrapper<UserFollowCategory> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("category", category);
        if (userFollowCategoryMapper.selectCount(query) == 0) {
            UserFollowCategory follow = new UserFollowCategory();
            follow.setUserId(userId);
            follow.setCategory(category);
            int rows = userFollowCategoryMapper.insert(follow);
            log.info("Inserted {} rows for follow category", rows);
        } else {
            log.info("User {} already follows category {}", userId, category);
        }
    }

    // 取消关注分类
    @Transactional
    public void unfollowCategory(String category) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        log.info("User {} unfollows category {}", userId, category);

        QueryWrapper<UserFollowCategory> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("category", category);
        userFollowCategoryMapper.delete(query);
    }

    // 获取用户关注的分类列表（带今日更新数或最新更新数）
    public List<UserFollowCategoryDTO> getUserFollowedCategories() {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        log.info("Fetching followed categories for user {}", userId);
        
        // 1. 获取用户关注的所有分类
        QueryWrapper<UserFollowCategory> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        List<UserFollowCategory> follows = userFollowCategoryMapper.selectList(query);
        
        // 2. 为每个分类获取最新统计信息
        List<UserFollowCategoryDTO> result = new java.util.ArrayList<>();
        
        for (UserFollowCategory follow : follows) {
            UserFollowCategoryDTO dto = new UserFollowCategoryDTO();
            dto.setCategory(follow.getCategory());
            
            // 获取该分类的最新论文日期和数量
            ArxivPaperMapper arxivPaperMapper = SpringUtil.getBean(ArxivPaperMapper.class);
            Map<String, Object> stats = arxivPaperMapper.selectLatestStatsByCategory(follow.getCategory());
            
            if (stats != null) {
                // 处理 date 类型可能是 java.sql.Date 或 java.time.LocalDate 的情况
                Object dateObj = stats.get("date");
                LocalDate latestDate = null;
                if (dateObj instanceof Date) {
                    latestDate = ((Date) dateObj).toLocalDate();
                } else if (dateObj instanceof LocalDate) {
                    latestDate = (LocalDate) dateObj;
                }
                
                dto.setLatestPaperDate(latestDate);
                dto.setPaperCount(((Number) stats.get("count")).longValue());
                
                // 兼容旧字段: 如果最新日期是今天，则 todayPaperCount = paperCount，否则为 0
                if (LocalDate.now().equals(latestDate)) {
                    dto.setTodayPaperCount(dto.getPaperCount());
                } else {
                    dto.setTodayPaperCount(0L);
                }
            } else {
                dto.setPaperCount(0L);
                dto.setTodayPaperCount(0L);
            }
            
            result.add(dto);
        }
        
        log.info("Found {} followed categories for user {}", result.size(), userId);
        return result;
    }

    // 收藏论文
    @Transactional
    public void collectPaper(String arxivId) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        QueryWrapper<UserCollect> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("arxiv_id", arxivId);
        if (userCollectMapper.selectCount(query) == 0) {
            UserCollect collect = new UserCollect();
            collect.setUserId(userId);
            collect.setArxivId(arxivId);
            userCollectMapper.insert(collect);
        }
    }

    // 取消收藏
    @Transactional
    public void uncollectPaper(String arxivId) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        QueryWrapper<UserCollect> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("arxiv_id", arxivId);
        userCollectMapper.delete(query);
    }

    // 点赞论文
    @Transactional
    public void likePaper(String arxivId) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        QueryWrapper<UserLike> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("arxiv_id", arxivId);
        if (userLikeMapper.selectCount(query) == 0) {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setArxivId(arxivId);
            userLikeMapper.insert(like);
        }
    }

    // 取消点赞
    @Transactional
    public void unlikePaper(String arxivId) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        QueryWrapper<UserLike> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("arxiv_id", arxivId);
        userLikeMapper.delete(query);
    }

    // 添加评论
    @Transactional
    public void addComment(String arxivId, String content) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }

        User user = userMapper.selectById(userId);
        String username = (user != null && user.getUsername() != null) ? user.getUsername() : "User " + userId;
        
        PaperComment comment = new PaperComment();
        comment.setArxivId(arxivId);
        comment.setUserId(userId);
        comment.setUsername(username);
        comment.setContent(content);
        paperCommentMapper.insert(comment);
    }

    // 获取评论列表
    public IPage<PaperComment> getComments(String arxivId, int page, int size) {
        Page<PaperComment> p = new Page<>(page, size);
        QueryWrapper<PaperComment> query = new QueryWrapper<>();
        query.eq("arxiv_id", arxivId).orderByDesc("created_time");
        return paperCommentMapper.selectPage(p, query);
    }

    // 获取用户收藏列表
    public IPage<UserCollect> getUserCollects(int page, int size) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        Page<UserCollect> p = new Page<>(page, size);
        QueryWrapper<UserCollect> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("created_time");
        return userCollectMapper.selectPage(p, query);
    }

    // 获取论文的互动数据（点赞数、收藏数、是否点赞、是否收藏）
    public void fillInteractionInfo(ArxivPaper paper) {
        if (paper == null) return;
        
        String arxivId = paper.getArxivId();
        
        // 统计数量
        QueryWrapper<UserLike> likeQuery = new QueryWrapper<>();
        likeQuery.eq("arxiv_id", arxivId);
        paper.setLikeCount(userLikeMapper.selectCount(likeQuery));
        
        QueryWrapper<UserCollect> collectQuery = new QueryWrapper<>();
        collectQuery.eq("arxiv_id", arxivId);
        paper.setCollectCount(userCollectMapper.selectCount(collectQuery));

        QueryWrapper<PaperComment> commentQuery = new QueryWrapper<>();
        commentQuery.eq("arxiv_id", arxivId);
        paper.setCommentCount(paperCommentMapper.selectCount(commentQuery));

        // 浏览量暂未实现，设为0
        paper.setViewCount(0L);

        // 用户状态
        Long userId = SpringUtil.getCurrentUserId();
        if (userId != null) {
            QueryWrapper<UserLike> userLikeQuery = new QueryWrapper<>();
            userLikeQuery.eq("arxiv_id", arxivId).eq("user_id", userId);
            paper.setIsLiked(userLikeMapper.selectCount(userLikeQuery) > 0);

            QueryWrapper<UserCollect> userCollectQuery = new QueryWrapper<>();
            userCollectQuery.eq("arxiv_id", arxivId).eq("user_id", userId);
            paper.setIsCollected(userCollectMapper.selectCount(userCollectQuery) > 0);
        } else {
            paper.setIsLiked(false);
            paper.setIsCollected(false);
        }
    }
}
