package com.jpablodrexler.photomanager.domain.enums;

import lombok.Getter;

@Getter
public enum SocialMediaFormat {
    INSTAGRAM_POST(1080, 1080, false),
    INSTAGRAM_PORTRAIT(1080, 1350, false),
    INSTAGRAM_LANDSCAPE(1080, 566, false),
    INSTAGRAM_STORY(1080, 1920, false),
    INSTAGRAM_PROFILE(110, 110, true),
    FACEBOOK_POST(1200, 630, false),
    FACEBOOK_PROFILE(170, 170, true),
    LINKEDIN_POST(1200, 627, false),
    LINKEDIN_PROFILE(400, 400, true),
    TWITTER_POST(1600, 900, false),
    TWITTER_PROFILE(400, 400, true),
    TWITTER_HEADER(1500, 500, false);

    private final int targetWidth;
    private final int targetHeight;
    private final boolean isCircle;

    SocialMediaFormat(int targetWidth, int targetHeight, boolean isCircle) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.isCircle = isCircle;
    }
}
