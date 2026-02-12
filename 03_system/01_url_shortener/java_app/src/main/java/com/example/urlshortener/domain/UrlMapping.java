package com.example.urlshortener.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "url_mappings")
public class UrlMapping {

    @Id
    private Long id;

    @Field("long_url")
    private String longUrl;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    public UrlMapping(Long id, String longUrl) {
        this.id = id;
        this.longUrl = longUrl;
    }
}
