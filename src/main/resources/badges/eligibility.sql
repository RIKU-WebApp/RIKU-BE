__VISITS__, days AS (SELECT DISTINCT user_id,DATE_SUB(DATE(local_time),INTERVAL WEEKDAY(local_time) DAY) AS monday FROM visits),
 islands AS (SELECT user_id, TO_DAYS(monday)-7*CAST(ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY monday) AS SIGNED) AS island FROM days),
 streaks AS (SELECT user_id,COUNT(*) AS n FROM islands GROUP BY user_id,island),
 monthly_places AS (__MONTHLY_RANKS__),
 course_counts AS (SELECT user_id, COUNT(DISTINCT post_type) AS kinds,
 SUM(post_type='REGULAR') AS r, SUM(post_type='FLASH') AS f,
 SUM(post_type='TRAINING') AS t, SUM(post_type='EVENT') AS e FROM visits GROUP BY user_id),
 season_counts AS (SELECT user_id, COUNT(*) AS n FROM visits GROUP BY user_id,FLOOR(MOD(MONTH(local_time),12)/3)),
 base_metrics AS (
 SELECT user_id,'visits' AS metric,COUNT(*) AS n FROM visits GROUP BY user_id
 UNION ALL SELECT user_id,'months',COUNT(DISTINCT DATE_FORMAT(local_time,'%Y-%m')) FROM visits GROUP BY user_id
 UNION ALL SELECT user_id,'kinds',CASE WHEN r>=15 AND f>=15 AND t>=6 AND e>=6 THEN 6 WHEN r>=3 AND f>=3 AND t>=2 AND e>=2 THEN 5 ELSE kinds END FROM course_counts
 UNION ALL SELECT user_id,'leads',SUM(led) FROM visits GROUP BY user_id
 UNION ALL SELECT user_id,post_type,COUNT(*) FROM visits GROUP BY user_id,post_type
 UNION ALL SELECT user_id,'weekends',SUM(WEEKDAY(local_time)>=5) FROM visits GROUP BY user_id
 UNION ALL SELECT user_id,'streak',MAX(n) FROM streaks GROUP BY user_id
 UNION ALL SELECT p.user_id,'hosted',COUNT(*) FROM post p
 WHERE p.status='ACTIVE' AND p.post_status='CLOSED' AND EXISTS
 (SELECT 1 FROM participant a WHERE a.post_id=p.post_id AND a.user_id<>p.user_id AND a.participant_status='ATTENDED') GROUP BY p.user_id
 UNION ALL SELECT user_id,'verifiedKm',COALESCE(SUM(km),0) FROM visits GROUP BY user_id
 UNION ALL SELECT r.user_id,'personalKm',SUM(r.distance_km) FROM personal_run_record r  GROUP BY r.user_id
 UNION ALL SELECT user_id,'monthlyRank',CASE WHEN best_rank=1 THEN 3 WHEN best_rank<=3 THEN 2 WHEN best_rank<=10 THEN 1 ELSE 0 END FROM monthly_places
 UNION ALL SELECT user_id,'checkins',COUNT(DISTINCT DATE(DATE_ADD(created_at,INTERVAL 9 HOUR))) FROM user_point WHERE point_type='ADD_ATTENDANCE' GROUP BY user_id
 UNION ALL SELECT user_id,'fourSeasons',CASE WHEN COUNT(*)=4 AND MIN(n)>=5 THEN 6 WHEN COUNT(*)=4 AND MIN(n)>=2 THEN 5 ELSE COUNT(*) END FROM season_counts GROUP BY user_id
 UNION ALL SELECT user_id,'certificateDays',COUNT(DISTINCT date) FROM run_certificate GROUP BY user_id
 __RECORD_PEAKS__
), rules AS (
 SELECT metric,COALESCE(code_prefix,metric) AS code_prefix,goal FROM JSON_TABLE(?, '$[*]' COLUMNS(
   metric VARCHAR(32) PATH '$.id', code_prefix VARCHAR(32) PATH '$.prefix', NESTED PATH '$.levels[*]' COLUMNS(goal INT PATH '$')
 )) AS catalog
), qualified AS (
 SELECT DISTINCT m.user_id, CONCAT(r.code_prefix,'_',r.goal) AS code
 FROM base_metrics m JOIN rules r ON BINARY r.metric=BINARY m.metric AND m.n>=r.goal
)
SELECT u.user_id,u.status,q.code FROM users u LEFT JOIN qualified q ON q.user_id=u.user_id
ORDER BY u.user_id,q.code
