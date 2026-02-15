package com.mootann.arxivdaily.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.DAYS;

public class QueryUtil {

    public static String getQuery(List<String> categories, String startDate, String endDate) {
        String query;
        
        // 判断日期是否为空
        boolean hasDateRange = startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty();
        
        // 构建日期查询条件
        String dateQuery = "";
        if (hasDateRange) {
            String formattedStartDate = formatDateForArxiv(startDate);
            String formattedEndDate = formatDateForArxiv(endDate);
            dateQuery = String.format("submittedDate:[%s TO %s]", formattedStartDate, formattedEndDate);
        }
        
        // 判断分类是否为空
        boolean hasCategories = categories != null && !categories.isEmpty();
        
        if (hasCategories && hasDateRange) {
            // 分类和日期范围都有
            String categoryQuery = String.join(" OR ", categories.stream()
                    .map(cat -> "cat:" + cat)
                    .toList());
            query = String.format("(%s) AND %s", categoryQuery, dateQuery);
        } else if (hasCategories) {
            // 只有分类
            String categoryQuery = String.join(" OR ", categories.stream()
                    .map(cat -> "cat:" + cat)
                    .toList());
            query = categoryQuery;
        } else if (hasDateRange) {
            // 只有日期范围
            query = dateQuery;
        } else {
            // 都没有，返回空查询（查询所有）
            query = "all:*";
        }
        
        return query;
    }

    /**
     * 将日期格式化为arXiv API要求的格式
     * @param date 日期字符串，格式：YYYY-MM-DD
     * @return 格式化后的日期字符串，格式：YYYYMMDD
     */
    public static String formatDateForArxiv(String date) {
        if (date == null || date.isEmpty()) {
            return date;
        }
        // 如果日期已经是YYYYMMDD格式，直接返回
        if (date.matches("\\d{8}")) {
            return date;
        }
        // 将YYYY-MM-DD格式转换为YYYYMMDD格式
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        return localDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    // 计算两个日期之间的天数
    public static Integer calculateDaysBetweenDates(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        return (int) DAYS.between(start, end) + 1;
    }

    /**
     * 过滤查询语句，只查询cs和eess主类下的子类
     * @param originalQuery 原始查询语句
     * @return 过滤后的查询语句
     */
    public static String filterToCs(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return "(cat:cs.*)";
        }

        // 如果查询语句已经包含分类过滤，检查是否只包含cs
        if (originalQuery.contains("cat:")) {
            // 提取所有分类
            Pattern pattern = Pattern.compile("cat:([a-zA-Z]+\\.[a-zA-Z]+)");
            Matcher matcher = pattern.matcher(originalQuery);

            Set<String> validCategories = new HashSet<>();
            Set<String> allCategories = new HashSet<>();

            while (matcher.find()) {
                String category = matcher.group(1);
                allCategories.add(category);
                if (category.startsWith("cs.")) {
                    validCategories.add(category);
                }
            }

            // 如果查询中只包含cs的分类，则保持原样
            if (allCategories.equals(validCategories) && !validCategories.isEmpty()) {
                return originalQuery;
            }

            // 如果查询中包含其他分类，需要过滤
            if (!validCategories.isEmpty()) {
                // 构建只包含有效分类的查询
                String filteredQuery = originalQuery;
                for (String cat : allCategories) {
                    if (!validCategories.contains(cat)) {
                        filteredQuery = filteredQuery.replace("cat:" + cat, "");
                    }
                }
                // 清理多余的AND和OR
                filteredQuery = filteredQuery.replaceAll("\\s+(AND|OR)\\s+(AND|OR)\\s+", " $1 ");
                filteredQuery = filteredQuery.replaceAll("^\\s*(AND|OR)\\s+", "");
                filteredQuery = filteredQuery.replaceAll("\\s*(AND|OR)\\s*$", "");
                return filteredQuery.trim();
            }

            // 如果查询中没有有效的分类，添加cs分类的过滤
            return "(" + originalQuery + ") AND (cat:cs.*)";
        }

        // 如果查询中没有分类过滤，添加cs分类的过滤
        return "(" + originalQuery + ") AND (cat:cs.*)";
    }
}
