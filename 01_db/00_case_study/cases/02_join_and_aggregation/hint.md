# 힌트

- 답변은 `posts.posttypeid = 2`다.
- 질문의 `acceptedanswerid`가 답변의 `id`와 같으면 채택 답변이다.
- 사용자별 집계 후 답변 수 조건은 `WHERE`가 아닌 `HAVING`에 둔다.
- 질문 조인은 채택되지 않은 답변도 남아야 하므로 `LEFT JOIN`이다.

