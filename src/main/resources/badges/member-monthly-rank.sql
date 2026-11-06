WITH member_months AS (
    SELECT EXTRACT(YEAR_MONTH FROM TIMESTAMPADD(HOUR, 9, p.created_at)) AS month_key,
           SUM(p.point) AS member_points
    FROM user_point p JOIN users u ON u.user_id = p.user_id
    WHERE p.user_id = ? AND p.created_at < ? AND u.status = 'ACTIVE' AND u.user_id NOT IN (10,29,6,79)
    GROUP BY month_key
    HAVING SUM(p.point) > 0
), scores AS (
    SELECT m.month_key, m.member_points, p.user_id, SUM(p.point) AS points
    FROM member_months m JOIN user_point p
      ON EXTRACT(YEAR_MONTH FROM TIMESTAMPADD(HOUR, 9, p.created_at)) = m.month_key
    JOIN users u ON u.user_id = p.user_id
    WHERE u.status = 'ACTIVE' AND u.user_id NOT IN (10,29,6,79)
    GROUP BY m.month_key, m.member_points, p.user_id
    HAVING SUM(p.point) > 0
)
SELECT MIN(place) FROM (
    SELECT 1 + SUM(CASE WHEN points > member_points THEN 1 ELSE 0 END) AS place
    FROM scores GROUP BY month_key HAVING COUNT(*) >= 20
) eligible
