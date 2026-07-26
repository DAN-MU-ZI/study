package com.example.dblab.query;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostQueryController.class)
class PostQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostQueryRepository repository;

    @Test
    void searchPosts_returnsStablePageContract() throws Exception {
        var post = new PostSummary(
            42L,
            "How does PostgreSQL use an index?",
            17,
            LocalDateTime.parse("2010-01-02T03:04:05"),
            7L,
            "<postgresql><index>"
        );
        when(repository.searchPosts("postgresql", 0, 21)).thenReturn(List.of(post));

        mockMvc.perform(get("/api/posts").param("tag", "<PostgreSQL>"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(42))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.pageSize").value(20))
            .andExpect(jsonPath("$.hasNext").value(false));

        verify(repository).searchPosts("postgresql", 0, 21);
    }

    @Test
    void searchPosts_rejectsPageSizeOverOneHundred() throws Exception {
        mockMvc.perform(
                get("/api/posts")
                    .param("tag", "java")
                    .param("pageSize", "101")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchPosts_rejectsPageOverTenThousand() throws Exception {
        mockMvc.perform(
                get("/api/posts")
                    .param("tag", "java")
                    .param("page", "10001")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchPosts_usesOneExtraRowToCalculateHasNext() throws Exception {
        var rows = java.util.stream.LongStream.rangeClosed(1, 3)
            .mapToObj(id -> new PostSummary(id, "post-" + id, 1, LocalDateTime.MIN, null, "<java>"))
            .toList();
        when(repository.searchPosts("java", 0, 3)).thenReturn(rows);

        mockMvc.perform(
                get("/api/posts")
                    .param("tag", "java")
                    .param("pageSize", "2")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.hasNext").value(true));

        verify(repository).searchPosts("java", 0, 3);
    }
}
