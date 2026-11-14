__VISITS__,
pending AS (
 SELECT g.user_id,g.badge_code,SUBSTRING_INDEX(g.badge_code,'_',1) AS family,
   rule.goal,
   DATE(TIMESTAMPADD(SECOND,FLOOR(UNIX_TIMESTAMP(g.granted_at))+32400,CAST('1970-01-01' AS DATETIME))) AS issued_day
 FROM automatic_badge_grant g
 LEFT JOIN JSON_TABLE(?, '$[*]' COLUMNS(code VARCHAR(64) PATH '$.code', goal INT PATH '$.goal')) AS rule
        ON BINARY rule.code=BINARY g.badge_code
 __MISSING__
), totals AS (
 SELECT v.*, COUNT(*) OVER w AS n,SUM(led) OVER w AS leads,SUM(WEEKDAY(local_time)>=5) OVER w AS weekends,
 SUM(COALESCE(km,0)) OVER w AS distance_km,SUM(post_type='REGULAR') OVER w AS r,
 SUM(post_type='FLASH') OVER w AS f,SUM(post_type='TRAINING') OVER w AS t,SUM(post_type='EVENT') OVER w AS e,
 SUM(season=0) OVER w AS winter,SUM(season=1) OVER w AS spring,SUM(season=2) OVER w AS summer,SUM(season=3) OVER w AS autumn
 FROM (SELECT visits.*,FLOOR(MOD(MONTH(local_time),12)/3) AS season FROM visits) v
 WINDOW w AS (PARTITION BY user_id ORDER BY local_time,post_id ROWS UNBOUNDED PRECEDING)
), week_days AS (
 SELECT user_id,DATE_SUB(DATE(local_time),INTERVAL WEEKDAY(local_time) DAY) AS monday,MIN(DATE(local_time)) AS day_on
 FROM visits GROUP BY user_id,monday
), islands AS (
 SELECT week_days.*,TO_DAYS(monday)-7*CAST(ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY monday) AS SIGNED) AS island FROM week_days
), other_events AS (
 SELECT user_id,'streak' AS family,day_on,ROW_NUMBER() OVER(PARTITION BY user_id,island ORDER BY monday) AS n FROM islands
 UNION ALL SELECT user_id,'months',day_on,ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY day_on)
 FROM (SELECT user_id,MIN(DATE(local_time)) AS day_on FROM visits GROUP BY user_id,EXTRACT(YEAR_MONTH FROM local_time)) m
 UNION ALL SELECT user_id,'checkins',day_on,ROW_NUMBER() OVER(PARTITION BY user_id ORDER BY day_on)
 FROM (SELECT DISTINCT user_id,DATE(TIMESTAMPADD(HOUR,9,created_at)) AS day_on FROM user_point WHERE point_type='ADD_ATTENDANCE') c
 UNION ALL SELECT p.user_id,'hosted',DATE(TIMESTAMPADD(HOUR,9,p.date)),ROW_NUMBER() OVER(PARTITION BY p.user_id ORDER BY p.date,p.post_id)
 FROM post p WHERE p.status='ACTIVE' AND p.post_status='CLOSED' AND EXISTS
 (SELECT 1 FROM participant a WHERE a.post_id=p.post_id AND a.user_id<>p.user_id AND a.participant_status='ATTENDED')
), hits AS (
 SELECT g.user_id,g.badge_code,MIN(DATE(v.local_time)) AS achieved_on FROM pending g JOIN totals v ON g.user_id=v.user_id
 WHERE DATE(v.local_time)<=g.issued_day AND CASE g.family
 WHEN 'visits' THEN n WHEN 'REGULAR' THEN r WHEN 'FLASH' THEN f WHEN 'TRAINING' THEN t WHEN 'EVENT' THEN e
 WHEN 'leads' THEN leads WHEN 'weekends' THEN weekends WHEN 'CLUB' THEN distance_km
 WHEN 'kinds' THEN CASE WHEN r>=15 AND f>=15 AND t>=6 AND e>=6 THEN 6 WHEN r>=3 AND f>=3 AND t>=2 AND e>=2 THEN 5 ELSE (r>0)+(f>0)+(t>0)+(e>0) END
 WHEN 'fourSeasons' THEN CASE WHEN LEAST(winter,spring,summer,autumn)>=5 THEN 6 WHEN LEAST(winter,spring,summer,autumn)>=2 THEN 5 ELSE (winter>0)+(spring>0)+(summer>0)+(autumn>0) END
 END >=g.goal GROUP BY g.user_id,g.badge_code
 UNION ALL SELECT g.user_id,g.badge_code,MIN(x.day_on) FROM pending g JOIN other_events x ON g.user_id=x.user_id AND BINARY x.family=g.family
 WHERE x.n>=g.goal AND x.day_on<=g.issued_day GROUP BY g.user_id,g.badge_code
)
SELECT g.user_id,g.badge_code,h.achieved_on FROM pending g LEFT JOIN hits h ON g.user_id=h.user_id AND g.badge_code=h.badge_code
