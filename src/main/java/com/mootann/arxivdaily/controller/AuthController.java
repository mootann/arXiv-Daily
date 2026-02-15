package com.mootann.arxivdaily.controller;

import com.mootann.arxivdaily.repository.dto.ApiResponse;
import com.mootann.arxivdaily.repository.dto.user.LoginRequest;
import com.mootann.arxivdaily.repository.dto.user.LoginResponse;
import com.mootann.arxivdaily.repository.dto.user.RegisterRequest;
import com.mootann.arxivdaily.repository.model.User;
import com.mootann.arxivdaily.service.AuthService;
import com.mootann.arxivdaily.util.SpringUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

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

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody java.util.Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Refresh Token不能为空"));
        }
        try {
            LoginResponse response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, e.getMessage()));
        }
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
}
