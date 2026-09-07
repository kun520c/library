-- 从已经使用 users/books 表、role 和 active_isbn 的上一版升级到分类与借阅闭环。
-- 执行前必须备份。本脚本不会删除表或数据，也不会由应用自动执行。
USE library;

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

INSERT INTO categories (id, name)
SELECT DISTINCT category_id, CONCAT('迁移分类-', category_id)
FROM books
WHERE category_id IS NOT NULL;

ALTER TABLE books
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
    KEY idx_borrow_book_id (book_id),
    KEY idx_borrow_status_due (status, due_time),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_borrow_due_time CHECK (due_time > borrow_time),
    CONSTRAINT chk_borrow_return_state CHECK (
        (status = 'BORROWED' AND return_time IS NULL)
        OR (status = 'RETURNED' AND return_time IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
