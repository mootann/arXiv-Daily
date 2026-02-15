package com.mootann.arxivdaily.service;

import com.mootann.arxivdaily.constant.UserConstant;
import com.mootann.arxivdaily.repository.dto.user.LoginRequest;
import com.mootann.arxivdaily.repository.dto.user.LoginResponse;
import com.mootann.arxivdaily.repository.dto.user.RegisterRequest;
import com.mootann.arxivdaily.repository.model.User;
import com.mootann.arxivdaily.repository.mapper.UserMapper;
import com.mootann.arxivdaily.util.JwtRedisCache;
import com.mootann.arxivdaily.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private final JwtRedisCache jwtRedisCache;

    @Transactional
    public User register(RegisterRequest request) {
        if (userMapper.existsByUsername(request.getUsername())) {
            throw new RuntimeException(UserConstant.USERNAME_EXISTS);
        }
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException(UserConstant.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.USER);

        return userMapper.insert(user) > 0 ? user : null;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException(UserConstant.PASSWORD_ERROR);
        }

        String accessToken = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        
        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                user.getUsername()
        );

        // 缓存accessToken，refreshToken也可以选择缓存以便于撤销
        jwtRedisCache.cacheToken(user.getUsername(), accessToken);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());

        return response;
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token无效或已过期");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        
        // 刷新token时也更新缓存
        jwtRedisCache.cacheToken(user.getUsername(), newAccessToken);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshToken); // 保持原Refresh Token，或者生成新的
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());

        return response;
    }


    public void logout(String token) {
        if (token != null) {
            String username = jwtUtil.extractUsername(token);
            jwtRedisCache.removeToken(username);
        }
    }

    public User getCurrentUser(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));
    }
}
