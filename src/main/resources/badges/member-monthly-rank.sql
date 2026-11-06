WITH member_months AS (
    SELECT CAST(DATE_FORMAT(TIMESTAMPADD(HOUR, 9, p.created_at), '%Y-%m-01') AS DATE) AS month_start,
           SUM(p.point) AS member_points
    FROM user_point p JOIN users u ON u.user_id = p.user_id
    WHERE p.user_id = ? AND p.created_at < ? AND u.status = 'ACTIVE' AND u.user_id NOT IN (10,29,6,79)
    GROUP BY month_start
    HAVING SUM(p.point) > 0
), scores AS (
    SELECT m.month_start, m.member_points, p.user_id, SUM(p.point) AS points
    FROM member_months m JOIN user_point p
      ON p.created_at >= TIMESTAMPADD(HOUR, -9, m.month_start)
     AND p.created_at < TIMESTAMPADD(HOUR, -9, TIMESTAMPADD(MONTH, 1, m.month_start))
    JOIN users u ON u.user_id = p.user_id
    WHERE u.status = 'ACTIVE' AND u.user_id NOT IN (10,29,6,79)
    GROUP BY m.month_start, m.member_points, p.user_id
    HAVING SUM(p.point) > 0
)
SELECT MIN(place) FROM (
    SELECT 1 + SUM(CASE WHEN points > member_points THEN 1 ELSE 0 END) AS place
    FROM scores GROUP BY month_start HAVING COUNT(*) >= 20
) eligible
