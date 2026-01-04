package com.mootann.arxivdaily.util;

import com.mootann.arxivdaily.client.RedisClient;
import com.mootann.arxivdaily.constant.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtRedisCache {

    @Autowired
    private RedisClient redisClient;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public void cacheToken(String username, String token) {
        String key = RedisKeyConstant.JWT_KEY_PREFIX + username;
        redisClient.set(key, token, expiration, TimeUnit.MILLISECONDS);
        log.info("JWT已缓存到Redis: {}", key);
    }

    public String getToken(String username) {
        String key = RedisKeyConstant.JWT_KEY_PREFIX + username;
        Object token = redisClient.get(key);
        return token != null ? token.toString() : null;
    }

    public void removeToken(String username) {
        String key = RedisKeyConstant.JWT_KEY_PREFIX + username;
        redisClient.delete(key);
        log.info("JWT已从Redis移除: {}", key);
    }

    public Boolean validateCachedToken(String username, String token) {
        String cachedToken = getToken(username);
        if (cachedToken == null) {
            return false;
        }
        return cachedToken.equals(token);
    }
}