package com.jpablodrexler.photomanager.application.usecase.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceHintParserTest {

    @Test
    void parse_nullUserAgent_returnsUnknownDevice() {
        assertThat(DeviceHintParser.parse(null)).isEqualTo("Unknown device");
    }

    @Test
    void parse_blankUserAgent_returnsUnknownDevice() {
        assertThat(DeviceHintParser.parse("  ")).isEqualTo("Unknown device");
    }

    @Test
    void parse_desktopChromeOnWindows_returnsChromeOnWindows() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Chrome on Windows");
    }

    @Test
    void parse_desktopFirefoxOnMac_returnsFirefoxOnMacOs() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:120.0) Gecko/20100101 Firefox/120.0";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Firefox on macOS");
    }

    @Test
    void parse_mobileSafariOnIphone_returnsMobileSafari() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Mobile Safari");
    }

    @Test
    void parse_mobileFirefoxOnAndroid_returnsMobileFirefox() {
        String ua = "Mozilla/5.0 (Android 13; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Mobile Firefox");
    }

    @Test
    void parse_edgeOnWindows_returnsEdgeOnWindows() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Edge on Windows");
    }

    @Test
    void parse_unrecognizedBrowser_returnsUnknownBrowserWithOs() {
        String ua = "curl/8.0 Linux";
        assertThat(DeviceHintParser.parse(ua)).isEqualTo("Unknown browser on Linux");
    }
}
