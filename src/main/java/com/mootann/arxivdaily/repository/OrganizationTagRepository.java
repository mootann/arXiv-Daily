package com.mootann.arxivdaily.repository;

import com.mootann.arxivdaily.model.OrganizationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationTagRepository extends JpaRepository<OrganizationTag, String> {

    Optional<OrganizationTag> findByTagId(String tagId);

    boolean existsByTagId(String tagId);

    List<OrganizationTag> findByCreatedBy(Long createdBy);

    List<OrganizationTag> findByParentTag(String parentTag);

    List<OrganizationTag> findByParentTagIsNull();
}
