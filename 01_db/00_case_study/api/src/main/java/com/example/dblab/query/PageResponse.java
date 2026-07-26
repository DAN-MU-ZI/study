package com.example.dblab.query;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    int page,
    int pageSize,
    boolean hasNext
) {

    static <T> PageResponse<T> from(List<T> fetchedRows, int page, int pageSize) {
        var hasNext = fetchedRows.size() > pageSize;
        var data = hasNext ? fetchedRows.subList(0, pageSize) : fetchedRows;
        return new PageResponse<>(List.copyOf(data), page, pageSize, hasNext);
    }
}

