package com.example.dblab.query;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcPostQueryRepository implements PostQueryRepository {

    private final JdbcClient jdbcClient;

    public JdbcPostQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<PostSummary> searchPosts(String tag, int page, int fetchSize) {
        var pageSize = fetchSize - 1;
        var offset = Math.multiplyExact(page, pageSize);

        return jdbcClient.sql("""
                SELECT id, title, score, creationdate, owneruserid, tags
                FROM posts
                WHERE tags LIKE :tagPattern
                ORDER BY score DESC, id DESC
                LIMIT :fetchSize OFFSET :offset
                """)
            .param("tagPattern", "%<" + tag + ">%")
            .param("fetchSize", fetchSize)
            .param("offset", offset)
            .query((resultSet, rowNumber) -> new PostSummary(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getInt("score"),
                resultSet.getTimestamp("creationdate").toLocalDateTime(),
                resultSet.getObject("owneruserid", Long.class),
                resultSet.getString("tags")
            ))
            .list();
    }
}

