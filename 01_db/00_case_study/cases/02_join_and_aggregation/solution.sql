SELECT users.id AS user_id,
       users.displayname,
       count(*) AS answer_count,
       sum(answers.score) AS total_answer_score,
       count(questions.id) AS accepted_answer_count
FROM posts AS answers
JOIN users
  ON users.id = answers.owneruserid
LEFT JOIN posts AS questions
  ON questions.acceptedanswerid = answers.id
WHERE answers.posttypeid = 2
  AND answers.creationdate >= TIMESTAMP '2009-01-01 00:00:00'
  AND answers.creationdate <  TIMESTAMP '2010-01-01 00:00:00'
GROUP BY users.id, users.displayname
HAVING count(*) >= 10
ORDER BY total_answer_score DESC,
         accepted_answer_count DESC,
         user_id DESC;

