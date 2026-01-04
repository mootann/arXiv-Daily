package com.mootann.arxivdaily.converter;

import com.mootann.arxivdaily.dto.arxiv.ArxivPaperDTO;
import com.mootann.arxivdaily.xml.ArxivAuthor;
import com.mootann.arxivdaily.xml.ArxivCategory;
import com.mootann.arxivdaily.xml.ArxivEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ArxivEntry 转 ArxivPaperDTO 映射器
 */
@Component
@Mapper(componentModel = "spring")
public interface ArxivEntryMapper {
    
    String ARXIV_BASE_URL = "https://arxiv.org";
    
    @Mapping(source = "id", target = "arxivId", qualifiedByName = "extractArxivId")
    @Mapping(source = "id", target = "version", qualifiedByName = "extractVersion")
    @Mapping(source = "title", target = "title", qualifiedByName = "cleanText")
    @Mapping(source = "summary", target = "summary", qualifiedByName = "cleanText")
    @Mapping(source = "authors", target = "authors", qualifiedByName = "mapAuthors")
    @Mapping(source = "published", target = "publishedDate", qualifiedByName = "parseDate")
    @Mapping(source = "updated", target = "updatedDate", qualifiedByName = "parseDate")
    @Mapping(source = "primaryCategory", target = "primaryCategory", qualifiedByName = "extractCategoryTerm")
    @Mapping(source = "categories", target = "categories", qualifiedByName = "mapCategories")
    @Mapping(source = "doi", target = "doi")
    ArxivPaperDTO entryToDTO(ArxivEntry entry);
    
    /**
     * 从 ID 中提取 arXiv ID
     */
    @Named("extractArxivId")
    default String extractArxivId(String id) {
        if (id == null || !id.contains("/abs/")) {
            return null;
        }
        String[] parts = id.split("/abs/");
        if (parts.length > 1) {
            String arxivIdWithVersion = parts[1];
            String[] idParts = arxivIdWithVersion.split("v");
            return idParts[0];
        }
        return null;
    }
    
    /**
     * 从 ID 中提取版本号
     */
    @Named("extractVersion")
    default Integer extractVersion(String id) {
        if (id == null || !id.contains("/abs/")) {
            return 1;
        }
        String[] parts = id.split("/abs/");
        if (parts.length > 1) {
            String arxivIdWithVersion = parts[1];
            String[] idParts = arxivIdWithVersion.split("v");
            if (idParts.length > 1) {
                try {
                    return Integer.parseInt(idParts[1]);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        }
        return 1;
    }
    
    /**
     * 清理文本中的多余空白
     */
    @Named("cleanText")
    default String cleanText(String text) {
        if (text == null) return null;
        return text.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * 映射作者列表
     */
    @Named("mapAuthors")
    default List<String> mapAuthors(List<ArxivAuthor> authors) {
        if (authors == null) {
            return null;
        }
        return authors.stream()
            .map(ArxivAuthor::getName)
            .filter(name -> name != null)
            .collect(Collectors.toList());
    }
    
    /**
     * 映射分类列表
     */
    @Named("mapCategories")
    default List<String> mapCategories(List<ArxivCategory> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream()
            .map(ArxivCategory::getTerm)
            .filter(term -> term != null)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 提取分类 term
     */
    @Named("extractCategoryTerm")
    default String extractCategoryTerm(ArxivCategory category) {
        if (category == null) {
            return null;
        }
        return category.getTerm();
    }
    
    /**
     * 解析日期
     */
    @Named("parseDate")
    default LocalDate parseDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            return LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            return null;
        }
    }
}
