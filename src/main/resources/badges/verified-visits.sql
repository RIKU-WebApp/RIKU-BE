WITH old_pacer_values AS (
 SELECT post_id,pacer_group,LOWER(REPLACE(TRIM(distance),' ','')) AS raw_km FROM pacer
 WHERE pacer_group IS NOT NULL AND TRIM(pacer_group)<>''
), old_pacer_km AS (
 SELECT post_id,pacer_group,CASE WHEN REGEXP_LIKE(raw_km,'^[0-9]{1,3}([.][0-9]{1,2})?(km)?$')
     THEN CAST(REPLACE(raw_km,'km','') AS DECIMAL(7,2)) END AS km FROM old_pacer_values
), old_groups AS (
 SELECT post_id,pacer_group,MIN(km) AS km FROM old_pacer_km GROUP BY post_id,pacer_group
 HAVING COUNT(*)=COUNT(km) AND MIN(km)=MAX(km) AND MIN(km)>0 AND MAX(km)<=200
), attended_rows AS (
 SELECT DISTINCT a.user_id,p.post_id,p.post_type,DATE_ADD(p.date,INTERVAL 9 HOUR) AS local_time,
 EXISTS(SELECT 1 FROM pacer x WHERE x.user_id=a.user_id AND x.post_id=p.post_id) AS led,
 COALESCE(g.distance_km,old.km) AS km
 FROM participant a JOIN post p ON p.post_id=a.post_id
 LEFT JOIN run_recruitment_group g ON g.post_id=p.post_id AND g.group_name=a.participant_group
 LEFT JOIN old_groups old ON old.post_id=p.post_id AND old.pacer_group=a.participant_group
 WHERE p.status='ACTIVE' AND p.post_status='CLOSED' AND a.participant_status='ATTENDED' __MEMBER_FILTER__
), visits AS (
 SELECT user_id,post_id,post_type,local_time,MAX(led) AS led,CASE WHEN COUNT(*)=COUNT(km) AND MIN(km)=MAX(km) THEN MIN(km) END AS km
 FROM attended_rows GROUP BY user_id,post_id,post_type,local_time
)
