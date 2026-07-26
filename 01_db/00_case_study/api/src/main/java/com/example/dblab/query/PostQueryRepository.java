package com.example.dblab.query;

import java.util.List;

public interface PostQueryRepository {

    List<PostSummary> searchPosts(String tag, int page, int fetchSize);
}

