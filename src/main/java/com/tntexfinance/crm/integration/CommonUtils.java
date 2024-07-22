package com.tntexfinance.crm.integration;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class CommonUtils {
    public static Long NVL(Long l) {
        return NVL(l, 0l);
    }

    public static Double NVL(Double l) {
        return NVL(l, 0.0);
    }

    public static Long NVL(Long l, Long defaultVal) {
        return (l == null ? defaultVal : l);
    }

    public static Integer NVL(Integer t) {
        return NVL(t, 0);
    }

    public static Integer NVL(Integer t, Integer defaultVal) {
        return (t == null ? defaultVal : t);
    }

    public static Double NVL(Double t, Double defaultVal) {
        return (t == null ? defaultVal : t);
    }

    public static String NVL(String l) {
        if (StringUtils.isBlank(l)) {
            return "";
        }

        return l.trim();
    }

    public static String NVL(String l, String defaultVal) {
        return l == null ? defaultVal : l.trim();
    }

    public static String hashMD5(String data, String clientKey) {
        String actualData = CommonUtils.NVL(data) + CommonUtils.NVL(clientKey);
        String md5Hex = DigestUtils.md5Hex(actualData);
        return md5Hex;
    }

    @SneakyThrows
    public static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }
}
