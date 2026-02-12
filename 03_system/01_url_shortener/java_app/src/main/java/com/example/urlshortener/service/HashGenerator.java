package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

@Service
public class HashGenerator {

    private static final String BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public String encode(long id) {
        StringBuilder sb = new StringBuilder();
        long num = id;
        
        if (num == 0) {
            return String.valueOf(BASE62.charAt(0));
        }

        while (num > 0) {
            sb.append(BASE62.charAt((int) (num % 62)));
            num /= 62;
        }

        return sb.reverse().toString();
    }

    public long decode(String str) {
        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result * 62 + BASE62.indexOf(str.charAt(i));
        }
        return result;
    }
}
