package com.mootann.arxivdaily.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitHubUrlExtractor {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
        "https://github\\.com/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+/?",
        Pattern.CASE_INSENSITIVE
    );

    public static String extractGitHubUrl(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        Matcher matcher = GITHUB_URL_PATTERN.matcher(text);
        if (matcher.find()) {
            String url = matcher.group();
            return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        }
        return null;
    }
}
