WITH monthly_scores AS (
    SELECT date_trunc('month', posts.creationdate)::date AS month,
           posts.owneruserid AS user_id,
           count(*) AS answer_count,
           sum(posts.score) AS total_score
    FROM posts
    WHERE posts.posttypeid = 2
      AND posts.owneruserid IS NOT NULL
      AND posts.creationdate >= TIMESTAMP '2009-01-01 00:00:00'
      AND posts.creationdate <  TIMESTAMP '2010-01-01 00:00:00'
    GROUP BY month, posts.owneruserid
), ranked AS (
    SELECT monthly_scores.*,
           rank() OVER (
               PARTITION BY month
               ORDER BY total_score DESC
           ) AS monthly_rank
    FROM monthly_scores
)
SELECT ranked.month,
       ranked.user_id,
       users.displayname,
       ranked.answer_count,
       ranked.total_score,
       ranked.monthly_rank
FROM ranked
JOIN users ON users.id = ranked.user_id
WHERE ranked.monthly_rank <= 10
ORDER BY ranked.month, ranked.monthly_rank, ranked.user_id;

