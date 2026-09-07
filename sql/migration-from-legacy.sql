-- 警告：执行前必须完整备份数据库。
-- 必须先检查并处理重复账号和重复的未删除 ISBN（见下方检查 SQL）。
-- 本文件是人工审核用的迁移参考，不是应用自动执行脚本。
-- 未经 DBA/负责人确认，不得直接在生产库执行。

USE library;

-- 迁移前检查；任何一条返回数据都必须先人工处理。
SELECT account, COUNT(*) AS duplicate_count
FROM `User`
GROUP BY account
HAVING COUNT(*) > 1;

SELECT isbn, COUNT(*) AS duplicate_count
FROM `Book`
WHERE is_deleted = 0
GROUP BY isbn
HAVING COUNT(*) > 1;

-- 请确认目标表 users/books 不存在后再执行重命名。
RENAME TABLE `User` TO users, `Book` TO books;

ALTER TABLE users
    CHANGE COLUMN id id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN username VARCHAR(50) NOT NULL,
    MODIFY COLUMN account VARCHAR(32) NOT NULL,
    MODIFY COLUMN password VARCHAR(100) NOT NULL,
    ADD COLUMN role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' AFTER password,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER role,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD UNIQUE KEY uk_users_account (account);

CREATE TABLE categories (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    active_name VARCHAR(50) GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN name ELSE NULL END
    ) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_active_name (active_name),
    KEY idx_categories_deleted_id (is_deleted, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 为旧图书中已有的 category_id 创建可识别的占位分类，迁移后可通过 API 改名。
INSERT INTO categories (id, name)
SELECT DISTINCT category_id, CONCAT('迁移分类-', category_id)
FROM books
WHERE category_id IS NOT NULL;

ALTER TABLE books
    CHANGE COLUMN id id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN title VARCHAR(200) NOT NULL,
    MODIFY COLUMN author VARCHAR(100) NOT NULL,
    MODIFY COLUMN isbn VARCHAR(32) NOT NULL,
    MODIFY COLUMN price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    MODIFY COLUMN stock INT UNSIGNED NOT NULL DEFAULT 0,
    MODIFY COLUMN category_id INT UNSIGNED NULL,
    MODIFY COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN active_isbn VARCHAR(32) GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN isbn ELSE NULL END
    ) STORED AFTER is_deleted,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER active_isbn,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD UNIQUE KEY uk_books_active_isbn (active_isbn),
    ADD KEY idx_books_category_id (category_id),
    ADD KEY idx_books_deleted_id (is_deleted, id),
    ADD CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id);

CREATE TABLE borrow_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    book_id INT UNSIGNED NOT NULL,
    borrow_time DATETIME NOT NULL,
    due_time DATETIME NOT NULL,
    return_time DATETIME NULL,
    status ENUM('BORROWED', 'RETURNED') NOT NULL DEFAULT 'BORROWED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_borrow_user_status_time (user_id, status, borrow_time),
    KEY idx_borrow_book_status (book_id, status),
    KEY idx_borrow_status_due (status, due_time),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_borrow_due_time CHECK (due_time > borrow_time),
    CONSTRAINT chk_borrow_return_state CHECK (
        (status = 'BORROWED' AND return_time IS NULL)
        OR (status = 'RETURNED' AND return_time IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
