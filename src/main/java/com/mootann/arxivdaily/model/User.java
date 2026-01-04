package com.mootann.arxivdaily.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    @Column(length = 255)
    private String orgTags;

    @Column(length = 50)
    private String primaryOrg;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    public Set<String> getOrgTagSet() {
        if (orgTags == null || orgTags.isEmpty()) {
            return new HashSet<>();
        }
        return Set.of(orgTags.split(","));
    }

    public void setOrgTagSet(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.orgTags = null;
        } else {
            this.orgTags = String.join(",", tags);
        }
    }

    public void addOrgTag(String tagId) {
        Set<String> tags = getOrgTagSet();
        tags.add(tagId);
        setOrgTagSet(tags);
    }

    public void removeOrgTag(String tagId) {
        Set<String> tags = getOrgTagSet();
        tags.remove(tagId);
        setOrgTagSet(tags);
    }

    public boolean hasOrgTag(String tagId) {
        return getOrgTagSet().contains(tagId);
    }

    public enum UserRole {
        ADMIN,
        USER
    }
}
