SELECT user_id, MIN(place) AS best_rank FROM (
 SELECT user_id, month_key, RANK() OVER(PARTITION BY month_key ORDER BY points DESC) AS place,
        COUNT(*) OVER(PARTITION BY month_key) AS competitors
 FROM (
  SELECT p.user_id,100*YEAR(TIMESTAMPADD(HOUR,9,p.created_at))+MONTH(TIMESTAMPADD(HOUR,9,p.created_at)) AS month_key,
         SUM(p.point) AS points
  FROM user_point p JOIN users u ON u.user_id=p.user_id
  WHERE u.status='ACTIVE' AND u.user_id NOT IN (10,29,6,79)
  GROUP BY p.user_id,month_key HAVING SUM(p.point)>0
 ) totals WHERE month_key < __MONTH_KEY__
) standings WHERE competitors>=20 GROUP BY user_id
