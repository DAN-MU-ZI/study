package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class HashGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] BASE62_CHARS = BASE62.toCharArray();

    public String encode(long id) {
        if (id == 0) {
            return "0";
        }

        char[] buffer = new char[11];
        int pos = buffer.length;
        long num = id;
        while (num > 0) {
            long q = num / 62;
            int r = (int) (num - q * 62);
            buffer[--pos] = BASE62_CHARS[r];
            num = q;
        }

        return new String(buffer, pos, buffer.length - pos);
    }

    public long decode(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result * 62 + BASE62.indexOf(str.charAt(i));
        }
        return result;
    }
}
