SET NOCOUNT ON;

SELECT LOWER(table_info.name) AS table_name,
       SUM(partition_info.row_count) AS row_count
FROM StackOverflow2010.sys.tables AS table_info
JOIN StackOverflow2010.sys.dm_db_partition_stats AS partition_info
  ON partition_info.object_id = table_info.object_id
WHERE partition_info.index_id IN (0, 1)
GROUP BY table_info.name
ORDER BY table_name;
