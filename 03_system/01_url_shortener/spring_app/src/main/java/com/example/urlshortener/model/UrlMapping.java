package com.example.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("url_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {
    @Id
    private Integer id;

    @Column("short_url")
    private String shortUrl;

    @Column("long_url")
    private String longUrl;

    public UrlMapping(String shortUrl, String longUrl) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
    }
}
