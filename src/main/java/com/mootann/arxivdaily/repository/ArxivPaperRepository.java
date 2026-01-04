package com.mootann.arxivdaily.repository;

import com.mootann.arxivdaily.model.ArxivPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * arXiv论文数据访问接口
 */
@Repository
public interface ArxivPaperRepository extends JpaRepository<ArxivPaper, Long> {
    
    /**
     * 根据arXiv ID查询
     * @param arxivId arXiv ID
     * @return 论文信息
     */
    Optional<ArxivPaper> findByArxivId(String arxivId);
    
    /**
     * 检查arXiv ID是否存在
     * @param arxivId arXiv ID
     * @return 是否存在
     */
    boolean existsByArxivId(String arxivId);
    
    /**
     * 根据主要分类查询
     * @param primaryCategory 主要分类
     * @return 论文列表
     */
    List<ArxivPaper> findByPrimaryCategory(String primaryCategory);
    
    /**
     * 根据主要分类分页查询
     * @param primaryCategory 主要分类
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategory(String primaryCategory, Pageable pageable);
    
    /**
     * 查询主要分类为空的论文（分页）
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNull(Pageable pageable);
    
    /**
     * 根据分类查询有GitHub URL的论文
     * @param primaryCategory 主要分类
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryAndGithubUrlIsNotNull(String primaryCategory, Pageable pageable);
    
    /**
     * 查询主要分类为空且有GitHub URL的论文
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNullAndGithubUrlIsNotNull(Pageable pageable);

    /**
     * 根据分类查询没有GitHub URL的论文
     * @param primaryCategory 主要分类
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryAndGithubUrlIsNull(String primaryCategory, Pageable pageable);
    
    /**
     * 查询主要分类为空且没有GitHub URL的论文
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNullAndGithubUrlIsNull(Pageable pageable);

    /**
     * 根据发布日期范围查询
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 论文列表
     */
    List<ArxivPaper> findByPublishedDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据发布日期范围分页查询
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPublishedDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 根据分类和日期范围查询
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 论文列表
     */
    List<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetween(
        String primaryCategory, LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据分类和日期范围分页查询
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetween(
        String primaryCategory, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 查询主要分类为空且在指定日期范围内的论文（分页）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetween(
        LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 根据标题模糊查询
     * @param keyword 关键词
     * @return 论文列表
     */
    @Query("SELECT p FROM ArxivPaper p WHERE p.title ILIKE %:keyword%")
    List<ArxivPaper> searchByTitle(@Param("keyword") String keyword);
    
    /**
     * 根据标题模糊查询(分页)
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    @Query("SELECT p FROM ArxivPaper p WHERE p.title ILIKE %:keyword%")
    Page<ArxivPaper> searchByTitle(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 根据标题或摘要模糊查询
     * @param keyword 关键词
     * @return 论文列表
     */
    @Query("SELECT p FROM ArxivPaper p WHERE p.title ILIKE %:keyword% OR p.summary ILIKE %:keyword%")
    List<ArxivPaper> searchByTitleOrSummary(@Param("keyword") String keyword);
    
    /**
     * 根据标题或摘要模糊查询(分页)
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    @Query("SELECT p FROM ArxivPaper p WHERE p.title ILIKE %:keyword% OR p.summary ILIKE %:keyword%")
    Page<ArxivPaper> searchByTitleOrSummary(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 查询指定日期之后发布的论文
     * @param date 日期
     * @return 论文列表
     */
    List<ArxivPaper> findByPublishedDateAfter(LocalDate date);
    
    /**
     * 统计各分类的论文数量（包括分类为空的记录）
     * @return 分类和数量列表
     */
    @Query("SELECT COALESCE(p.primaryCategory, 'UNCATEGORIZED'), COUNT(p) FROM ArxivPaper p GROUP BY p.primaryCategory")
    List<Object[]> countByCategory();

    /**
     * 根据日期范围统计各分类的论文数量（包括分类为空的记录）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分类和数量列表
     */
    @Query("SELECT COALESCE(p.primaryCategory, 'UNCATEGORIZED'), COUNT(p) FROM ArxivPaper p WHERE p.publishedDate >= :startDate AND p.publishedDate <= :endDate GROUP BY p.primaryCategory")
    List<Object[]> countByCategoryAndPublishedDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 使用 pg_class.reltuples 快速估算论文总数
     * @return 估算的论文总数
     */
    @Query(value = "SELECT reltuples::bigint FROM pg_class WHERE relname = 'arxiv_papers'", nativeQuery = true)
    Long estimateTotalCount();
    
    /**
     * 批量查询指定arXiv ID列表
     * @param arxivIds arXiv ID列表
     * @return 论文列表
     */
    @Query("SELECT p FROM ArxivPaper p WHERE p.arxivId IN :arxivIds")
    List<ArxivPaper> findByArxivIds(@Param("arxivIds") List<String> arxivIds);

    /**
     * 查询有GitHub URL的论文
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByGithubUrlIsNotNull(Pageable pageable);

    /**
     * 查询没有GitHub URL的论文
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByGithubUrlIsNull(Pageable pageable);

    /**
     * 根据分类和日期范围查询有GitHub URL的论文
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNotNull(
        String primaryCategory, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 查询主要分类为空且在指定日期范围内且有GitHub URL的论文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNotNull(
        LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 根据分类和日期范围查询没有GitHub URL的论文
     * @param primaryCategory 主要分类
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryAndPublishedDateBetweenAndGithubUrlIsNull(
        String primaryCategory, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    /**
     * 查询主要分类为空且在指定日期范围内且没有GitHub URL的论文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPrimaryCategoryIsNullAndPublishedDateBetweenAndGithubUrlIsNull(
        LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 根据日期范围查询有GitHub URL的论文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPublishedDateBetweenAndGithubUrlIsNotNull(
        LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 根据日期范围查询没有GitHub URL的论文
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    Page<ArxivPaper> findByPublishedDateBetweenAndGithubUrlIsNull(
        LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * 根据标题或摘要模糊查询有GitHub URL的论文
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    @Query("SELECT p FROM ArxivPaper p WHERE (p.title ILIKE %:keyword% OR p.summary ILIKE %:keyword%) AND p.githubUrl IS NOT NULL")
    Page<ArxivPaper> searchByTitleOrSummaryAndGithubUrlIsNotNull(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据标题或摘要模糊查询没有GitHub URL的论文
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 论文分页结果
     */
    @Query("SELECT p FROM ArxivPaper p WHERE (p.title ILIKE %:keyword% OR p.summary ILIKE %:keyword%) AND p.githubUrl IS NULL")
    Page<ArxivPaper> searchByTitleOrSummaryAndGithubUrlIsNull(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 查询数据库中最新的论文发布日期
     * @return 最新的发布日期
     */
    @Query("SELECT MAX(p.publishedDate) FROM ArxivPaper p")
    LocalDate findLatestPublishedDate();
}