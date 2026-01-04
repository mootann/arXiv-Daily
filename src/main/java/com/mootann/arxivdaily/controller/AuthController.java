package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.annotation.RequireDataPermission;
import com.mootann.arxivdaily.annotation.RequirePermission;
import com.mootann.arxivdaily.dto.ApiResponse;
import com.mootann.arxivdaily.dto.user.LoginRequest;
import com.mootann.arxivdaily.dto.user.LoginResponse;
import com.mootann.arxivdaily.dto.user.RegisterRequest;
import com.mootann.arxivdaily.model.User;
import com.mootann.arxivdaily.service.AuthService;
import com.mootann.arxivdaily.util.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        Long currentUserId = SpringUtil.getCurrentUserId();
        User user = authService.getCurrentUser(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/organizations/join")
    public ResponseEntity<ApiResponse<User>> joinOrganization(@RequestParam String tagId) {
        Long currentUserId = SpringUtil.getCurrentUserId();
        User user = authService.joinOrganization(currentUserId, tagId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/organizations/leave")
    public ResponseEntity<ApiResponse<User>> leaveOrganization(@RequestParam String tagId) {
        Long currentUserId = SpringUtil.getCurrentUserId();
        User user = authService.leaveOrganization(currentUserId, tagId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping("/organizations/primary")
    public ResponseEntity<ApiResponse<User>> setPrimaryOrganization(@RequestParam String tagId) {
        Long currentUserId = SpringUtil.getCurrentUserId();
        User user = authService.setPrimaryOrganization(currentUserId, tagId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/admin/users/{userId}")
    @RequirePermission(value = "user:read", roles = {"ADMIN"})
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        User user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/organizations/users/{userId}")
    @RequireDataPermission(type = RequireDataPermission.DataPermissionType.ORG, orgTagParam = "orgTag")
    public ResponseEntity<ApiResponse<User>> getUserInOrganization(@PathVariable Long userId, @RequestParam String orgTag) {
        User user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
