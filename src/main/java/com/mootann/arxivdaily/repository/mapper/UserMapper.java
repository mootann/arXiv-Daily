package com.mootann.arxivdaily.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mootann.arxivdaily.repository.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 用户数据访问接口
 * 使用 MyBatis Plus 的 BaseMapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询
     * @param username 用户名
     * @return 用户信息
     */
    default Optional<User> findByUsername(String username) {
        User user = selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username));
        return Optional.ofNullable(user);
    }

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    default boolean existsByUsername(String username) {
        return selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username)) > 0;
    }

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 是否存在
     */
    default boolean existsByEmail(String email) {
        return selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("email", email)) > 0;
    }
}
