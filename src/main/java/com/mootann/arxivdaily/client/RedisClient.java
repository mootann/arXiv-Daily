package com.mootann.arxivdaily.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis客户端
 * 统一封装Redis操作，支持所有数据类型
 */
@Slf4j
@Component
public class RedisClient {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier("redisObjectMapper")
    private ObjectMapper objectMapper;

    private static final String DEFAULT_PREFIX = "arxiv-daily:";
    /**
     * arXiv论文缓存前缀
     */
    public static final String ARXIV_PAPERS_PREFIX = "arxivPapers:";

    /**
     * arXiv论文列表缓存前缀
     */
    public static final String PAPERS_PREFIX = "papers:";

    /**
     * GitHub仓库信息缓存key
     */
    public static final String GITHUB_REPO_INFO = "githubRepoInfo";

    /**
     * GitHub Stars数量缓存key
     */
    public static final String GITHUB_STARS = "githubStars";

    /**
     * GitHub Forks数量缓存key
     */
    public static final String GITHUB_FORKS = "githubForks";

    // ==================== 缓存过期时间常量 ====================

    /**
     * 一天（小时）
     */
    public static final int ONE_DAY_HOURS = 24;

    /**
     * 一天（分钟）
     */
    public static final int ONE_DAY_MINUTES = 1440;

    /**
     * 三十分钟（分钟）
     */
    public static final int THIRTY_MINUTES = 30;

    private String buildKey(String key) {
        return DEFAULT_PREFIX + key;
    }

    private String[] buildKeys(String... keys) {
        String[] fullKeys = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            fullKeys[i] = buildKey(keys[i]);
        }
        return fullKeys;
    }

    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.error("类型转换失败: type={}", clazz.getName(), e);
            return null;
        }
    }

    private <T> T convertValue(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, typeReference);
        } catch (Exception e) {
            log.error("类型转换失败", e);
            return null;
        }
    }

    public boolean delete(String key) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.delete(fullKey);
            log.debug("删除key: key={}, result={}", key, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("删除key失败: key={}", key, e);
            return false;
        }
    }

    public long delete(Collection<String> keys) {
        try {
            List<String> fullKeys = keys.stream()
                .map(this::buildKey)
                .toList();
            Long result = redisTemplate.delete(fullKeys);
            log.debug("批量删除key: count={}", result);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("批量删除key失败", e);
            return 0;
        }
    }

    public boolean exists(String key) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.hasKey(fullKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("判断key存在失败: key={}", key, e);
            return false;
        }
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.expire(fullKey, timeout, unit);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("设置过期时间失败: key={}", key, e);
            return false;
        }
    }

    public long getExpire(String key, TimeUnit unit) {
        try {
            String fullKey = buildKey(key);
            Long result = redisTemplate.getExpire(fullKey, unit);
            return result != null ? result : -1;
        } catch (Exception e) {
            log.error("获取过期时间失败: key={}", key, e);
            return -1;
        }
    }

    public boolean persist(String key) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.persist(fullKey);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("移除过期时间失败: key={}", key, e);
            return false;
        }
    }

    public Set<String> keys(String pattern) {
        try {
            String fullPattern = buildKey(pattern);
            Set<String> result = redisTemplate.keys(fullPattern);
            if (result != null) {
                return result.stream()
                    .map(k -> k.substring(DEFAULT_PREFIX.length()))
                    .collect(Collectors.toSet());
            }
            return Set.of();
        } catch (Exception e) {
            log.error("模糊匹配获取key失败: pattern={}", pattern, e);
            return Set.of();
        }
    }

    /**
     * 根据模式删除key
     * @param pattern 匹配模式
     */
    public void deleteByPattern(String pattern) {
        try {
            Set<String> matchedKeys = keys(pattern);
            if (!matchedKeys.isEmpty()) {
                redisTemplate.delete(matchedKeys.stream().map(this::buildKey).toList());
                log.info("按模式删除缓存: pattern={}, count={}", pattern, matchedKeys.size());
            }
        } catch (Exception e) {
            log.error("按模式删除缓存失败: pattern={}", pattern, e);
        }
    }

    public void clear() {
        try {
            Set<String> allKeys = keys("*");
            if (!allKeys.isEmpty()) {
                redisTemplate.delete(allKeys.stream().map(this::buildKey).toList());
                log.info("清空所有缓存: count={}", allKeys.size());
            }
        } catch (Exception e) {
            log.error("清空所有缓存失败", e);
        }
    }

    public String type(String key) {
        try {
            String fullKey = buildKey(key);
            org.springframework.data.redis.connection.DataType result = redisTemplate.type(fullKey);
            return result != null ? result.name() : null;
        } catch (Exception e) {
            log.error("获取key类型失败: key={}", key, e);
            return null;
        }
    }

    public void set(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForValue().set(fullKey, value);
            log.debug("设置String: key={}", key);
        } catch (Exception e) {
            log.error("设置String失败: key={}", key, e);
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForValue().set(fullKey, value, timeout, unit);
            log.debug("设置String: key={}, timeout={} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置String失败: key={}", key, e);
        }
    }

    public Object get(String key) {
        try {
            String fullKey = buildKey(key);
            Object value = redisTemplate.opsForValue().get(fullKey);
            log.debug("获取String: key={}, hit={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("获取String失败: key={}", key, e);
            return null;
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        return convertValue(value, clazz);
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        Object value = get(key);
        return convertValue(value, typeReference);
    }

    public Long increment(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().increment(fullKey);
        } catch (Exception e) {
            log.error("递增失败: key={}", key, e);
            return 0L;
        }
    }

    public Long increment(String key, long delta) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().increment(fullKey, delta);
        } catch (Exception e) {
            log.error("递增失败: key={}, delta={}", key, delta, e);
            return 0L;
        }
    }

    public Double increment(String key, double delta) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().increment(fullKey, delta);
        } catch (Exception e) {
            log.error("递增失败: key={}, delta={}", key, delta, e);
            return 0.0;
        }
    }

    public Long decrement(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().decrement(fullKey);
        } catch (Exception e) {
            log.error("递减失败: key={}", key, e);
            return 0L;
        }
    }

    public Long decrement(String key, long delta) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().decrement(fullKey, delta);
        } catch (Exception e) {
            log.error("递减失败: key={}, delta={}", key, delta, e);
            return 0L;
        }
    }

    public Long append(String key, String value) {
        try {
            String fullKey = buildKey(key);
            Integer result = redisTemplate.opsForValue().append(fullKey, value);
            return result != null ? result.longValue() : 0L;
        } catch (Exception e) {
            log.error("追加字符串失败: key={}", key, e);
            return 0L;
        }
    }

    public String getRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().get(fullKey, start, end);
        } catch (Exception e) {
            log.error("获取子字符串失败: key={}", key, e);
            return null;
        }
    }

    public void hSet(String key, String field, Object value) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForHash().put(fullKey, field, value);
            log.debug("设置Hash: key={}, field={}", key, field);
        } catch (Exception e) {
            log.error("设置Hash失败: key={}, field={}", key, field, e);
        }
    }

    public void hSetAll(String key, Map<String, Object> map) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForHash().putAll(fullKey, map);
            log.debug("批量设置Hash: key={}", key);
        } catch (Exception e) {
            log.error("批量设置Hash失败: key={}", key, e);
        }
    }

    public Object hGet(String key, String field) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().get(fullKey, field);
        } catch (Exception e) {
            log.error("获取Hash失败: key={}, field={}", key, field, e);
            return null;
        }
    }

    public <T> T hGet(String key, String field, Class<T> clazz) {
        Object value = hGet(key, field);
        return convertValue(value, clazz);
    }

    public Map<Object, Object> hGetAll(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().entries(fullKey);
        } catch (Exception e) {
            log.error("获取所有Hash失败: key={}", key, e);
            return Map.of();
        }
    }

    public Long hDelete(String key, Object... fields) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().delete(fullKey, fields);
        } catch (Exception e) {
            log.error("删除Hash字段失败: key={}", key, e);
            return 0L;
        }
    }

    public boolean hExists(String key, String field) {
        try {
            String fullKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(fullKey, field));
        } catch (Exception e) {
            log.error("判断Hash字段存在失败: key={}, field={}", key, field, e);
            return false;
        }
    }

    public Long hIncrement(String key, String field, long delta) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().increment(fullKey, field, delta);
        } catch (Exception e) {
            log.error("Hash字段递增失败: key={}, field={}", key, field, e);
            return 0L;
        }
    }

    public Double hIncrement(String key, String field, double delta) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().increment(fullKey, field, delta);
        } catch (Exception e) {
            log.error("Hash字段递增失败: key={}, field={}", key, field, e);
            return 0.0;
        }
    }

    public Set<Object> hKeys(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().keys(fullKey);
        } catch (Exception e) {
            log.error("获取Hash所有字段失败: key={}", key, e);
            return Set.of();
        }
    }

    public Long hSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().size(fullKey);
        } catch (Exception e) {
            log.error("获取Hash大小失败: key={}", key, e);
            return 0L;
        }
    }

    public List<Object> hValues(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHash().values(fullKey);
        } catch (Exception e) {
            log.error("获取Hash所有值失败: key={}", key, e);
            return List.of();
        }
    }

    public Long lLeftPush(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().leftPush(fullKey, values);
        } catch (Exception e) {
            log.error("List左侧插入失败: key={}", key, e);
            return 0L;
        }
    }

    public Long lRightPush(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().rightPush(fullKey, values);
        } catch (Exception e) {
            log.error("List右侧插入失败: key={}", key, e);
            return 0L;
        }
    }

    public Object lLeftPop(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().leftPop(fullKey);
        } catch (Exception e) {
            log.error("List左侧弹出失败: key={}", key, e);
            return null;
        }
    }

    public Object lRightPop(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().rightPop(fullKey);
        } catch (Exception e) {
            log.error("List右侧弹出失败: key={}", key, e);
            return null;
        }
    }

    public List<Object> lRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().range(fullKey, start, end);
        } catch (Exception e) {
            log.error("获取List范围失败: key={}", key, e);
            return List.of();
        }
    }

    public Long lSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().size(fullKey);
        } catch (Exception e) {
            log.error("获取List长度失败: key={}", key, e);
            return 0L;
        }
    }

    public void lSet(String key, long index, Object value) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForList().set(fullKey, index, value);
        } catch (Exception e) {
            log.error("设置List索引失败: key={}, index={}", key, index, e);
        }
    }

    public Long lRemove(String key, long count, Object value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().remove(fullKey, count, value);
        } catch (Exception e) {
            log.error("移除List元素失败: key={}", key, e);
            return 0L;
        }
    }

    public Object lIndex(String key, long index) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForList().index(fullKey, index);
        } catch (Exception e) {
            log.error("获取List索引失败: key={}, index={}", key, index, e);
            return null;
        }
    }

    public void lTrim(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            redisTemplate.opsForList().trim(fullKey, start, end);
        } catch (Exception e) {
            log.error("裁剪List失败: key={}", key, e);
        }
    }

    public Long sAdd(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().add(fullKey, values);
        } catch (Exception e) {
            log.error("添加Set失败: key={}", key, e);
            return 0L;
        }
    }

    public Long sRemove(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().remove(fullKey, values);
        } catch (Exception e) {
            log.error("移除Set元素失败: key={}", key, e);
            return 0L;
        }
    }

    public Object sPop(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().pop(fullKey);
        } catch (Exception e) {
            log.error("随机弹出Set元素失败: key={}", key, e);
            return null;
        }
    }

    public List<Object> sPop(String key, long count) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().pop(fullKey, count);
        } catch (Exception e) {
            log.error("批量弹出Set元素失败: key={}", key, e);
            return List.of();
        }
    }

    public Object sRandomMember(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().randomMember(fullKey);
        } catch (Exception e) {
            log.error("随机获取Set元素失败: key={}", key, e);
            return null;
        }
    }

    public List<Object> sRandomMembers(String key, long count) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().randomMembers(fullKey, count);
        } catch (Exception e) {
            log.error("批量随机获取Set元素失败: key={}", key, e);
            return List.of();
        }
    }

    public Set<Object> sMembers(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().members(fullKey);
        } catch (Exception e) {
            log.error("获取所有Set元素失败: key={}", key, e);
            return Set.of();
        }
    }

    public boolean sIsMember(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(fullKey, value));
        } catch (Exception e) {
            log.error("判断Set成员存在失败: key={}", key, e);
            return false;
        }
    }

    public Long sSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForSet().size(fullKey);
        } catch (Exception e) {
            log.error("获取Set大小失败: key={}", key, e);
            return 0L;
        }
    }

    public Set<Object> sUnion(String key, String otherKey) {
        try {
            String fullKey = buildKey(key);
            String fullOtherKey = buildKey(otherKey);
            return redisTemplate.opsForSet().union(fullKey, fullOtherKey);
        } catch (Exception e) {
            log.error("Set并集失败: key={}, otherKey={}", key, otherKey, e);
            return Set.of();
        }
    }

    public Set<Object> sIntersect(String key, String otherKey) {
        try {
            String fullKey = buildKey(key);
            String fullOtherKey = buildKey(otherKey);
            return redisTemplate.opsForSet().intersect(fullKey, fullOtherKey);
        } catch (Exception e) {
            log.error("Set交集失败: key={}, otherKey={}", key, otherKey, e);
            return Set.of();
        }
    }

    public Set<Object> sDifference(String key, String otherKey) {
        try {
            String fullKey = buildKey(key);
            String fullOtherKey = buildKey(otherKey);
            return redisTemplate.opsForSet().difference(fullKey, fullOtherKey);
        } catch (Exception e) {
            log.error("Set差集失败: key={}, otherKey={}", key, otherKey, e);
            return Set.of();
        }
    }

    public Long zAdd(String key, double score, Object value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().add(fullKey, value, score) ? 1L : 0L;
        } catch (Exception e) {
            log.error("添加ZSet失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zAdd(String key, Set<ZSetOperations.TypedTuple<Object>> tuples) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().add(fullKey, tuples);
        } catch (Exception e) {
            log.error("批量添加ZSet失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zRemove(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().remove(fullKey, values);
        } catch (Exception e) {
            log.error("移除ZSet元素失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().size(fullKey);
        } catch (Exception e) {
            log.error("获取ZSet大小失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zRank(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().rank(fullKey, value);
        } catch (Exception e) {
            log.error("获取ZSet排名失败: key={}", key, e);
            return null;
        }
    }

    public Long zReverseRank(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().reverseRank(fullKey, value);
        } catch (Exception e) {
            log.error("获取ZSet反向排名失败: key={}", key, e);
            return null;
        }
    }

    public Double zScore(String key, Object value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().score(fullKey, value);
        } catch (Exception e) {
            log.error("获取ZSet分数失败: key={}", key, e);
            return null;
        }
    }

    public Long zCount(String key, double min, double max) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().count(fullKey, min, max);
        } catch (Exception e) {
            log.error("统计ZSet分数范围失败: key={}", key, e);
            return 0L;
        }
    }

    public Set<Object> zRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().range(fullKey, start, end);
        } catch (Exception e) {
            log.error("获取ZSet范围失败: key={}", key, e);
            return Set.of();
        }
    }

    public Set<Object> zReverseRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().reverseRange(fullKey, start, end);
        } catch (Exception e) {
            log.error("获取ZSet反向范围失败: key={}", key, e);
            return Set.of();
        }
    }

    public Set<Object> zRangeByScore(String key, double min, double max) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().rangeByScore(fullKey, min, max);
        } catch (Exception e) {
            log.error("按分数获取ZSet范围失败: key={}", key, e);
            return Set.of();
        }
    }

    public Set<Object> zReverseRangeByScore(String key, double min, double max) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().reverseRangeByScore(fullKey, min, max);
        } catch (Exception e) {
            log.error("按分数获取ZSet反向范围失败: key={}", key, e);
            return Set.of();
        }
    }

    public Set<ZSetOperations.TypedTuple<Object>> zRangeByScoreWithScores(String key, double min, double max) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().rangeByScoreWithScores(fullKey, min, max);
        } catch (Exception e) {
            log.error("按分数获取ZSet范围及分数失败: key={}", key, e);
            return Set.of();
        }
    }

    public Long zRemoveRange(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().removeRange(fullKey, start, end);
        } catch (Exception e) {
            log.error("移除ZSet范围失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zRemoveRangeByScore(String key, double min, double max) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForZSet().removeRangeByScore(fullKey, min, max);
        } catch (Exception e) {
            log.error("按分数移除ZSet范围失败: key={}", key, e);
            return 0L;
        }
    }

    public Long zUnionAndStore(String key, String otherKey, String destKey) {
        try {
            String fullKey = buildKey(key);
            String fullOtherKey = buildKey(otherKey);
            String fullDestKey = buildKey(destKey);
            return redisTemplate.opsForZSet().unionAndStore(fullKey, fullOtherKey, fullDestKey);
        } catch (Exception e) {
            log.error("ZSet并集并存储失败: key={}, otherKey={}, destKey={}", key, otherKey, destKey, e);
            return 0L;
        }
    }

    public Long zIntersectAndStore(String key, String otherKey, String destKey) {
        try {
            String fullKey = buildKey(key);
            String fullOtherKey = buildKey(otherKey);
            String fullDestKey = buildKey(destKey);
            return redisTemplate.opsForZSet().intersectAndStore(fullKey, fullOtherKey, fullDestKey);
        } catch (Exception e) {
            log.error("ZSet交集并存储失败: key={}, otherKey={}, destKey={}", key, otherKey, destKey, e);
            return 0L;
        }
    }

    public Long bitSet(String key, long offset, boolean value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().setBit(fullKey, offset, value) ? 1L : 0L;
        } catch (Exception e) {
            log.error("设置BitMap位失败: key={}, offset={}", key, offset, e);
            return 0L;
        }
    }

    public Boolean bitGet(String key, long offset) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForValue().getBit(fullKey, offset);
        } catch (Exception e) {
            log.error("获取BitMap位失败: key={}, offset={}", key, offset, e);
            return false;
        }
    }

    public Long bitCount(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.execute((RedisCallback<Long>) connection -> connection.bitCount(fullKey.getBytes()));
        } catch (Exception e) {
            log.error("统计BitMap位数失败: key={}", key, e);
            return 0L;
        }
    }

    public Long bitCount(String key, long start, long end) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.bitCount(fullKey.getBytes(), start, end));
        } catch (Exception e) {
            log.error("统计BitMap范围位数失败: key={}", key, e);
            return 0L;
        }
    }

    public Long bitOpAnd(String destKey, String... sourceKeys) {
        try {
            String fullDestKey = buildKey(destKey);
            byte[][] fullSourceKeys = Arrays.stream(sourceKeys)
                .map(k -> buildKey(k).getBytes())
                .toArray(byte[][]::new);
            return redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.bitOp(RedisStringCommands.BitOperation.AND, fullDestKey.getBytes(), fullSourceKeys));
        } catch (Exception e) {
            log.error("BitMap位运算失败: destKey={}", destKey, e);
            return 0L;
        }
    }

    public Long bitPos(String key, boolean value) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.bitPos(fullKey.getBytes(), value));
        } catch (Exception e) {
            log.error("查找BitMap位位置失败: key={}, value={}", key, value, e);
            return -1L;
        }
    }

    public Long bitPos(String key, boolean value, long start) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.execute((RedisCallback<Long>) connection -> 
                connection.bitPos(fullKey.getBytes(), value, org.springframework.data.domain.Range.closed(start, -1L)));
        } catch (Exception e) {
            log.error("查找BitMap位位置失败: key={}, value={}, start={}", key, value, start, e);
            return -1L;
        }
    }

    public Long pfAdd(String key, Object... values) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHyperLogLog().add(fullKey, values);
        } catch (Exception e) {
            log.error("添加HyperLogLog失败: key={}", key, e);
            return 0L;
        }
    }

    public Long pfSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForHyperLogLog().size(fullKey);
        } catch (Exception e) {
            log.error("获取HyperLogLog基数失败: key={}", key, e);
            return 0L;
        }
    }

    public Long pfUnion(String destKey, String... sourceKeys) {
        try {
            String fullDestKey = buildKey(destKey);
            String[] fullSourceKeys = buildKeys(sourceKeys);
            return redisTemplate.opsForHyperLogLog().union(fullDestKey, fullSourceKeys);
        } catch (Exception e) {
            log.error("HyperLogLog并集失败: destKey={}", destKey, e);
            return 0L;
        }
    }

    public Long geoAdd(String key, double longitude, double latitude, Object member) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().add(fullKey, new Point(longitude, latitude), member);
        } catch (Exception e) {
            log.error("添加GEO位置失败: key={}, member={}", key, member, e);
            return 0L;
        }
    }

    public Long geoAdd(String key, Map<Object, Point> memberCoordinateMap) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().add(fullKey, memberCoordinateMap);
        } catch (Exception e) {
            log.error("批量添加GEO位置失败: key={}", key, e);
            return 0L;
        }
    }

    public Point geoPos(String key, Object member) {
        try {
            String fullKey = buildKey(key);
            List<Point> positions = redisTemplate.opsForGeo().position(fullKey, member);
            return positions != null && !positions.isEmpty() ? positions.get(0) : null;
        } catch (Exception e) {
            log.error("获取GEO位置失败: key={}, member={}", key, member, e);
            return null;
        }
    }

    public Distance geoDist(String key, Object member1, Object member2) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().distance(fullKey, member1, member2);
        } catch (Exception e) {
            log.error("计算GEO距离失败: key={}, member1={}, member2={}", key, member1, member2, e);
            return null;
        }
    }

    public Distance geoDist(String key, Object member1, Object member2, Metric metric) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().distance(fullKey, member1, member2, metric);
        } catch (Exception e) {
            log.error("计算GEO距离失败: key={}, member1={}, member2={}, metric={}", key, member1, member2, metric, e);
            return null;
        }
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(String key, double longitude, double latitude, double radius) {
        try {
            String fullKey = buildKey(key);
            Circle circle = new Circle(new Point(longitude, latitude), new Distance(radius, RedisGeoCommands.DistanceUnit.METERS));
            return redisTemplate.opsForGeo().radius(fullKey, circle);
        } catch (Exception e) {
            log.error("GEO半径查询失败: key={}", key, e);
            return null;
        }
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(String key, double longitude, double latitude, double radius, Metric metric) {
        try {
            String fullKey = buildKey(key);
            Circle circle = new Circle(new Point(longitude, latitude), new Distance(radius, metric));
            return redisTemplate.opsForGeo().radius(fullKey, circle);
        } catch (Exception e) {
            log.error("GEO半径查询失败: key={}, metric={}", key, metric, e);
            return null;
        }
    }

    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadiusByMember(String key, Object member, double radius) {
        try {
            String fullKey = buildKey(key);
            Distance distance = new Distance(radius, RedisGeoCommands.DistanceUnit.METERS);
            return redisTemplate.opsForGeo().radius(fullKey, member, distance);
        } catch (Exception e) {
            log.error("GEO半径查询失败: key={}, member={}", key, member, e);
            return null;
        }
    }

    public Long geoRemove(String key, Object... members) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().remove(fullKey, members);
        } catch (Exception e) {
            log.error("删除GEO位置失败: key={}", key, e);
            return 0L;
        }
    }

    public List<String> geoHash(String key, Object... members) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForGeo().hash(fullKey, members);
        } catch (Exception e) {
            log.error("获取GEO Hash失败: key={}", key, e);
            return List.of();
        }
    }

    public String streamAdd(String key, Map<Object, Object> map) {
        try {
            String fullKey = buildKey(key);
            RecordId recordId = redisTemplate.opsForStream().add(fullKey, map);
            return recordId != null ? recordId.getValue() : null;
        } catch (Exception e) {
            log.error("添加Stream失败: key={}", key, e);
            return null;
        }
    }

    public String streamAdd(String key, String recordId, Map<Object, Object> map) {
        try {
            String fullKey = buildKey(key);
            RecordId result = redisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(fullKey)
                .ofMap(map)
                .withId(RecordId.of(recordId)));
            return result != null ? result.getValue() : null;
        } catch (Exception e) {
            log.error("添加Stream失败: key={}", key, e);
            return null;
        }
    }

    public List<MapRecord<String, Object, Object>> streamRange(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForStream().range(fullKey, org.springframework.data.domain.Range.unbounded());
        } catch (Exception e) {
            log.error("获取Stream范围失败: key={}", key, e);
            return List.of();
        }
    }

    public List<MapRecord<String, Object, Object>> streamRange(String key, String startId, String endId) {
        try {
            String fullKey = buildKey(key);
            org.springframework.data.domain.Range<String> range = 
                org.springframework.data.domain.Range.closed(startId, endId);
            return redisTemplate.opsForStream().range(fullKey, range);
        } catch (Exception e) {
            log.error("获取Stream范围失败: key={}", key, e);
            return List.of();
        }
    }

    public MapRecord<String, Object, Object> streamGet(String key, String recordId) {
        try {
            String fullKey = buildKey(key);
            org.springframework.data.domain.Range<String> range = 
                org.springframework.data.domain.Range.closed(recordId, recordId);
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(fullKey, range);
            return records.isEmpty() ? null : records.get(0);
        } catch (Exception e) {
            log.error("获取Stream记录失败: key={}, recordId={}", key, recordId, e);
            return null;
        }
    }

    public Long streamDelete(String key, String... recordIds) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForStream().delete(fullKey, recordIds);
        } catch (Exception e) {
            log.error("删除Stream记录失败: key={}", key, e);
            return 0L;
        }
    }

    public Long streamSize(String key) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForStream().size(fullKey);
        } catch (Exception e) {
            log.error("获取Stream大小失败: key={}", key, e);
            return 0L;
        }
    }

    public Long streamTrim(String key, long count) {
        try {
            String fullKey = buildKey(key);
            return redisTemplate.opsForStream().trim(fullKey, count);
        } catch (Exception e) {
            log.error("裁剪Stream失败: key={}, count={}", key, count, e);
            return 0L;
        }
    }

    public <T> T execute(DefaultRedisScript<T> script, List<String> keys, Object... args) {
        try {
            List<String> fullKeys = keys.stream().map(this::buildKey).toList();
            return redisTemplate.execute(script, fullKeys, args);
        } catch (Exception e) {
            log.error("执行Lua脚本失败", e);
            return null;
        }
    }

    public Long ttl(String key) {
        return getExpire(key, TimeUnit.SECONDS);
    }

    public Long pttl(String key) {
        return getExpire(key, TimeUnit.MILLISECONDS);
    }

    public boolean expireAt(String key, long timestamp) {
        try {
            String fullKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.expireAt(fullKey, new Date(timestamp)));
        } catch (Exception e) {
            log.error("设置过期时间戳失败: key={}, timestamp={}", key, timestamp, e);
            return false;
        }
    }

    public boolean move(String key, int dbIndex) {
        try {
            String fullKey = buildKey(key);
            Boolean result = redisTemplate.move(fullKey, dbIndex);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("移动key失败: key={}, dbIndex={}", key, dbIndex, e);
            return false;
        }
    }

    public void rename(String oldKey, String newKey) {
        try {
            String fullOldKey = buildKey(oldKey);
            String fullNewKey = buildKey(newKey);
            redisTemplate.rename(fullOldKey, fullNewKey);
        } catch (Exception e) {
            log.error("重命名key失败: oldKey={}, newKey={}", oldKey, newKey, e);
        }
    }

    public boolean renameNX(String oldKey, String newKey) {
        try {
            String fullOldKey = buildKey(oldKey);
            String fullNewKey = buildKey(newKey);
            return Boolean.TRUE.equals(redisTemplate.renameIfAbsent(fullOldKey, fullNewKey));
        } catch (Exception e) {
            log.error("重命名key失败: oldKey={}, newKey={}", oldKey, newKey, e);
            return false;
        }
    }
}
