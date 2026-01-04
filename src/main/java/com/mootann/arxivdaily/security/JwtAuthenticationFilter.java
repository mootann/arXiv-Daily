package com.mootann.arxivdaily.security;

import com.mootann.arxivdaily.constant.JwtConstant;
import com.mootann.arxivdaily.util.JwtRedisCache;
import com.mootann.arxivdaily.util.JwtUtil;
import com.mootann.arxivdaily.util.SpringUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtRedisCache jwtRedisCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                String username = jwtUtil.extractUsername(jwt);

                if (jwtRedisCache.validateCachedToken(username, jwt)) {
                    Long userId = jwtUtil.extractUserId(jwt);
                    String role = jwtUtil.extractRole(jwt);
                    java.util.Set<String> orgTags = jwtUtil.extractOrgTags(jwt);
                    String primaryOrg = jwtUtil.extractPrimaryOrg(jwt);

                    SpringUtil.setUserInfo(new SpringUtil.UserInfo(
                            userId,
                            username,
                            role,
                            orgTags,
                            primaryOrg
                    ));

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("用户{}已通过JWT认证", username);
                } else {
                    log.warn("JWT token未在Redis中找到或已失效");
                }
            }
        } catch (Exception e) {
            log.error("JWT认证失败", e);
            SpringUtil.clearUserInfo();
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(JwtConstant.HEADER_NAME);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(JwtConstant.BEARER_PREFIX)) {
            return bearerToken.substring(JwtConstant.BEARER_PREFIX.length());
        }
        return null;
    }

    @Override
    public void destroy() {
        SpringUtil.clearUserInfo();
        super.destroy();
    }
}
