package com.mootann.arxivdaily.service;

import com.mootann.arxivdaily.constant.OrganizationConstant;
import com.mootann.arxivdaily.constant.UserConstant;
import com.mootann.arxivdaily.repository.dto.user.LoginRequest;
import com.mootann.arxivdaily.repository.dto.user.LoginResponse;
import com.mootann.arxivdaily.repository.dto.user.RegisterRequest;
import com.mootann.arxivdaily.repository.model.OrganizationTag;
import com.mootann.arxivdaily.repository.model.User;
import com.mootann.arxivdaily.repository.OrganizationTagRepository;
import com.mootann.arxivdaily.repository.UserRepository;
import com.mootann.arxivdaily.util.JwtRedisCache;
import com.mootann.arxivdaily.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationTagRepository organizationTagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtRedisCache jwtRedisCache;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException(UserConstant.USERNAME_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(UserConstant.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.USER);

        User savedUser = userRepository.save(user);

        String defaultOrgTagId = createDefaultOrganization(savedUser.getId());
        savedUser.setPrimaryOrg(defaultOrgTagId);
        savedUser.addOrgTag(defaultOrgTagId);

        return userRepository.save(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException(UserConstant.PASSWORD_ERROR);
        }

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getOrgTagSet(),
                user.getPrimaryOrg()
        );

        jwtRedisCache.cacheToken(user.getUsername(), token);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());
        response.setOrgTags(user.getOrgTagSet());
        response.setPrimaryOrg(user.getPrimaryOrg());

        return response;
    }

    public void logout(String token) {
        if (token != null) {
            String username = jwtUtil.extractUsername(token);
            jwtRedisCache.removeToken(username);
        }
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));
    }

    @Transactional
    public User joinOrganization(Long userId, String tagId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        OrganizationTag orgTag = organizationTagRepository.findByTagId(tagId)
                .orElseThrow(() -> new RuntimeException(OrganizationConstant.ORG_NOT_FOUND));

        user.addOrgTag(tagId);
        return userRepository.save(user);
    }

    @Transactional
    public User leaveOrganization(Long userId, String tagId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        user.removeOrgTag(tagId);
        return userRepository.save(user);
    }

    @Transactional
    public User setPrimaryOrganization(Long userId, String tagId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(UserConstant.USER_NOT_FOUND));

        if (!user.hasOrgTag(tagId)) {
            throw new RuntimeException("用户不属于该组织");
        }

        user.setPrimaryOrg(tagId);
        return userRepository.save(user);
    }

    private String createDefaultOrganization(Long userId) {
        String tagId = generateOrgTagId();
        while (organizationTagRepository.existsByTagId(tagId)) {
            tagId = generateOrgTagId();
        }

        OrganizationTag orgTag = new OrganizationTag();
        orgTag.setTagId(tagId);
        orgTag.setName(OrganizationConstant.DEFAULT_ORG_NAME + userId);
        orgTag.setDescription("默认个人组织");
        orgTag.setCreatedBy(userId);

        organizationTagRepository.save(orgTag);
        return tagId;
    }

    private String generateOrgTagId() {
        return OrganizationConstant.ORG_TAG_PREFIX + java.util.UUID.randomUUID().toString()
                .substring(0, OrganizationConstant.ORG_TAG_ID_LENGTH);
    }
}
