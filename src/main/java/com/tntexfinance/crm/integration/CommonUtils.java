package com.tntexfinance.crm.integration;

import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.json.JSONObject;

import static java.util.stream.Collectors.joining;

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

    @SneakyThrows
    public static String getEncodedStr(JSONObject jsonObject) {
        System.out.println(jsonObject);

        //Thực hiện việc sắp xếp lại
        SortedMap<String, String> map = new TreeMap<>();
        Class<?> classObject = jsonObject.getClass();
        Field[] fields = classObject.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(jsonObject);
            String type = field.getType().getSimpleName();
            if (type.equals("Map")) {
                HashMap<String, Object> hashedFieldValue = (HashMap<String, Object>) fieldValue;

                //Lặp trên khóa và giá trị
                for (Map.Entry<String, Object> entry : hashedFieldValue.entrySet()) {
                    Object value = entry.getValue();
                    String strValue = "";

                    if (value != null) {
                        if (value instanceof Float || value instanceof Double) {
                            strValue = String.format("%.6f", Double.parseDouble(value.toString()));
                        } else {
                            strValue = value.toString();
                        }
                    }

                    map.put(entry.getKey(), strValue);
                }
            }
        }

        String encodedURL = map.keySet().stream()
                .map(key -> key + "=" + CommonUtils.encodeValue(map.get(key)))
                .collect(joining("&", "", ""));

        return encodedURL;
    }
}
