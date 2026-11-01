ALTER TABLE payments 
ADD idempotency_key VARCHAR(255) NULL UNIQUE AFTER paid_at;
