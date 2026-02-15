package com.mootann.arxivdaily.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private static final ThreadLocal<UserInfo> USER_CONTEXT = new ThreadLocal<>();

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static void setUserInfo(UserInfo userInfo) {
        USER_CONTEXT.set(userInfo);
    }

    public static UserInfo getUserInfo() {
        return USER_CONTEXT.get();
    }

    public static Long getCurrentUserId() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getUserId() : null;
    }

    public static String getCurrentUsername() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getUsername() : null;
    }

    public static String getCurrentRole() {
        UserInfo userInfo = USER_CONTEXT.get();
        return userInfo != null ? userInfo.getRole() : null;
    }

    public static void clearUserInfo() {
        USER_CONTEXT.remove();
    }

    public static boolean isAdmin() {
        String role = getCurrentRole();
        return "ADMIN".equals(role);
    }

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private final Long userId;
        private final String username;
        private final String role;
    }
}