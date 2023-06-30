package com.alter.obsyncshare.utils;

import java.util.HashMap;
import java.util.Map;

public class UsernameGeneratorUtil {
    public static String generateUsername(String userAgent) {
        // 将用户代理字符串转换为小写以进行统一处理
        userAgent = userAgent.toLowerCase();

        // 定义浏览器类型的关键字和对应的可读标识
        Map<String, String> browserKeywords = new HashMap<>();
        browserKeywords.put("chrome", "Chrome");
        browserKeywords.put("firefox", "Firefox");
        browserKeywords.put("safari", "Safari");
        browserKeywords.put("opera", "Opera");
        browserKeywords.put("edge", "Edge");
        browserKeywords.put("ie", "Internet Explorer");

        // 定义设备类型的关键字和对应的可读标识
        Map<String, String> deviceKeywords = new HashMap<>();
        deviceKeywords.put("windows", "Windows");
        deviceKeywords.put("macintosh", "Mac");
        deviceKeywords.put("iphone", "iPhone");
        deviceKeywords.put("ipad", "iPad");
        deviceKeywords.put("android", "Android");
        deviceKeywords.put("linux", "Linux");

        // 检查用户代理字符串中是否包含浏览器类型关键字
        String browser = findKeyword(browserKeywords, userAgent);

        // 检查用户代理字符串中是否包含设备类型关键字
        String device = findKeyword(deviceKeywords, userAgent);

        // 将浏览器类型和设备类型组合成可读的用户名
        String username = device + "_" + browser;

        return username;
    }

    private static String findKeyword(Map<String, String> keywords, String userAgent) {
        for (String keyword : keywords.keySet()) {
            if (userAgent.contains(keyword)) {
                return keywords.get(keyword);
            }
        }
        return "Unknown";
    }

    public static void main(String[] args) {
//        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
        String username = generateUsername(userAgent);
        System.out.println(username);
    }
}
