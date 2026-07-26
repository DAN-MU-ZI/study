package com.example.dblab.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/posts")
public class PostQueryController {

    private final PostQueryRepository repository;

    public PostQueryController(PostQueryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public PageResponse<PostSummary> searchPosts(
        @RequestParam String tag,
        @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize
    ) {
        var normalizedTag = TagFilter.normalize(tag);
        var rows = repository.searchPosts(normalizedTag, page, pageSize + 1);
        return PageResponse.from(rows, page, pageSize);
    }
}
