package com.mootann.arxivdaily.client;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储客户端
 * 统一封装MinIO操作，提供文件上传、下载、删除等功能
 */
@Slf4j
@Component
public class MinIOClient {

    @Autowired
    private com.mootann.arxivdaily.config.MinIOConfig minIOConfig;

    private MinioClient minioClient;

    /**
     * 初始化MinIO客户端
     */
    @PostConstruct
    public void init() {
        try {
            this.minioClient = MinioClient.builder()
                .endpoint(minIOConfig.getEndpoint())
                .credentials(minIOConfig.getAccessKey(), minIOConfig.getSecretKey())
                .build();
            
            log.info("MinIO客户端初始化成功: endpoint={}", minIOConfig.getEndpoint());
            
            // 如果配置了自动创建存储桶，则创建默认存储桶
            if (Boolean.TRUE.equals(minIOConfig.getAutoCreateBucket())) {
                createBucketIfNotExists(minIOConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("MinIO客户端初始化失败", e);
        }
    }

    /**
     * 检查存储桶是否存在
     * @param bucketName 存储桶名称
     * @return 是否存在
     */
    public boolean bucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
        } catch (Exception e) {
            log.error("检查存储桶存在失败: bucketName={}", bucketName, e);
            return false;
        }
    }

    /**
     * 创建存储桶
     * @param bucketName 存储桶名称
     * @return 是否创建成功
     */
    public boolean createBucket(String bucketName) {
        try {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            log.info("创建存储桶成功: bucketName={}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("创建存储桶失败: bucketName={}", bucketName, e);
            return false;
        }
    }

    /**
     * 如果存储桶不存在则创建
     * @param bucketName 存储桶名称
     * @return 是否创建成功或已存在
     */
    public boolean createBucketIfNotExists(String bucketName) {
        if (!bucketExists(bucketName)) {
            return createBucket(bucketName);
        }
        return true;
    }

    /**
     * 删除存储桶
     * @param bucketName 存储桶名称
     * @return 是否删除成功
     */
    public boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(
                RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build()
            );
            log.info("删除存储桶成功: bucketName={}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("删除存储桶失败: bucketName={}", bucketName, e);
            return false;
        }
    }

    /**
     * 获取所有存储桶
     * @return 存储桶列表
     */
    public List<Bucket> listBuckets() {
        try {
            return minioClient.listBuckets();
        } catch (Exception e) {
            log.error("获取存储桶列表失败", e);
            return List.of();
        }
    }

    /**
     * 上传文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileSize 文件大小
     * @return 是否上传成功
     */
    public boolean putObject(String bucketName, String objectName, InputStream inputStream, 
                            String contentType, long fileSize) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build()
            );
            log.info("上传文件成功: bucketName={}, objectName={}", bucketName, objectName);
            return true;
        } catch (Exception e) {
            log.error("上传文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return false;
        }
    }

    /**
     * 上传文件（使用MultipartFile）
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param file 文件
     * @return 是否上传成功
     */
    public boolean putObject(String bucketName, String objectName, MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            return putObject(bucketName, objectName, inputStream, file.getContentType(), file.getSize());
        } catch (IOException e) {
            log.error("上传文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return false;
        }
    }

    /**
     * 上传文件（字节数组）
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param data 文件数据
     * @param contentType 文件类型
     * @return 是否上传成功
     */
    public boolean putObject(String bucketName, String objectName, byte[] data, String contentType) {
        try {
            InputStream inputStream = new ByteArrayInputStream(data);
            return putObject(bucketName, objectName, inputStream, contentType, data.length);
        } catch (Exception e) {
            log.error("上传文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return false;
        }
    }

    /**
     * 下载文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件输入流
     */
    public InputStream getObject(String bucketName, String objectName) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("下载文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 下载文件到字节数组
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件字节数组
     */
    public byte[] getObjectBytes(String bucketName, String objectName) {
        try (InputStream inputStream = getObject(bucketName, objectName)) {
            if (inputStream == null) {
                return null;
            }
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("读取文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 删除文件
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 是否删除成功
     */
    public boolean removeObject(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            log.info("删除文件成功: bucketName={}, objectName={}", bucketName, objectName);
            return true;
        } catch (Exception e) {
            log.error("删除文件失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return false;
        }
    }

    /**
     * 批量删除文件
     * @param bucketName 存储桶名称
     * @param objectNames 对象名称列表
     * @return 删除结果
     */
    public Iterable<Result<DeleteError>> removeObjects(String bucketName, List<String> objectNames) {
        try {
            List<DeleteObject> deleteObjects = objectNames.stream()
                .map(DeleteObject::new)
                .toList();
            
            return minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteObjects)
                    .build()
            );
        } catch (Exception e) {
            log.error("批量删除文件失败: bucketName={}", bucketName, e);
            return List.of();
        }
    }

    /**
     * 复制文件
     * @param sourceBucketName 源存储桶名称
     * @param sourceObjectName 源对象名称
     * @param targetBucketName 目标存储桶名称
     * @param targetObjectName 目标对象名称
     * @return 是否复制成功
     */
    public boolean copyObject(String sourceBucketName, String sourceObjectName, 
                            String targetBucketName, String targetObjectName) {
        try {
            minioClient.copyObject(
                CopyObjectArgs.builder()
                    .bucket(targetBucketName)
                    .object(targetObjectName)
                    .source(CopySource.builder()
                        .bucket(sourceBucketName)
                        .object(sourceObjectName)
                        .build())
                    .build()
            );
            log.info("复制文件成功: source={}/{}, target={}/{}", 
                sourceBucketName, sourceObjectName, targetBucketName, targetObjectName);
            return true;
        } catch (Exception e) {
            log.error("复制文件失败: source={}/{}, target={}/{}", 
                sourceBucketName, sourceObjectName, targetBucketName, targetObjectName, e);
            return false;
        }
    }

    /**
     * 获取文件信息
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 文件信息
     */
    public StatObjectResponse statObject(String bucketName, String objectName) {
        try {
            return minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            log.error("获取文件信息失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 列出存储桶中的所有文件
     * @param bucketName 存储桶名称
     * @param prefix 前缀（可选）
     * @return 文件列表
     */
    public List<Item> listObjects(String bucketName, String prefix) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            );
            
            List<Item> items = new java.util.ArrayList<>();
            for (Result<Item> result : results) {
                items.add(result.get());
            }
            return items;
        } catch (Exception e) {
            log.error("列出文件失败: bucketName={}, prefix={}", bucketName, prefix, e);
            return List.of();
        }
    }

    /**
     * 列出存储桶中的所有文件
     * @param bucketName 存储桶名称
     * @return 文件列表
     */
    public List<Item> listObjects(String bucketName) {
        return listObjects(bucketName, null);
    }

    /**
     * 获取文件预签名URL（临时访问URL）
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @param expires 过期时间（秒）
     * @return 预签名URL
     */
    public String getPresignedObjectUrl(String bucketName, String objectName, int expires) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expires, TimeUnit.SECONDS)
                    .build()
            );
        } catch (Exception e) {
            log.error("获取预签名URL失败: bucketName={}, objectName={}", bucketName, objectName, e);
            return null;
        }
    }

    /**
     * 获取文件预签名URL（默认7天过期）
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 预签名URL
     */
    public String getPresignedObjectUrl(String bucketName, String objectName) {
        return getPresignedObjectUrl(bucketName, objectName, 7 * 24 * 60 * 60);
    }

    /**
     * 检查文件是否存在
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（文件路径）
     * @return 是否存在
     */
    public boolean objectExists(String bucketName, String objectName) {
        try {
            StatObjectResponse response = statObject(bucketName, objectName);
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取MinIO客户端实例
     * @return MinioClient
     */
    public MinioClient getMinioClient() {
        return minioClient;
    }

    /**
     * 获取配置的默认存储桶名称
     * @return 存储桶名称
     */
    public String getDefaultBucketName() {
        return minIOConfig.getBucketName();
    }
}