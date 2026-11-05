SELECT member,place FROM (
                  SELECT member,RANK() OVER(ORDER BY score DESC) AS place,COUNT(*) OVER() AS competitors FROM (
                    SELECT p.user_id AS member,SUM(p.point) AS score FROM user_point p JOIN users u ON u.user_id=p.user_id
                    WHERE u.status='ACTIVE' AND u.user_id NOT IN (10,29,6,79) AND p.created_at>=? AND p.created_at<?
                    GROUP BY p.user_id HAVING SUM(p.point)>0
                  ) scores
                ) ranked WHERE place<=3 AND competitors>=? ORDER BY place,member
