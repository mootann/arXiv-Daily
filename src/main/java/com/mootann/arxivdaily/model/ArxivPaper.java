package com.mootann.arxivdaily.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "arxiv_papers",
        indexes = {
                @Index(name = "idx_arxiv_id", columnList = "arxiv_id", unique = true),
                @Index(name = "idx_published_date", columnList = "published_date"),
                @Index(name = "idx_primary_category", columnList = "primary_category"),
                @Index(name = "idx_created_time", columnList = "created_time"),
                @Index(name = "idx_github_url", columnList = "github_url")
        })
@NoArgsConstructor
@AllArgsConstructor
public class ArxivPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "arxiv_id", nullable = false, length = 50)
    private String arxivId;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // 使用 JSONB 类型存储 List<String>
    @Type(JsonBinaryType.class)
    @Column(name = "authors", columnDefinition = "jsonb")
    private List<String> authors;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "updated_date")
    private LocalDate updatedDate;

    @Column(name = "primary_category", length = 50)
    private String primaryCategory;

    // 使用 JSONB 类型存储 List<String>
    @Type(JsonBinaryType.class)
    @Column(name = "categories", columnDefinition = "jsonb")
    private List<String> categories;

    @Column(name = "pdf_url", columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(name = "latex_url", columnDefinition = "TEXT")
    private String latexUrl;

    @Column(name = "arxiv_url", columnDefinition = "TEXT")
    private String arxivUrl;

    @Column(name = "doi", length = 255)
    private String doi;

    @Column(name = "version")
    private Integer version;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
}