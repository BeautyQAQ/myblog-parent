-- Apply once to the application database after applying V1__initial_schema.sql.
-- The application limits each upload to 5 MiB; MEDIUMBLOB avoids MySQL's 64 KiB BLOB limit.
CREATE TABLE IF NOT EXISTS tb_image (
    image_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    content_type VARCHAR(32) CHARACTER SET ascii NOT NULL,
    image_data MEDIUMBLOB NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (image_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
