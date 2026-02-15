package com.mootann.arxivdaily.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mootann.arxivdaily.repository.dto.UserFollowCategoryDTO;
import com.mootann.arxivdaily.repository.model.UserFollowCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFollowCategoryMapper extends BaseMapper<UserFollowCategory> {

    @Select("SELECT ufc.category, " +
            "COUNT(CASE WHEN ap.published_date = CURDATE() THEN 1 END) as todayPaperCount, " +
            "MAX(ap.published_date) as latestPaperDate, " +
            "COUNT(ap.id) as paperCount " +
            "FROM user_follow_categories ufc " +
            "LEFT JOIN arxiv_papers ap ON (ap.primary_category = ufc.category OR ap.categories LIKE CONCAT('%', ufc.category, '%')) " +
            "WHERE ufc.user_id = #{userId} " +
            "GROUP BY ufc.category")
    List<UserFollowCategoryDTO> selectFollowedCategoriesWithCount(@Param("userId") Long userId);
}
