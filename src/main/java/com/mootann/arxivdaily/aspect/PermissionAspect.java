package com.mootann.arxivdaily.aspect;

import com.mootann.arxivdaily.annotation.RequirePermission;
import com.mootann.arxivdaily.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String requiredPermission = requirePermission.value();
        String[] allowedRoles = requirePermission.roles();

        if (SpringUtil.isAdmin()) {
            return joinPoint.proceed();
        }

        String currentRole = SpringUtil.getCurrentRole();

        boolean hasRoleAccess = false;
        for (String role : allowedRoles) {
            if (role.equals(currentRole)) {
                hasRoleAccess = true;
                break;
            }
        }

        if (!hasRoleAccess) {
            log.warn("用户角色{}无权限访问需要角色{}的接口", currentRole, allowedRoles);
            throw new RuntimeException("无权访问");
        }

        return joinPoint.proceed();
    }
}
