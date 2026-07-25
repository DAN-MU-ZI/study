package com.example.urlshortener.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public final class ShortCodeConverter implements Converter<String, ShortCode> {

    @Override
    public ShortCode convert(String source) {
        long id = Base62Codec.decode(source);
        return new ShortCode(source, id);
    }
}
