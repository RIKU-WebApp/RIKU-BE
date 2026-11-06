WITH scores AS (
    SELECT p.user_id, EXTRACT(YEAR_MONTH FROM TIMESTAMPADD(HOUR, 9, p.created_at)) AS month_key,
           SUM(p.point) AS points
    FROM user_point p JOIN users u ON u.user_id = p.user_id
    WHERE p.created_at < ? AND u.status = 'ACTIVE' AND u.user_id NOT IN (10,29,6,79)
    GROUP BY p.user_id, month_key
    HAVING SUM(p.point) > 0
)
SELECT MIN(place) FROM (
    SELECT 1 + SUM(CASE WHEN competitor.points > mine.points THEN 1 ELSE 0 END) AS place
    FROM scores mine JOIN scores competitor ON competitor.month_key = mine.month_key
    WHERE mine.user_id = ?
    GROUP BY mine.month_key
    HAVING COUNT(*) >= 20
) eligible
