package com.mootann.arxivdaily.aspect;

import com.mootann.arxivdaily.annotation.RequireDataPermission;
import com.mootann.arxivdaily.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
public class DataPermissionAspect {

    @Around("@annotation(requireDataPermission)")
    public Object checkDataPermission(ProceedingJoinPoint joinPoint, RequireDataPermission requireDataPermission) throws Throwable {
        RequireDataPermission.DataPermissionType type = requireDataPermission.type();

        if (SpringUtil.isAdmin()) {
            return joinPoint.proceed();
        }

        if (type == RequireDataPermission.DataPermissionType.OWN) {
            Long currentUserId = SpringUtil.getCurrentUserId();
            Long targetUserId = extractTargetUserId(joinPoint);
            if (targetUserId != null && !targetUserId.equals(currentUserId)) {
                log.warn("用户{}无权访问用户{}的数据", currentUserId, targetUserId);
                throw new RuntimeException("无权访问该数据");
            }
        } else if (type == RequireDataPermission.DataPermissionType.ORG) {
            String orgTagParam = requireDataPermission.orgTagParam();
            String targetOrgTag = extractOrgTagParam(joinPoint, orgTagParam);
            if (targetOrgTag != null && !SpringUtil.hasOrgTag(targetOrgTag)) {
                log.warn("用户{}不属于组织{}", SpringUtil.getCurrentUsername(), targetOrgTag);
                throw new RuntimeException("无权访问该组织的数据");
            }
        }

        return joinPoint.proceed();
    }

    private Long extractTargetUserId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals("userId") && args[i] instanceof Long) {
                return (Long) args[i];
            }
        }
        return null;
    }

    private String extractOrgTagParam(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getName().equals(paramName) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }
}
