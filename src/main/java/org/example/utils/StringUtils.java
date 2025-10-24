package org.example.utils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 */
public class StringUtils {

    /**
     * 构建完整的URL路径
     * @param baseUrls controller-urls
     * @param methodUrls method-urls
     * @return full-urls
     */
    public static List<String> buildFullUrls(List<String> baseUrls, List<String> methodUrls) {
        if (methodUrls.isEmpty()) {
            return baseUrls.isEmpty() ? List.of("/") : baseUrls;
        }

        if (baseUrls.isEmpty()) {
            return methodUrls;
        }

        return baseUrls.stream()
            .flatMap(base -> methodUrls.stream()
                .map(method -> normalizePath(base + method)))
            .collect(Collectors.toList());
    }

    private static String normalizePath(String path) {
        return path.replaceAll("/+", "/");
    }

    /**
     * 计算字符串的可见宽度(考虑 Emoji、CJK 字符等宽字符)
     *
     * @param str 要计算宽度的字符串
     * @return 字符串在终端中的实际显示宽度
     */
    public static int calculateVisibleWidth(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < str.length(); ) {
            int codePoint = str.codePointAt(i);
            width += getCharWidth(codePoint);
            i += Character.charCount(codePoint);
        }
        return width;
    }

    /**
     * 获取单个字符的显示宽度
     *
     * @param codePoint Unicode 码点
     * @return 字符显示宽度 (0/1/2)
     */
    public static int getCharWidth(int codePoint) {
        // 控制字符和零宽字符
        if (isZeroWidth(codePoint)) {
            return 0;
        }

        // 宽字符范围 (CJK、全角、Emoji 等)
        if (isWideChar(codePoint)) {
            return 2;
        }

        return 1;
    }

    /**
     * 判断字符是否为零宽字符
     */
    private static boolean isZeroWidth(int codePoint) {
        // 控制字符
        if (Character.isISOControl(codePoint)) {
            return true;
        }

        // 零宽字符
        if (codePoint == 0x00AD || // SOFT HYPHEN
            (codePoint >= 0x200B && codePoint <= 0x200F) || // 零宽空格、连接符等
            (codePoint >= 0x202A && codePoint <= 0x202E)) { // 文本方向控制
            return true;
        }

        // 组合字符标记
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK ||
            type == Character.ENCLOSING_MARK ||
            type == Character.COMBINING_SPACING_MARK;
    }

    /**
     * 判断字符是否为双宽字符
     */
    private static boolean isWideChar(int codePoint) {
        return (codePoint >= 0x1100 && codePoint <= 0x115F) ||  // Hangul Jamo
            (codePoint >= 0x2329 && codePoint <= 0x232A) ||  // 左右尖括号
            (codePoint >= 0x2E80 && codePoint <= 0x303E) ||  // CJK 部首补充
            (codePoint >= 0x3040 && codePoint <= 0xA4CF) ||  // CJK 统一表意文字
            (codePoint >= 0xAC00 && codePoint <= 0xD7A3) ||  // Hangul Syllables
            (codePoint >= 0xF900 && codePoint <= 0xFAFF) ||  // CJK 兼容表意文字
            (codePoint >= 0xFE10 && codePoint <= 0xFE19) ||  // 竖排标点
            (codePoint >= 0xFE30 && codePoint <= 0xFE6F) ||  // CJK 兼容形式
            (codePoint >= 0xFF00 && codePoint <= 0xFF60) ||  // 全角 ASCII
            (codePoint >= 0xFFE0 && codePoint <= 0xFFE6) ||  // 全角符号
            (codePoint >= 0x1F000 && codePoint <= 0x1F9FF) || // Emoji
            (codePoint >= 0x20000 && codePoint <= 0x2FFFF) || // CJK 扩展 B-E
            (codePoint >= 0x30000 && codePoint <= 0x3FFFF);   // CJK 扩展 F-G
    }

}
