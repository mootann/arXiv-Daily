package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.client.MinIOClient;
import com.mootann.arxivdaily.repository.KnowledgeDocumentRepository;
import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.model.KnowledgeDocument;
import com.mootann.arxivdaily.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private MinIOClient minIOClient;

    @Autowired
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    private static final String BUCKET_NAME = "knowledge-base";

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> upload(@RequestParam("file") MultipartFile file,
                                                                 @RequestParam(value = "isPublic", defaultValue = "false") Boolean isPublic) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录"));
        }

        try {
            if (!minIOClient.bucketExists(BUCKET_NAME)) {
                minIOClient.createBucket(BUCKET_NAME);
            }

            String originalFilename = file.getOriginalFilename();
            String objectName = userId + "/" + UUID.randomUUID() + "_" + originalFilename;

            boolean success = minIOClient.putObject(BUCKET_NAME, objectName, file);
            if (!success) {
                return ResponseEntity.status(500).body(ApiResponse.error(500, "文件上传失败"));
            }

            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setFileName(originalFilename);
            doc.setObjectName(objectName);
            doc.setFileType(file.getContentType());
            doc.setFileSize(file.getSize());
            doc.setUserId(userId);
            doc.setIsPublic(isPublic);

            KnowledgeDocument savedDoc = knowledgeDocumentRepository.save(doc);
            return ResponseEntity.ok(ApiResponse.success(savedDoc));

        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.status(500).body(ApiResponse.error(500, "文件上传出错: " + e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<KnowledgeDocument>>> list() {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录"));
        }
        List<KnowledgeDocument> list = knowledgeDocumentRepository.findByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = SpringUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未登录"));
        }

        KnowledgeDocument doc = knowledgeDocumentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "文件不存在"));
        }

        if (!doc.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "无权删除"));
        }

        // 删除 MinIO 上的文件 (MinIOClient 需要添加 deleteObject 方法，这里暂时假设可以忽略或后续添加)
        // minIOClient.removeObject(BUCKET_NAME, doc.getObjectName()); 

        knowledgeDocumentRepository.delete(doc);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
