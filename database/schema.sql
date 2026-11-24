CREATE DATABASE IF NOT EXISTS ucampus_mart CHARACTER SET utf8mb4;
USE ucampus_mart;

CREATE TABLE IF NOT EXISTS marketplace_lock (
    id TINYINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO marketplace_lock (id) VALUES (1)
ON DUPLICATE KEY UPDATE id = 1;

CREATE TABLE IF NOT EXISTS listings (
    id INT NOT NULL PRIMARY KEY,
    owner VARCHAR(24) NOT NULL,
    title LONGTEXT NOT NULL,
    category LONGTEXT NOT NULL,
    item_condition LONGTEXT NOT NULL,
    age_months INT NOT NULL,
    mode VARCHAR(8) NOT NULL,
    -- Store the exact decimal string so Java's BigDecimal values round-trip.
    price LONGTEXT,
    status VARCHAR(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS offers (
    id INT NOT NULL PRIMARY KEY,
    requester VARCHAR(24) NOT NULL,
    target_id INT NOT NULL,
    offered_id INT,
    status VARCHAR(16) NOT NULL,
    FOREIGN KEY (target_id) REFERENCES listings (id),
    FOREIGN KEY (offered_id) REFERENCES listings (id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;
