-- =============================================================================
-- Sistema Fábrica de Repuestos - DDL Oracle
-- Ejecutar como usuario con permisos de creación (ej. SYS o schema propietario).
-- =============================================================================

-- 1) SECUENCIAS
CREATE SEQUENCE role_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE app_user_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE user_address_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE enterprise_profile_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE category_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE brand_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE vehicle_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_image_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_tech_spec_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE inventory_movement_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE order_header_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE order_item_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE order_status_history_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_review_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE json_operation_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE market_feed_config_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE market_sales_snapshot_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_number_map_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- 2) TABLAS - Seguridad / Usuarios
CREATE TABLE role (
  role_id   NUMBER(19) PRIMARY KEY,
  name      VARCHAR2(100) NOT NULL
);

CREATE TABLE app_user (
  user_id       NUMBER(19) PRIMARY KEY,
  email         VARCHAR2(255) NOT NULL,
  password_hash VARCHAR2(255) NOT NULL,
  full_name     VARCHAR2(200),
  phone         VARCHAR2(50),
  status        VARCHAR2(20) DEFAULT 'ACTIVE',
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_app_user_email UNIQUE (email),
  CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE','BLOCKED'))
);

CREATE TABLE user_role (
  user_id NUMBER(19) NOT NULL,
  role_id NUMBER(19) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(role_id)
);

CREATE TABLE user_address (
  address_id          NUMBER(19) PRIMARY KEY,
  user_id             NUMBER(19) NOT NULL,
  line1               VARCHAR2(200) NOT NULL,
  line2               VARCHAR2(200),
  city                VARCHAR2(100),
  state               VARCHAR2(100),
  zip                 VARCHAR2(20),
  country             VARCHAR2(100),
  is_default_shipping NUMBER(1) DEFAULT 0,
  is_default_billing  NUMBER(1) DEFAULT 0,
  CONSTRAINT fk_user_address_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT chk_def_ship CHECK (is_default_shipping IN (0,1)),
  CONSTRAINT chk_def_bill CHECK (is_default_billing IN (0,1))
);

CREATE TABLE enterprise_profile (
  enterprise_id              NUMBER(19) PRIMARY KEY,
  user_id                    NUMBER(19) NOT NULL,
  api_key                    VARCHAR2(255),
  default_shipping_address_id NUMBER(19),
  default_billing_address_id  NUMBER(19),
  default_card_token         VARCHAR2(255),
  delivery_window            VARCHAR2(500),
  discount_percent            NUMBER(5,2) DEFAULT 0,
  CONSTRAINT uq_enterprise_user UNIQUE (user_id),
  CONSTRAINT fk_enterprise_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT fk_enterprise_ship FOREIGN KEY (default_shipping_address_id) REFERENCES user_address(address_id),
  CONSTRAINT fk_enterprise_bill FOREIGN KEY (default_billing_address_id) REFERENCES user_address(address_id)
);

-- 3) Catálogo
CREATE TABLE category (
  category_id NUMBER(19) PRIMARY KEY,
  name        VARCHAR2(200) NOT NULL,
  parent_id   NUMBER(19),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(category_id)
);

CREATE TABLE brand (
  brand_id NUMBER(19) PRIMARY KEY,
  name     VARCHAR2(200) NOT NULL
);

CREATE TABLE vehicle (
  vehicle_id              NUMBER(19) PRIMARY KEY,
  universal_vehicle_code  VARCHAR2(100) NOT NULL,
  make                   VARCHAR2(100),
  line                   VARCHAR2(100),
  year_number            NUMBER(4),
  CONSTRAINT uq_vehicle_code_year UNIQUE (universal_vehicle_code, year_number)
);

CREATE TABLE part (
  part_id       NUMBER(19) PRIMARY KEY,
  category_id   NUMBER(19) NOT NULL,
  brand_id      NUMBER(19) NOT NULL,
  part_number   VARCHAR2(100) NOT NULL,
  title         VARCHAR2(500) NOT NULL,
  description   CLOB,
  weight_lb     NUMBER(10,2),
  price         NUMBER(12,2) NOT NULL,
  active        NUMBER(1) DEFAULT 1,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_part_number UNIQUE (part_number),
  CONSTRAINT fk_part_category FOREIGN KEY (category_id) REFERENCES category(category_id),
  CONSTRAINT fk_part_brand FOREIGN KEY (brand_id) REFERENCES brand(brand_id),
  CONSTRAINT chk_part_active CHECK (active IN (0,1))
);

CREATE TABLE part_image (
  image_id   NUMBER(19) PRIMARY KEY,
  part_id    NUMBER(19) NOT NULL,
  url_path   VARCHAR2(500) NOT NULL,
  sort_order NUMBER(5) DEFAULT 0,
  CONSTRAINT fk_part_image_part FOREIGN KEY (part_id) REFERENCES part(part_id)
);

CREATE TABLE part_tech_spec (
  spec_id    NUMBER(19) PRIMARY KEY,
  part_id    NUMBER(19) NOT NULL,
  spec_name  VARCHAR2(200) NOT NULL,
  spec_value VARCHAR2(500),
  CONSTRAINT fk_spec_part FOREIGN KEY (part_id) REFERENCES part(part_id)
);

CREATE TABLE part_compatibility (
  part_id    NUMBER(19) NOT NULL,
  vehicle_id NUMBER(19) NOT NULL,
  PRIMARY KEY (part_id, vehicle_id),
  CONSTRAINT fk_comp_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_comp_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
);

-- 4) Inventario
CREATE TABLE inventory_movement (
  movement_id   NUMBER(19) PRIMARY KEY,
  part_id       NUMBER(19) NOT NULL,
  user_id       NUMBER(19) NOT NULL,
  movement_type VARCHAR2(30) NOT NULL,
  quantity      NUMBER(10) NOT NULL,
  reference     VARCHAR2(500),
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mov_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_mov_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT chk_mov_type CHECK (movement_type IN ('INBOUND','ADJUSTMENT','OUTBOUND_RESERVED','OUTBOUND_SOLD'))
);

CREATE TABLE part_stock (
  part_id      NUMBER(19) PRIMARY KEY,
  on_hand_qty  NUMBER(10) DEFAULT 0,
  reserved_qty NUMBER(10) DEFAULT 0,
  updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_stock_part FOREIGN KEY (part_id) REFERENCES part(part_id)
);

-- 5) Pedidos
CREATE TABLE order_header (
  order_id       NUMBER(19) PRIMARY KEY,
  order_number   VARCHAR2(50) NOT NULL,
  user_id        NUMBER(19) NOT NULL,
  order_type     VARCHAR2(20) DEFAULT 'WEB',
  subtotal       NUMBER(12,2) NOT NULL,
  shipping_total NUMBER(12,2) DEFAULT 0,
  total          NUMBER(12,2) NOT NULL,
  currency       VARCHAR2(3) DEFAULT 'USD',
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_order_number UNIQUE (order_number),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT chk_order_type CHECK (order_type IN ('WEB','ENTERPRISE_API'))
);

CREATE TABLE order_item (
  order_item_id NUMBER(19) PRIMARY KEY,
  order_id      NUMBER(19) NOT NULL,
  part_id       NUMBER(19) NOT NULL,
  qty           NUMBER(10) NOT NULL,
  unit_price    NUMBER(12,2) NOT NULL,
  line_total    NUMBER(12,2) NOT NULL,
  CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES order_header(order_id),
  CONSTRAINT fk_oi_part FOREIGN KEY (part_id) REFERENCES part(part_id)
);

CREATE TABLE order_status_history (
  status_id          NUMBER(19) PRIMARY KEY,
  order_id           NUMBER(19) NOT NULL,
  status             VARCHAR2(30) NOT NULL,
  comment_text       VARCHAR2(500),
  tracking_number    VARCHAR2(100),
  eta_days           NUMBER(5),
  changed_by_user_id NUMBER(19),
  changed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_osh_order FOREIGN KEY (order_id) REFERENCES order_header(order_id),
  CONSTRAINT fk_osh_user FOREIGN KEY (changed_by_user_id) REFERENCES app_user(user_id),
  CONSTRAINT chk_osh_status CHECK (status IN ('INITIATED','PREPARING','SHIPPED','DELIVERED'))
);

CREATE TABLE payment (
  payment_id     NUMBER(19) PRIMARY KEY,
  order_id       NUMBER(19) NOT NULL,
  method         VARCHAR2(50) DEFAULT 'CARD',
  amount         NUMBER(12,2) NOT NULL,
  auth_reference VARCHAR2(200),
  payment_status VARCHAR2(50),
  created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES order_header(order_id)
);

-- 6) Reviews
CREATE TABLE part_review (
  review_id        NUMBER(19) PRIMARY KEY,
  part_id          NUMBER(19) NOT NULL,
  user_id          NUMBER(19) NOT NULL,
  parent_review_id NUMBER(19),
  rating           NUMBER(1),
  comment_text    CLOB,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT fk_review_parent FOREIGN KEY (parent_review_id) REFERENCES part_review(review_id),
  CONSTRAINT chk_review_rating CHECK (rating BETWEEN 0 AND 5)
);

-- 7) Logs JSON
CREATE TABLE json_operation_log (
  op_id         NUMBER(19) PRIMARY KEY,
  user_id       NUMBER(19) NOT NULL,
  op_type       VARCHAR2(80) NOT NULL,
  file_name     VARCHAR2(500),
  success_count NUMBER(10) DEFAULT 0,
  error_count   NUMBER(10) DEFAULT 0,
  error_detail  CLOB,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_jsonlog_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

-- 8) Reportes mercado
CREATE TABLE market_feed_config (
  feed_id      NUMBER(19) PRIMARY KEY,
  enterprise_id NUMBER(19) NOT NULL,
  endpoint_url VARCHAR2(500) NOT NULL,
  auth_type    VARCHAR2(50),
  auth_token   VARCHAR2(500),
  active       NUMBER(1) DEFAULT 1,
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_feed_enterprise FOREIGN KEY (enterprise_id) REFERENCES enterprise_profile(enterprise_id),
  CONSTRAINT chk_feed_active CHECK (active IN (0,1))
);

CREATE TABLE market_sales_snapshot (
  snapshot_id   NUMBER(19) PRIMARY KEY,
  feed_id       NUMBER(19) NOT NULL,
  snapshot_date DATE NOT NULL,
  part_number   VARCHAR2(100),
  part_name     VARCHAR2(500),
  qty           NUMBER(10),
  unit_price    NUMBER(12,2),
  currency      VARCHAR2(3),
  received_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_snap_feed FOREIGN KEY (feed_id) REFERENCES market_feed_config(feed_id)
);

CREATE TABLE part_number_map (
  map_id              NUMBER(19) PRIMARY KEY,
  part_number_external VARCHAR2(100) NOT NULL,
  part_id             NUMBER(19) NOT NULL,
  feed_id             NUMBER(19),
  CONSTRAINT uq_part_map_ext_feed UNIQUE (part_number_external, feed_id),
  CONSTRAINT fk_map_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_map_feed FOREIGN KEY (feed_id) REFERENCES market_feed_config(feed_id)
);

-- 9) ÍNDICES recomendados
CREATE INDEX idx_part_part_number ON part(part_number);
CREATE INDEX idx_vehicle_code_year ON vehicle(universal_vehicle_code, year_number);
CREATE INDEX idx_part_comp_part_vehicle ON part_compatibility(part_id, vehicle_id);
CREATE INDEX idx_order_header_user_created ON order_header(user_id, created_at);
CREATE INDEX idx_order_status_order_changed ON order_status_history(order_id, changed_at);
CREATE INDEX idx_market_snap_date ON market_sales_snapshot(snapshot_date);
CREATE INDEX idx_market_snap_part ON market_sales_snapshot(part_number);
CREATE INDEX idx_part_review_part ON part_review(part_id);
CREATE INDEX idx_part_review_parent ON part_review(parent_review_id);

-- 10) Datos iniciales (roles) — todos los tipos de usuario
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'ADMIN');
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'REGISTERED');
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'ENTERPRISE');
COMMIT;
