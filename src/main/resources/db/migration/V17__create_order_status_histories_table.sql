CREATE TABLE order_status_histories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  notes VARCHAR(255) NULL,
  changed_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES orders (id),
  CONSTRAINT fk_osh_changed_by FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_osh_order_created ON order_status_histories(order_id, created_at);