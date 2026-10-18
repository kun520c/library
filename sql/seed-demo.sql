-- LOCAL DEVELOPMENT ONLY. Select the target database before executing.
-- Fictional catalogue: no accounts, passwords or personal data.
-- Re-runnable: insert missing active rows; never reset stock or overwrite existing rows.
-- Run while the local app is stopped; not a concurrent migration tool.
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO categories(name)
SELECT '[演示] 后端工程' WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE active_name = '[演示] 后端工程');
INSERT INTO categories(name)
SELECT '[演示] 数据系统' WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE active_name = '[演示] 数据系统');
INSERT INTO categories(name)
SELECT '[演示] 阅读与思考' WHERE NOT EXISTS (
    SELECT 1 FROM categories WHERE active_name = '[演示] 阅读与思考');

INSERT INTO books(title, author, isbn, price, stock, category_id)
SELECT demo.title, '一方书室示例编者', demo.isbn, demo.price, demo.stock, c.id
FROM (
    SELECT 'Java 后端实践手册' AS title, 'DEMO-JAVA-001' AS isbn, 59.00 AS price, 5 AS stock, '[演示] 后端工程' AS category
    UNION ALL SELECT 'Spring 应用设计笔记', 'DEMO-SPRING-002', 68.00, 3, '[演示] 后端工程'
    UNION ALL SELECT 'SQL 与事务实验', 'DEMO-SQL-003', 49.00, 4, '[演示] 数据系统'
    UNION ALL SELECT '缓存系统入门', 'DEMO-CACHE-004', 45.00, 2, '[演示] 数据系统'
    UNION ALL SELECT '代码阅读的方法', 'DEMO-READ-005', 39.00, 6, '[演示] 阅读与思考'
    UNION ALL SELECT '并发编程实验室', 'DEMO-CONCURRENT-006', 72.00, 1, '[演示] 后端工程'
    UNION ALL SELECT '数据建模练习册', 'DEMO-DATA-007', 52.00, 3, '[演示] 数据系统'
    UNION ALL SELECT '书页之间', 'DEMO-ESSAY-008', 32.00, 0, '[演示] 阅读与思考'
) demo
JOIN categories c ON c.active_name = demo.category
WHERE NOT EXISTS (SELECT 1 FROM books b WHERE b.active_isbn = demo.isbn);

COMMIT;
