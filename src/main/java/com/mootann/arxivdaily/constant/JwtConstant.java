package com.mootann.arxivdaily.constant;

public final class JwtConstant {

    private JwtConstant() {}

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String HEADER_NAME = "Authorization";

    public static final String CLAIM_USERNAME = "username";

    public static final String CLAIM_USER_ID = "userId";

    public static final String CLAIM_ROLE = "role";

    public static final String CLAIM_ORG_TAGS = "orgTags";

    public static final String CLAIM_PRIMARY_ORG = "primaryOrg";

    public static final long DEFAULT_EXPIRATION = 86400000L;
}
