package com.example.urlshortener.filter;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBitSetReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisBloomFilter {

    private final RedissonReactiveClient redissonReactiveClient;
    private RBitSetReactive shortUrlFilter;
    
    // Python bloom_filter.py와 동일한 설정
    private static final String BLOOM_KEY = "bloom:short_url";
    private static final int SIZE = 5_000_000;
    private static final int HASH_COUNT = 5;

    @PostConstruct
    public void init() {
        shortUrlFilter = redissonReactiveClient.getBitSet(BLOOM_KEY);
    }

    public Mono<Boolean> contains(String shortUrl) {
        List<Long> indices = getHashIndices(shortUrl);
        return Flux.fromIterable(indices)
                .flatMap(index -> shortUrlFilter.get(index))
                .all(val -> val);
    }

    public Mono<Boolean> add(String shortUrl) {
        List<Long> indices = getHashIndices(shortUrl);
        return Flux.fromIterable(indices)
                .flatMap(index -> shortUrlFilter.set(index))
                .then(Mono.just(true));
    }

    /**
     * Python의 _get_hash_indices 로직 1:1 이식
     * digest = hashlib.md5(item.encode()).hexdigest()
     * base_hash = int(digest, 16)
     * index = (base_hash + i * 1999) % self.size
     */
    private List<Long> getHashIndices(String item) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestBytes = md.digest(item.getBytes(StandardCharsets.UTF_8));
            
            // digest.hexdigest() 후 int(hex, 16) 하는 것과 BigInteger(1, bytes)는 동일
            BigInteger baseHash = new BigInteger(1, digestBytes);
            List<Long> indices = new ArrayList<>();
            
            for (int i = 0; i < HASH_COUNT; i++) {
                BigInteger iVal = BigInteger.valueOf(i);
                BigInteger step = BigInteger.valueOf(1999);
                BigInteger size = BigInteger.valueOf(SIZE);
                
                long index = baseHash.add(iVal.multiply(step))
                        .remainder(size)
                        .longValue();
                indices.add(index);
            }
            return indices;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
