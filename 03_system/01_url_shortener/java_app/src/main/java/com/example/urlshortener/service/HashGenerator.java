package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

@Service
public class HashGenerator {

    public String generateShortUrl(String longUrl, int length) {
        CRC32 crc = new CRC32();
        crc.update(longUrl.getBytes(StandardCharsets.UTF_8));
        long hash = crc.getValue();

        String hex = Long.toHexString(hash);
        
        if (hex.length() < length) {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < length - hex.length()) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        } else {
            return hex.substring(0, length);
        }
    }
}
