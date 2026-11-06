UNION ALL SELECT user_id,'personalKm',personal_km FROM badge_record_peak WHERE personal_km>0
 UNION ALL SELECT user_id,'certificateDays',1 FROM badge_record_peak WHERE had_certificate=TRUE
