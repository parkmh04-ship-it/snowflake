CREATE TABLE IF NOT EXISTS global_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    global_transaction_id BIGINT NOT NULL,
    origin_global_transaction_id BIGINT,
    created_at DATETIME NOT NULL,
    INDEX idx_global_trx_id (global_transaction_id),
    INDEX idx_origin_global_trx_id (origin_global_transaction_id)
);
