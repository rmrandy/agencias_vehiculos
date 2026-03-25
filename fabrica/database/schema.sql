-- =============================================================================
-- Sistema Fábrica de Repuestos - DDL Oracle (versión consolidada)
-- Incluye cambios que antes estaban en 06, 07, 08 y tablas usadas por JPA
-- (INVENTORY_LOG, IMPORT_EXPORT_LOG, PART_ENGAGEMENT_LOG).
-- Secuencias ORDER_SEQ y STATUS_SEQ alineadas con el backend Java.
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
-- JPA OrderHeader / OrderStatusHistory
CREATE SEQUENCE order_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE order_item_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE status_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_review_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE json_operation_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE market_feed_config_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE market_sales_snapshot_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_number_map_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE inventory_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE import_export_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE part_engagement_log_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

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
  enterprise_id               NUMBER(19) PRIMARY KEY,
  user_id                     NUMBER(19) NOT NULL,
  api_key                     VARCHAR2(255),
  default_shipping_address_id NUMBER(19),
  default_billing_address_id  NUMBER(19),
  default_card_token          VARCHAR2(255),
  default_address_text        VARCHAR2(1000),
  default_card_last4          VARCHAR2(10),
  delivery_window             VARCHAR2(500),
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
  image_data  BLOB,
  image_type  VARCHAR2(50),
  CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category(category_id)
);

CREATE TABLE brand (
  brand_id   NUMBER(19) PRIMARY KEY,
  name       VARCHAR2(200) NOT NULL,
  image_data BLOB,
  image_type VARCHAR2(50)
);

CREATE TABLE vehicle (
  vehicle_id             NUMBER(19) PRIMARY KEY,
  universal_vehicle_code VARCHAR2(100) NOT NULL,
  make                   VARCHAR2(100),
  line                   VARCHAR2(100),
  year_number            NUMBER(4),
  image_data             BLOB,
  image_type             VARCHAR2(50),
  CONSTRAINT uq_vehicle_code_year UNIQUE (universal_vehicle_code, year_number)
);

CREATE TABLE part (
  part_id             NUMBER(19) PRIMARY KEY,
  category_id         NUMBER(19) NOT NULL,
  brand_id            NUMBER(19) NOT NULL,
  part_number         VARCHAR2(100) NOT NULL,
  title               VARCHAR2(500) NOT NULL,
  description         CLOB,
  weight_lb           NUMBER(10,2),
  price               NUMBER(12,2) NOT NULL,
  active              NUMBER(1) DEFAULT 1,
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  image_data          BLOB,
  image_type          VARCHAR2(50),
  stock_quantity      NUMBER(10) DEFAULT 0 NOT NULL,
  low_stock_threshold NUMBER(10) DEFAULT 5 NOT NULL,
  reserved_quantity   NUMBER(10) DEFAULT 0 NOT NULL,
  part_year           NUMBER(4),
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
  CONSTRAINT chk_osh_status CHECK (status IN (
    'INITIATED','CONFIRMED','IN_PREPARATION','PREPARING','SHIPPED','DELIVERED','CANCELLED'
  ))
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

-- 6) Reviews (columna BODY según JPA PartReview)
CREATE TABLE part_review (
  review_id   NUMBER(19) PRIMARY KEY,
  part_id     NUMBER(19) NOT NULL,
  user_id     NUMBER(19) NOT NULL,
  parent_id   NUMBER(19),
  rating      NUMBER(1),
  body        CLOB NOT NULL,
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user(user_id),
  CONSTRAINT fk_review_parent FOREIGN KEY (parent_id) REFERENCES part_review(review_id),
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
  feed_id       NUMBER(19) PRIMARY KEY,
  enterprise_id NUMBER(19) NOT NULL,
  endpoint_url  VARCHAR2(500) NOT NULL,
  auth_type     VARCHAR2(50),
  auth_token    VARCHAR2(500),
  active        NUMBER(1) DEFAULT 1,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
  map_id               NUMBER(19) PRIMARY KEY,
  part_number_external VARCHAR2(100) NOT NULL,
  part_id              NUMBER(19) NOT NULL,
  feed_id              NUMBER(19),
  CONSTRAINT uq_part_map_ext_feed UNIQUE (part_number_external, feed_id),
  CONSTRAINT fk_map_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_map_feed FOREIGN KEY (feed_id) REFERENCES market_feed_config(feed_id)
);

-- 9) Logs de inventario / import-export / engagement (JPA)
CREATE TABLE inventory_log (
  log_id           NUMBER(19) PRIMARY KEY,
  part_id          NUMBER(19) NOT NULL,
  user_id          NUMBER(19) NOT NULL,
  quantity_added   NUMBER(10) NOT NULL,
  previous_quantity NUMBER(10) NOT NULL,
  new_quantity     NUMBER(10) NOT NULL,
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_invlog_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_invlog_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

CREATE TABLE import_export_log (
  log_id        NUMBER(19) PRIMARY KEY,
  user_id       NUMBER(19) NOT NULL,
  operation     VARCHAR2(50) NOT NULL,
  file_name     VARCHAR2(500),
  success_count NUMBER(10) DEFAULT 0 NOT NULL,
  error_count   NUMBER(10) DEFAULT 0 NOT NULL,
  detail        CLOB,
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_iexlog_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

CREATE TABLE part_engagement_log (
  log_id     NUMBER(19) PRIMARY KEY,
  event_type VARCHAR2(30) NOT NULL,
  part_id    NUMBER(19) NOT NULL,
  user_id    NUMBER(19),
  client_type VARCHAR2(20),
  source     VARCHAR2(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_englog_part FOREIGN KEY (part_id) REFERENCES part(part_id),
  CONSTRAINT fk_englog_user FOREIGN KEY (user_id) REFERENCES app_user(user_id)
);

-- 10) ÍndICES
CREATE INDEX idx_part_part_number ON part(part_number);
CREATE INDEX idx_vehicle_code_year ON vehicle(universal_vehicle_code, year_number);
CREATE INDEX idx_part_comp_part_vehicle ON part_compatibility(part_id, vehicle_id);
CREATE INDEX idx_order_header_user_created ON order_header(user_id, created_at);
CREATE INDEX idx_order_status_order_changed ON order_status_history(order_id, changed_at);
CREATE INDEX idx_market_snap_date ON market_sales_snapshot(snapshot_date);
CREATE INDEX idx_market_snap_part ON market_sales_snapshot(part_number);
CREATE INDEX idx_part_review_part ON part_review(part_id);
CREATE INDEX idx_part_review_parent ON part_review(parent_id);
CREATE INDEX idx_inventory_log_part ON inventory_log(part_id);
CREATE INDEX idx_import_export_log_user ON import_export_log(user_id);
CREATE INDEX idx_part_engagement_part ON part_engagement_log(part_id);
CREATE INDEX idx_part_engagement_created ON part_engagement_log(created_at);

-- 11) Datos iniciales (roles)
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'ADMIN');
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'REGISTERED');
INSERT INTO role (role_id, name) VALUES (role_seq.NEXTVAL, 'ENTERPRISE');
COMMIT;

-- =============================================================================
-- Notas
-- - APP_USER: el modelo JPA mapea la PK como USERID; este DDL usa USER_ID.
--   Si Hibernate falla, ejecute database/04_fix_app_user_columns.sql o alinee
--   @Column en AppUser.java a USER_ID.
-- - Migraciones 06, 07, 08 ya están integradas aquí; no hace falta ejecutarlas
--   en bases creadas desde cero con este archivo.
-- - Scripts antiguos que usaban order_header_seq / order_status_history_seq deben
--   usar order_seq / status_seq (alineado con ORDER_SEQ y STATUS_SEQ en JPA).
-- =============================================================================
