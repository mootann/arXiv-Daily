package com.mootann.arxivdaily.repository;

import com.mootann.arxivdaily.repository.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByUserId(Long userId);
    List<KnowledgeDocument> findByIsPublicTrue();
}
