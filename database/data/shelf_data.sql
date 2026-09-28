-- ============================================================
-- 初始化数据：货架 + 货位
-- 货架：A-01 ~ A-03（A 区）、B-01 ~ B-03（B 区）
-- 每个货架 10 个货位（2 行 × 5 列），编号如 A-01-01 ~ A-01-10
-- ============================================================
INSERT INTO `shelf` (`shelf_no`, `area`, `capacity`, `used_count`, `status`, `remark`) VALUES
  ('A-01', 'A 区', 10, 0, 1, NULL),
  ('A-02', 'A 区', 10, 0, 1, NULL),
  ('A-03', 'A 区', 10, 0, 1, NULL),
  ('B-01', 'B 区', 10, 0, 1, NULL),
  ('B-02', 'B 区', 10, 0, 1, NULL),
  ('B-03', 'B 区', 10, 0, 1, NULL);

-- 为每个货架生成 10 个货位（行号 = CEIL(n/5)，列号 = ((n-1) MOD 5)+1）
INSERT INTO `shelf_location` (`shelf_id`, `location_no`, `row_no`, `column_no`, `capacity`, `used_count`, `status`)
SELECT s.id,
       CONCAT(s.shelf_no, '-', LPAD(seq.n, 2, '0')),
       CEIL(seq.n / 5),
       ((seq.n - 1) MOD 5) + 1,
       1,
       0,
       1
FROM `shelf` s
JOIN (
  SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) seq
ON NOT EXISTS (SELECT 1 FROM `shelf_location` l WHERE l.location_no = CONCAT(s.shelf_no, '-', LPAD(seq.n, 2, '0')));
