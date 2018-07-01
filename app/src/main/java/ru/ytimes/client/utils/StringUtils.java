package ru.ytimes.client.utils;

/**
 * Created by andrey on 26.06.18.
 */

public class StringUtils {

    public static String twoColumn(String s1, String s2, int length) {
        return twoColumn(s1, s2, length, ' ');
    }

    public static String twoColumn(String s1, String s2, int length, char ch) {
        if (s1 == null) {
            s1 = "";
        }
        if (s2 == null) {
            s2 = "";
        }

        length -= s1.length();
        length -= s2.length();

        if (length <= 1) {
            return s1 + ch + s2;
        }
        else {
            return s1 + generate(ch, length) + s2;
        }
    }

    public static String generate(char ch, int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < length; i++) {
            builder.append(ch);
        }

        return builder.toString();
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

}
