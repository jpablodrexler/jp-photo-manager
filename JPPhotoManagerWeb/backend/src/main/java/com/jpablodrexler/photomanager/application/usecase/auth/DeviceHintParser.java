package com.jpablodrexler.photomanager.application.usecase.auth;

/**
 * Parses a {@code User-Agent} header into a short, human-readable device hint (e.g.
 * {@code "Chrome on macOS"}, {@code "Mobile Safari"}). A simple keyword heuristic is used
 * rather than full UAP parsing, matching the level of detail users need to tell their own
 * sessions apart — see {@code openspec/changes/session-management/design.md}.
 */
final class DeviceHintParser {

    private DeviceHintParser() {
    }

    static String parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }

        boolean mobile = userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone");
        String browser = detectBrowser(userAgent);

        if (mobile) {
            return "Mobile " + browser;
        }

        String os = detectOs(userAgent);
        return os != null ? browser + " on " + os : browser;
    }

    private static String detectBrowser(String userAgent) {
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }
        if (userAgent.contains("OPR/") || userAgent.contains("Opera")) {
            return "Opera";
        }
        if (userAgent.contains("Firefox")) {
            return "Firefox";
        }
        if (userAgent.contains("Chrome")) {
            return "Chrome";
        }
        if (userAgent.contains("Safari")) {
            return "Safari";
        }
        return "Unknown browser";
    }

    private static String detectOs(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "Windows";
        }
        if (userAgent.contains("Mac OS") || userAgent.contains("Macintosh")) {
            return "macOS";
        }
        if (userAgent.contains("Android")) {
            return "Android";
        }
        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iOS")) {
            return "iOS";
        }
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        return null;
    }
}
