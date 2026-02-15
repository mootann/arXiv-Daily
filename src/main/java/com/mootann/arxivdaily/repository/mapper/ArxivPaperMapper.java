package com.mootann.arxivdaily.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mootann.arxivdaily.repository.model.ArxivPaper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * arXiv论文数据访问接口
 * 使用 MyBatis Plus 的 BaseMapper
 */
@Mapper
public interface ArxivPaperMapper extends BaseMapper<ArxivPaper> {

    /**
     * 根据arXiv ID查询
     * @param arxivId arXiv ID
     * @return 论文信息
     */
    default ArxivPaper findByArxivId(String arxivId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("arxiv_id", arxivId));
    }

    /**
     * 检查arXiv ID是否存在
     * @param arxivId arXiv ID
     * @return 是否存在
     */
    default boolean existsByArxivId(String arxivId) {
        return selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("arxiv_id", arxivId)) > 0;
    }

    /**
     * 根据主要分类查询
     * @param primaryCategory 主要分类
     * @return 论文列表
     */
    default List<ArxivPaper> findByPrimaryCategory(String primaryCategory) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory));
    }

    /**
     * 根据主要分类分页查询
     * @param primaryCategory 主要分类
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategory(String primaryCategory, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .orderByDesc("published_date"));
    }

    /**
     * 查询主要分类为空的论文（分页）
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNull(int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .orderByDesc("published_date"));
    }

    /**
     * 根据发布日期范围查询
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 论文列表
     */
    default List<ArxivPaper> findByPublishedDateBetween(LocalDate startDate, LocalDate endDate) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .between("published_date", startDate, endDate));
    }

    /**
     * 根据发布日期范围分页查询
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPublishedDateBetween(LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .between("published_date", startDate, endDate)
                .orderByDesc("published_date"));
    }

    /**
     * 根据分类和日期范围查询
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 论文列表
     */
    default List<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetween(
            String primaryCategory, LocalDate startDate, LocalDate endDate) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .between("published_date", startDate, endDate));
    }

    /**
     * 根据分类和日期范围分页查询
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetween(
            String primaryCategory, LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .between("published_date", startDate, endDate)
                .orderByDesc("published_date"));
    }

    /**
     * 根据标题模糊查询
     * @param keyword 关键词
     * @return 论文列表
     */
    @Select("SELECT * FROM arxiv_papers WHERE title LIKE CONCAT('%', #{keyword}, '%')")
    List<ArxivPaper> searchByTitle(@Param("keyword") String keyword);

    /**
     * 根据标题或摘要模糊查询
     * @param keyword 关键词
     * @return 论文列表
     */
    @Select("SELECT * FROM arxiv_papers WHERE title LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%')")
    List<ArxivPaper> searchByTitleOrSummary(@Param("keyword") String keyword);

    /**
     * 根据标题或摘要模糊查询分页
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> searchByTitleOrSummary(String keyword, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .like("title", keyword)
                .or()
                .like("summary", keyword)
                .orderByDesc("published_date"));
    }

    /**
     * 查询指定日期之后发布的论文
     * @param date 日期
     * @return 论文列表
     */
    default List<ArxivPaper> findByPublishedDateAfter(LocalDate date) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .gt("published_date", date));
    }

    /**
     * 批量查询指定arXiv ID列表
     * @param arxivIds arXiv ID列表
     * @return 论文列表
     */
    default List<ArxivPaper> findByArxivIds(List<String> arxivIds) {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .in("arxiv_id", arxivIds));
    }

    /**
     * 查询有GitHub URL的论文
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByGithubUrlIsNotNull(int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询没有GitHub URL的论文
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByGithubUrlIsNull(int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询有GitHub URL的论文
     * @return 论文列表
     */
    default List<ArxivPaper> findByGithubUrlIsNotNull() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNotNull("github_url"));
    }

    /**
     * 查询没有GitHub URL的论文
     * @return 论文列表
     */
    default List<ArxivPaper> findByGithubUrlIsNull() {
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("github_url"));
    }

    /**
     * 根据主要分类查询有GitHub URL的论文（分页）
     * @param primaryCategory 主要分类
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryAndGithubUrlIsNotNull(String primaryCategory, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据主要分类查询没有GitHub URL的论文（分页）
     * @param primaryCategory 主要分类
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryAndGithubUrlIsNull(String primaryCategory, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询主要分类为空且有GitHub URL的论文（分页）
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNullAndGithubUrlIsNotNull(int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询主要分类为空且没有GitHub URL的论文（分页）
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNullAndGithubUrlIsNull(int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据日期范围查询有GitHub URL的论文（分页）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPublishedDateBetweenAndGithubUrlIsNotNull(LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .between("published_date", startDate, endDate)
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据日期范围查询没有GitHub URL的论文（分页）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPublishedDateBetweenAndGithubUrlIsNull(LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .between("published_date", startDate, endDate)
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据标题或摘要模糊查询有GitHub URL的论文（分页）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> searchByTitleOrSummaryAndGithubUrlIsNotNull(String keyword, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .and(w -> w.like("title", keyword).or().like("summary", keyword))
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据标题或摘要模糊查询没有GitHub URL的论文（分页）
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> searchByTitleOrSummaryAndGithubUrlIsNull(String keyword, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .and(w -> w.like("title", keyword).or().like("summary", keyword))
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据分类和日期范围分页查询
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetween(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .between("published_date", startDate, endDate)
                .orderByDesc("published_date"));
    }

    /**
     * 根据分类和日期范围查询有GitHub URL的论文（分页）
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNotNull(
            String primaryCategory, LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .between("published_date", startDate, endDate)
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 根据分类和日期范围查询没有GitHub URL的论文（分页）
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNull(
            String primaryCategory, LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .eq("primary_category", primaryCategory)
                .between("published_date", startDate, endDate)
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询主要分类为空且在指定日期范围内且有GitHub URL的论文（分页）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNotNull(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .between("published_date", startDate, endDate)
                .isNotNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询主要分类为空且在指定日期范围内且没有GitHub URL的论文（分页）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param page 页码
     * @param size 每页数量
     * @return 论文分页结果
     */
    default IPage<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNull(
            LocalDate startDate, LocalDate endDate, int page, int size) {
        Page<ArxivPaper> pageObj = new Page<>(page, size);
        return selectPage(pageObj, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArxivPaper>()
                .isNull("primary_category")
                .between("published_date", startDate, endDate)
                .isNull("github_url")
                .orderByDesc("published_date"));
    }

    /**
     * 查询数据库中最新的论文发布日期
     * @return 最新的发布日期
     */
    @Select("SELECT MAX(published_date) FROM arxiv_papers")
    LocalDate findLatestPublishedDate();

    /**
     * 获取指定分类的最新发布日期和该日期的论文数量
     * @param category 分类名称
     * @return 包含 date 和 count 的 Map
     */
    @Select("SELECT published_date as date, COUNT(*) as count " +
            "FROM arxiv_papers " +
            "WHERE primary_category = #{category} OR categories LIKE CONCAT('%', #{category}, '%') " +
            "GROUP BY published_date " +
            "ORDER BY published_date DESC " +
            "LIMIT 1")
    java.util.Map<String, Object> selectLatestStatsByCategory(@Param("category") String category);
}
