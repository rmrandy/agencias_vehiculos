-- =============================================================================
-- Datos dummy completos para TODO el esquema de la Fábrica
-- Para probar múltiples compras y flujos completos.
-- Ejecutar como FABRICA después de schema base y migraciones (01-08).
--
-- Nota: Usa USER_ID (schema estándar). Si tu BD tiene USERID como PK,
--       busca y reemplaza USER_ID por USERID en APP_USER.
-- Nota: PART_REVIEW usa BODY (schema consolidado y JPA PartReview).
-- =============================================================================

-- Crear secuencias faltantes (si Hibernate/JPA las usa con otros nombres)
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE ORDER_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;  -- 955 = name already used
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE STATUS_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE INVENTORY_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE IMPORT_EXPORT_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'CREATE SEQUENCE PART_ENGAGEMENT_LOG_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- =============================================================================
-- 1. ROLES (si no existen)
-- =============================================================================
INSERT INTO ROLE (ROLE_ID, NAME) SELECT ROLE_SEQ.NEXTVAL, 'ADMIN' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE WHERE NAME = 'ADMIN');
INSERT INTO ROLE (ROLE_ID, NAME) SELECT ROLE_SEQ.NEXTVAL, 'REGISTERED' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE WHERE NAME = 'REGISTERED');
INSERT INTO ROLE (ROLE_ID, NAME) SELECT ROLE_SEQ.NEXTVAL, 'ENTERPRISE' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ROLE WHERE NAME = 'ENTERPRISE');
COMMIT;

-- =============================================================================
-- 2. APP_USER - Usuarios (particulares, empresariales, admin)
-- Password hash para "123456" (BCrypt)
-- Usa USER_ID (schema estándar); si tu BD tiene USERID, cambia el nombre de columna.
-- =============================================================================
DECLARE
  v_admin_id NUMBER;
  v_reg_id   NUMBER;
  v_ent_id   NUMBER;
BEGIN
  SELECT ROLE_ID INTO v_admin_id FROM ROLE WHERE NAME = 'ADMIN' AND ROWNUM = 1;
  SELECT ROLE_ID INTO v_reg_id   FROM ROLE WHERE NAME = 'REGISTERED' AND ROWNUM = 1;
  SELECT ROLE_ID INTO v_ent_id   FROM ROLE WHERE NAME = 'ENTERPRISE' AND ROWNUM = 1;

  -- Admin
  INSERT INTO APP_USER (USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE, STATUS, CREATED_AT)
  SELECT APP_USER_SEQ.NEXTVAL, 'admin@fabrica.local', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqOIdFw.7QlH.HqxqVqjQ7dKdKGvSi', 'Administrador', NULL, 'ACTIVE', SYSDATE FROM DUAL
  WHERE NOT EXISTS (SELECT 1 FROM APP_USER WHERE EMAIL = 'admin@fabrica.local');

  -- Usuarios particulares (para múltiples compras)
  FOR i IN 1..15 LOOP
    INSERT INTO APP_USER (USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE, STATUS, CREATED_AT)
    SELECT APP_USER_SEQ.NEXTVAL, 'cliente'||i||'@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqOIdFw.7QlH.HqxqVqjQ7dKdKGvSi', 'Cliente '||i, '555-'||LPAD(i,4,'0'), 'ACTIVE', SYSDATE FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM APP_USER WHERE EMAIL = 'cliente'||i||'@test.com');
  END LOOP;

  -- Usuarios empresariales
  FOR i IN 1..5 LOOP
    INSERT INTO APP_USER (USER_ID, EMAIL, PASSWORD_HASH, FULL_NAME, PHONE, STATUS, CREATED_AT)
    SELECT APP_USER_SEQ.NEXTVAL, 'empresa'||i||'@corp.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqOIdFw.7QlH.HqxqVqjQ7dKdKGvSi', 'Empresa '||i, '555-9'||LPAD(i,3,'0'), 'ACTIVE', SYSDATE FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM APP_USER WHERE EMAIL = 'empresa'||i||'@corp.com');
  END LOOP;

  COMMIT;
END;
/

-- USER_ROLE (asignar roles)
DECLARE
  v_admin_rid NUMBER;
  v_reg_rid   NUMBER;
  v_ent_rid   NUMBER;
BEGIN
  SELECT ROLE_ID INTO v_admin_rid FROM ROLE WHERE NAME = 'ADMIN' AND ROWNUM = 1;
  SELECT ROLE_ID INTO v_reg_rid   FROM ROLE WHERE NAME = 'REGISTERED' AND ROWNUM = 1;
  SELECT ROLE_ID INTO v_ent_rid   FROM ROLE WHERE NAME = 'ENTERPRISE' AND ROWNUM = 1;

  FOR r IN (SELECT USER_ID uid FROM APP_USER WHERE EMAIL = 'admin@fabrica.local') LOOP
    INSERT INTO USER_ROLE (USER_ID, ROLE_ID) SELECT r.uid, v_admin_rid FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USER_ROLE WHERE USER_ID = r.uid AND ROLE_ID = v_admin_rid);
    INSERT INTO USER_ROLE (USER_ID, ROLE_ID) SELECT r.uid, v_reg_rid FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USER_ROLE WHERE USER_ID = r.uid AND ROLE_ID = v_reg_rid);
  END LOOP;

  FOR r IN (SELECT USER_ID uid FROM APP_USER u WHERE u.EMAIL LIKE 'cliente%@test.com' AND NOT EXISTS (SELECT 1 FROM USER_ROLE ur WHERE ur.USER_ID = u.USER_ID)) LOOP
    INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (r.uid, v_reg_rid);
  END LOOP;

  FOR r IN (SELECT USER_ID uid FROM APP_USER u WHERE u.EMAIL LIKE 'empresa%@corp.com' AND NOT EXISTS (SELECT 1 FROM USER_ROLE ur WHERE ur.USER_ID = u.USER_ID)) LOOP
    INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (r.uid, v_ent_rid);
    INSERT INTO USER_ROLE (USER_ID, ROLE_ID) SELECT r.uid, v_reg_rid FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM USER_ROLE WHERE USER_ID = r.uid AND ROLE_ID = v_reg_rid);
  END LOOP;
  COMMIT;
END;
/

-- =============================================================================
-- 3. USER_ADDRESS - Direcciones de envío
-- =============================================================================
INSERT INTO USER_ADDRESS (ADDRESS_ID, USER_ID, LINE1, LINE2, CITY, STATE, ZIP, COUNTRY, IS_DEFAULT_SHIPPING, IS_DEFAULT_BILLING)
SELECT USER_ADDRESS_SEQ.NEXTVAL, u.USER_ID, 'Calle Principal '||u.USER_ID, 'Edificio A', 'Guatemala', 'Guatemala', '01001', 'GT', 1, 1
FROM APP_USER u
WHERE u.EMAIL LIKE 'cliente%@test.com'
  AND NOT EXISTS (SELECT 1 FROM USER_ADDRESS a WHERE a.USER_ID = u.USER_ID);

INSERT INTO USER_ADDRESS (ADDRESS_ID, USER_ID, LINE1, LINE2, CITY, STATE, ZIP, COUNTRY, IS_DEFAULT_SHIPPING, IS_DEFAULT_BILLING)
SELECT USER_ADDRESS_SEQ.NEXTVAL, u.USER_ID, 'Av. Empresarial '||u.USER_ID, 'Bodega 1', 'Guatemala', 'Guatemala', '01010', 'GT', 1, 1
FROM APP_USER u
WHERE u.EMAIL LIKE 'empresa%@corp.com'
  AND NOT EXISTS (SELECT 1 FROM USER_ADDRESS a WHERE a.USER_ID = u.USER_ID);

COMMIT;

-- =============================================================================
-- 4. ENTERPRISE_PROFILE - Perfiles empresariales
-- =============================================================================
INSERT INTO ENTERPRISE_PROFILE (ENTERPRISE_ID, USER_ID, API_KEY, DISCOUNT_PERCENT, DELIVERY_WINDOW)
SELECT ENTERPRISE_PROFILE_SEQ.NEXTVAL, u.USER_ID, 'api-key-emp-'||u.USER_ID, 5 + (MOD(u.USER_ID, 10)), 'Lun-Vie 8:00-18:00'
FROM APP_USER u
WHERE u.EMAIL LIKE 'empresa%@corp.com'
  AND NOT EXISTS (SELECT 1 FROM ENTERPRISE_PROFILE ep WHERE ep.USER_ID = u.USER_ID);
COMMIT;

-- =============================================================================
-- 5. CATEGORY - Categorías
-- =============================================================================
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Motor', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Motor');
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Transmisión', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Transmisión');
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Frenos', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Frenos');
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Suspensión', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Suspensión');
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Eléctrico', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Eléctrico');
INSERT INTO CATEGORY (CATEGORY_ID, NAME, PARENT_ID) SELECT CATEGORY_SEQ.NEXTVAL, 'Filtros', NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Filtros');
COMMIT;

-- =============================================================================
-- 6. BRAND - Marcas
-- =============================================================================
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'Bosch' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'Bosch');
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'Denso' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'Denso');
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'NGK' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'NGK');
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'Brembo' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'Brembo');
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'Mann Filter' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'Mann Filter');
INSERT INTO BRAND (BRAND_ID, NAME) SELECT BRAND_SEQ.NEXTVAL, 'Monroe' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM BRAND WHERE NAME = 'Monroe');
COMMIT;

-- =============================================================================
-- 7. VEHICLE - Vehículos
-- =============================================================================
INSERT INTO VEHICLE (VEHICLE_ID, UNIVERSAL_VEHICLE_CODE, MAKE, LINE, YEAR_NUMBER)
SELECT VEHICLE_SEQ.NEXTVAL, 'UVC-TOY-CAM-'||v.y, 'Toyota', 'Camry', v.y FROM (SELECT 2018 + LEVEL y FROM DUAL CONNECT BY LEVEL <= 5) v
WHERE NOT EXISTS (SELECT 1 FROM VEHICLE WHERE UNIVERSAL_VEHICLE_CODE = 'UVC-TOY-CAM-'||v.y AND YEAR_NUMBER = v.y);

INSERT INTO VEHICLE (VEHICLE_ID, UNIVERSAL_VEHICLE_CODE, MAKE, LINE, YEAR_NUMBER)
SELECT VEHICLE_SEQ.NEXTVAL, 'UVC-HON-CIV-'||v.y, 'Honda', 'Civic', v.y FROM (SELECT 2017 + LEVEL y FROM DUAL CONNECT BY LEVEL <= 6) v
WHERE NOT EXISTS (SELECT 1 FROM VEHICLE WHERE UNIVERSAL_VEHICLE_CODE = 'UVC-HON-CIV-'||v.y AND YEAR_NUMBER = v.y);

INSERT INTO VEHICLE (VEHICLE_ID, UNIVERSAL_VEHICLE_CODE, MAKE, LINE, YEAR_NUMBER)
SELECT VEHICLE_SEQ.NEXTVAL, 'UVC-FOR-F150-'||v.y, 'Ford', 'F-150', v.y FROM (SELECT 2019 + LEVEL y FROM DUAL CONNECT BY LEVEL <= 4) v
WHERE NOT EXISTS (SELECT 1 FROM VEHICLE WHERE UNIVERSAL_VEHICLE_CODE = 'UVC-FOR-F150-'||v.y AND YEAR_NUMBER = v.y);

COMMIT;

-- =============================================================================
-- 8. PART - Repuestos (muchos para probar compras)
-- Asume categorías 1-6 y marcas 1-6. Ajustar IDs si tus secuencias empezaron en otro valor.
-- =============================================================================
DECLARE
  v_cat_filtros NUMBER; v_cat_frenos NUMBER; v_cat_elec NUMBER; v_cat_susp NUMBER; v_cat_motor NUMBER;
  v_br_bosch NUMBER; v_br_denso NUMBER; v_br_ngk NUMBER; v_br_brembo NUMBER; v_br_mann NUMBER; v_br_monroe NUMBER;
BEGIN
  SELECT CATEGORY_ID INTO v_cat_filtros FROM CATEGORY WHERE NAME = 'Filtros' AND ROWNUM = 1;
  SELECT CATEGORY_ID INTO v_cat_frenos FROM CATEGORY WHERE NAME = 'Frenos' AND ROWNUM = 1;
  SELECT CATEGORY_ID INTO v_cat_elec  FROM CATEGORY WHERE NAME = 'Eléctrico' AND ROWNUM = 1;
  SELECT CATEGORY_ID INTO v_cat_susp  FROM CATEGORY WHERE NAME = 'Suspensión' AND ROWNUM = 1;
  SELECT CATEGORY_ID INTO v_cat_motor FROM CATEGORY WHERE NAME = 'Motor' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_bosch FROM BRAND WHERE NAME = 'Bosch' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_denso FROM BRAND WHERE NAME = 'Denso' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_ngk   FROM BRAND WHERE NAME = 'NGK' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_brembo FROM BRAND WHERE NAME = 'Brembo' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_mann  FROM BRAND WHERE NAME = 'Mann Filter' AND ROWNUM = 1;
  SELECT BRAND_ID INTO v_br_monroe FROM BRAND WHERE NAME = 'Monroe' AND ROWNUM = 1;

  -- Repuestos base (los de 05_datos_dummy)
  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_filtros, v_br_mann, 'MF-OIL-001', 'Filtro de aceite Mann Filter', 'Filtro de aceite alta eficiencia', 0.5, 15.99, 1, SYSDATE, 200, 5, 0, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'MF-OIL-001');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_frenos, v_br_brembo, 'BRE-PAD-F200', 'Pastillas de freno Brembo', 'Pastillas cerámicas alto rendimiento', 2.3, 89.99, 1, SYSDATE, 150, 5, 0, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'BRE-PAD-F200');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_elec, v_br_ngk, 'NGK-SP-V4', 'Bujías NGK V-Power (4)', 'Bujías alto rendimiento', 0.8, 32.50, 1, SYSDATE, 300, 5, 0, 2019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'NGK-SP-V4');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_susp, v_br_monroe, 'MON-SHOCK-58620', 'Amortiguador Monroe', 'Amortiguador gas alta presión', 5.2, 125.00, 1, SYSDATE, 80, 5, 0, 2021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'MON-SHOCK-58620');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_elec, v_br_bosch, 'BSH-ALT-12V-90A', 'Alternador Bosch 12V 90A', 'Alternador remanufacturado', 12.5, 245.00, 1, SYSDATE, 60, 5, 0, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'BSH-ALT-12V-90A');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_elec, v_br_denso, 'DEN-O2-234-4668', 'Sensor oxígeno Denso', 'Sensor O2 de 4 cables', 0.6, 67.50, 1, SYSDATE, 120, 5, 0, 2019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'DEN-O2-234-4668');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_filtros, v_br_mann, 'MF-AIR-C25114', 'Filtro de aire Mann', 'Filtro aire papel alta eficiencia', 0.9, 22.99, 1, SYSDATE, 180, 5, 0, 2020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'MF-AIR-C25114');

  INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
  SELECT PART_SEQ.NEXTVAL, v_cat_frenos, v_br_brembo, 'BRE-DISC-09C84811', 'Discos freno Brembo', 'Par discos ventilados', 18.5, 189.99, 1, SYSDATE, 90, 5, 0, 2021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'BRE-DISC-09C84811');

  -- Más repuestos para variedad
  FOR i IN 1..20 LOOP
    INSERT INTO PART (PART_ID, CATEGORY_ID, BRAND_ID, PART_NUMBER, TITLE, DESCRIPTION, WEIGHT_LB, PRICE, ACTIVE, CREATED_AT, STOCK_QUANTITY, LOW_STOCK_THRESHOLD, RESERVED_QUANTITY, PART_YEAR)
    SELECT PART_SEQ.NEXTVAL,
           CASE MOD(i,6) WHEN 0 THEN v_cat_filtros WHEN 1 THEN v_cat_frenos WHEN 2 THEN v_cat_elec WHEN 3 THEN v_cat_susp WHEN 4 THEN v_cat_motor ELSE v_cat_filtros END,
           CASE MOD(i,6) WHEN 0 THEN v_br_mann WHEN 1 THEN v_br_brembo WHEN 2 THEN v_br_ngk WHEN 3 THEN v_br_monroe WHEN 4 THEN v_br_bosch ELSE v_br_denso END,
           'EXT-PART-'||LPAD(i,4,'0'), 'Repuesto adicional '||i, 'Descripción repuesto '||i, 1 + MOD(i, 5), 25 + (i * 3.5), 1, SYSDATE, 100 + i*5, 5, 0, 2019 + MOD(i,4)
    FROM DUAL
    WHERE NOT EXISTS (SELECT 1 FROM PART WHERE PART_NUMBER = 'EXT-PART-'||LPAD(i,4,'0'));
  END LOOP;

  COMMIT;
END;
/

-- =============================================================================
-- 9. PART_COMPATIBILITY - Compatibilidad parte-vehículo (hasta 50 pares)
-- =============================================================================
DECLARE
  v_count NUMBER := 0;
BEGIN
  FOR p_rec IN (SELECT PART_ID FROM (SELECT PART_ID FROM PART ORDER BY PART_ID) WHERE ROWNUM <= 10) LOOP
    FOR v_rec IN (SELECT VEHICLE_ID FROM (SELECT VEHICLE_ID FROM VEHICLE ORDER BY VEHICLE_ID) WHERE ROWNUM <= 5) LOOP
      BEGIN
        INSERT INTO PART_COMPATIBILITY (PART_ID, VEHICLE_ID) VALUES (p_rec.PART_ID, v_rec.VEHICLE_ID);
        v_count := v_count + 1;
      EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
      END;
      IF v_count >= 50 THEN EXIT; END IF;
    END LOOP;
    IF v_count >= 50 THEN EXIT; END IF;
  END LOOP;
  COMMIT;
END;
/

-- =============================================================================
-- 10. ORDER_HEADER + ORDER_ITEM - Múltiples pedidos
-- =============================================================================
DECLARE
  v_user_id  NUMBER;
  v_part_id  NUMBER;
  v_price    NUMBER;
  v_order_id NUMBER;
  v_order_num VARCHAR2(50);
  v_subtotal NUMBER;
  v_total    NUMBER;
  v_qty      NUMBER;
  v_count    NUMBER := 0;
  v_ord_type VARCHAR2(20);
BEGIN
  FOR u_rec IN (SELECT USER_ID uid FROM APP_USER WHERE EMAIL LIKE 'cliente%@test.com' OR EMAIL LIKE 'empresa%@corp.com') LOOP
    v_user_id := u_rec.uid;
    v_ord_type := CASE WHEN EXISTS (SELECT 1 FROM ENTERPRISE_PROFILE WHERE USER_ID = v_user_id) THEN 'ENTERPRISE_API' ELSE 'WEB' END;

    FOR ord_num IN 1..4 LOOP
      v_order_num := 'ORD-'||TO_CHAR(SYSDATE,'YYYYMMDD')||'-'||LPAD(v_count,6,'0');
      v_subtotal := 0;

      SELECT order_seq.NEXTVAL INTO v_order_id FROM DUAL;
      INSERT INTO ORDER_HEADER (ORDER_ID, ORDER_NUMBER, USER_ID, ORDER_TYPE, SUBTOTAL, SHIPPING_TOTAL, TOTAL, CURRENCY, CREATED_AT)
      VALUES (v_order_id, v_order_num, v_user_id, v_ord_type, 0, 0, 0, 'USD', SYSDATE - ord_num);

      FOR oi IN 1..(2 + MOD(v_count, 4)) LOOP
        SELECT PART_ID, PRICE INTO v_part_id, v_price
        FROM (SELECT PART_ID, PRICE FROM PART WHERE ACTIVE = 1 ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM = 1;
        v_qty := 1 + MOD(v_count + oi, 3);
        v_subtotal := v_subtotal + (v_price * v_qty);
        INSERT INTO ORDER_ITEM (ORDER_ITEM_ID, ORDER_ID, PART_ID, QTY, UNIT_PRICE, LINE_TOTAL)
        VALUES (order_item_seq.NEXTVAL, v_order_id, v_part_id, v_qty, v_price, v_price * v_qty);
      END LOOP;

      v_total := v_subtotal + 5.99;
      UPDATE ORDER_HEADER SET SUBTOTAL = v_subtotal, SHIPPING_TOTAL = 5.99, TOTAL = v_total WHERE ORDER_ID = v_order_id;

      INSERT INTO ORDER_STATUS_HISTORY (STATUS_ID, ORDER_ID, STATUS, COMMENT_TEXT, CHANGED_BY_USER_ID, CHANGED_AT)
      VALUES (status_seq.NEXTVAL, v_order_id,
              CASE WHEN ord_num = 1 THEN 'DELIVERED' WHEN ord_num = 2 THEN 'SHIPPED' WHEN ord_num = 3 THEN 'PREPARING' ELSE 'INITIATED' END,
              'Estado inicial', v_user_id, SYSDATE - ord_num);

      v_count := v_count + 1;
    END LOOP;
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- Si ORDER_SEQ/STATUS_SEQ usan secuencias distintas, intentar con ORDER_HEADER_SEQ
-- Ajustar si tu esquema usa ORDER_HEADER_SEQ para ORDER_HEADER.ORDER_ID

-- =============================================================================
-- 11. PAYMENT - Pagos asociados a pedidos
-- =============================================================================
INSERT INTO PAYMENT (PAYMENT_ID, ORDER_ID, METHOD, AMOUNT, AUTH_REFERENCE, PAYMENT_STATUS, CREATED_AT)
SELECT payment_seq.NEXTVAL, oh.ORDER_ID, 'CARD', oh.TOTAL, 'AUTH-'||oh.ORDER_ID, 'COMPLETED', oh.CREATED_AT
FROM (SELECT ORDER_ID, TOTAL, CREATED_AT FROM ORDER_HEADER WHERE ROWNUM <= 30) oh
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT p WHERE p.ORDER_ID = oh.ORDER_ID);
COMMIT;

-- =============================================================================
-- 12. PART_REVIEW - Comentarios y ratings
-- =============================================================================
DECLARE
  v_part NUMBER;
  v_user NUMBER;
  v_rating NUMBER;
  v_body VARCHAR2(200);
BEGIN
  FOR i IN 1..40 LOOP
    SELECT PART_ID INTO v_part FROM (SELECT PART_ID FROM PART ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM = 1;
    SELECT USER_ID INTO v_user FROM (SELECT USER_ID FROM APP_USER WHERE EMAIL LIKE 'cliente%@test.com' ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM = 1;
    v_rating := 1 + MOD(i, 5);
    v_body := 'Excelente repuesto, muy recomendado. Compra '||i;
    BEGIN
      INSERT INTO PART_REVIEW (REVIEW_ID, PART_ID, USER_ID, PARENT_ID, RATING, BODY, CREATED_AT)
      VALUES (PART_REVIEW_SEQ.NEXTVAL, v_part, v_user, NULL, v_rating, v_body, SYSDATE - MOD(i, 30));
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN NULL;
    END;
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 13. INVENTORY_MOVEMENT - Movimientos de inventario
-- =============================================================================
DECLARE
  v_admin NUMBER;
BEGIN
  SELECT USER_ID INTO v_admin FROM APP_USER WHERE EMAIL = 'admin@fabrica.local' AND ROWNUM = 1;
  FOR p_rec IN (SELECT PART_ID, STOCK_QUANTITY FROM PART WHERE ROWNUM <= 15) LOOP
    INSERT INTO INVENTORY_MOVEMENT (MOVEMENT_ID, PART_ID, USER_ID, MOVEMENT_TYPE, QUANTITY, REFERENCE, CREATED_AT)
    VALUES (inventory_movement_seq.NEXTVAL, p_rec.PART_ID, v_admin, 'INBOUND', p_rec.STOCK_QUANTITY, 'Carga inicial dummy', SYSDATE - 7);
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 14. PART_STOCK - Nivel de stock (si la tabla existe)
-- =============================================================================
BEGIN
  MERGE INTO PART_STOCK ps
  USING (SELECT PART_ID, STOCK_QUANTITY, RESERVED_QUANTITY FROM PART) p
  ON (ps.PART_ID = p.PART_ID)
  WHEN NOT MATCHED THEN
    INSERT (PART_ID, ON_HAND_QTY, RESERVED_QTY, UPDATED_AT)
    VALUES (p.PART_ID, p.STOCK_QUANTITY, p.RESERVED_QUANTITY, SYSDATE);
  COMMIT;
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 THEN ROLLBACK; RAISE; END IF;  -- 942 = table does not exist
  ROLLBACK;
END;
/

-- =============================================================================
-- 15. JSON_OPERATION_LOG - Logs de operaciones
-- =============================================================================
DECLARE
  v_admin NUMBER;
BEGIN
  SELECT USER_ID INTO v_admin FROM APP_USER WHERE EMAIL = 'admin@fabrica.local' AND ROWNUM = 1;
  FOR i IN 1..10 LOOP
    INSERT INTO JSON_OPERATION_LOG (OP_ID, USER_ID, OP_TYPE, FILE_NAME, SUCCESS_COUNT, ERROR_COUNT, ERROR_DETAIL, CREATED_AT)
    VALUES (json_operation_log_seq.NEXTVAL, v_admin, CASE MOD(i,3) WHEN 0 THEN 'EXPORT_PARTS' WHEN 1 THEN 'IMPORT_PARTS' ELSE 'BULK_INVENTORY_LOAD' END,
            'dummy_export_'||i||'.json', 50 + i*10, MOD(i, 3), NULL, SYSDATE - i);
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE != -942 AND SQLCODE != -2289 THEN ROLLBACK; RAISE; END IF;
  ROLLBACK;
END;
/

-- =============================================================================
-- 16. IMPORT_EXPORT_LOG
-- =============================================================================
DECLARE
  v_admin NUMBER;
BEGIN
  SELECT USER_ID INTO v_admin FROM APP_USER WHERE EMAIL = 'admin@fabrica.local' AND ROWNUM = 1;
  FOR i IN 1..8 LOOP
    INSERT INTO IMPORT_EXPORT_LOG (LOG_ID, USER_ID, OPERATION, FILE_NAME, SUCCESS_COUNT, ERROR_COUNT, DETAIL, CREATED_AT)
    VALUES (IMPORT_EXPORT_LOG_SEQ.NEXTVAL, v_admin, CASE MOD(i,3) WHEN 0 THEN 'EXPORT' WHEN 1 THEN 'IMPORT' ELSE 'IMPORT_INVENTORY' END,
            'data_'||i||'.json', 100 - i*5, i, NULL, SYSDATE - i);
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 17. INVENTORY_LOG
-- =============================================================================
DECLARE
  v_admin NUMBER;
BEGIN
  SELECT USER_ID INTO v_admin FROM APP_USER WHERE EMAIL = 'admin@fabrica.local' AND ROWNUM = 1;
  FOR p_rec IN (SELECT PART_ID, STOCK_QUANTITY FROM PART WHERE ROWNUM <= 10) LOOP
    INSERT INTO INVENTORY_LOG (LOG_ID, PART_ID, USER_ID, QUANTITY_ADDED, PREVIOUS_QUANTITY, NEW_QUANTITY, CREATED_AT)
    VALUES (INVENTORY_LOG_SEQ.NEXTVAL, p_rec.PART_ID, v_admin, 50, 0, 50, SYSDATE - 5);
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 18. PART_ENGAGEMENT_LOG - Eventos de engagement
-- =============================================================================
DECLARE
  v_part NUMBER;
  v_user NUMBER;
  v_evt VARCHAR2(30);
BEGIN
  FOR i IN 1..50 LOOP
    SELECT PART_ID INTO v_part FROM (SELECT PART_ID FROM PART ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM = 1;
    SELECT USER_ID INTO v_user FROM (SELECT USER_ID FROM APP_USER WHERE EMAIL LIKE 'cliente%@test.com' ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM = 1;
    v_evt := CASE MOD(i,3) WHEN 0 THEN 'VIEW_DETAIL' WHEN 1 THEN 'ADD_TO_CART' ELSE 'SEARCH' END;
    INSERT INTO PART_ENGAGEMENT_LOG (LOG_ID, EVENT_TYPE, PART_ID, USER_ID, CLIENT_TYPE, SOURCE, CREATED_AT)
    VALUES (PART_ENGAGEMENT_LOG_SEQ.NEXTVAL, v_evt, v_part, v_user, 'PARTICULAR', 'web', SYSDATE - MOD(i, 14));
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 19. MARKET_FEED_CONFIG + MARKET_SALES_SNAPSHOT
-- =============================================================================
DECLARE
  v_feed_id NUMBER;
BEGIN
  FOR ep_rec IN (SELECT ENTERPRISE_ID FROM ENTERPRISE_PROFILE WHERE ROWNUM <= 3) LOOP
    INSERT INTO MARKET_FEED_CONFIG (FEED_ID, ENTERPRISE_ID, ENDPOINT_URL, AUTH_TYPE, ACTIVE, CREATED_AT)
    VALUES (market_feed_config_seq.NEXTVAL, ep_rec.ENTERPRISE_ID, 'https://api.empresa'||ep_rec.ENTERPRISE_ID||'.com/ventas', 'Bearer', 1, SYSDATE);
    v_feed_id := market_feed_config_seq.CURRVAL;

    FOR d IN 1..5 LOOP
      INSERT INTO MARKET_SALES_SNAPSHOT (SNAPSHOT_ID, FEED_ID, SNAPSHOT_DATE, PART_NUMBER, PART_NAME, QTY, UNIT_PRICE, CURRENCY, RECEIVED_AT)
      VALUES (market_sales_snapshot_seq.NEXTVAL, v_feed_id, TRUNC(SYSDATE) - d, 'MF-OIL-001', 'Filtro aceite', 10 + d, 15.99, 'USD', SYSDATE - d);
    END LOOP;
  END LOOP;
  COMMIT;
EXCEPTION WHEN OTHERS THEN ROLLBACK; RAISE;
END;
/

-- =============================================================================
-- 20. PART_NUMBER_MAP (mapeo externo)
-- =============================================================================
INSERT INTO PART_NUMBER_MAP (MAP_ID, PART_NUMBER_EXTERNAL, PART_ID, FEED_ID)
SELECT part_number_map_seq.NEXTVAL, 'EXT-'||p.PART_NUMBER, p.PART_ID, mfc.FEED_ID
FROM PART p, MARKET_FEED_CONFIG mfc
WHERE ROWNUM <= 15
  AND NOT EXISTS (SELECT 1 FROM PART_NUMBER_MAP pnm WHERE pnm.PART_ID = p.PART_ID AND pnm.FEED_ID = mfc.FEED_ID);
COMMIT;

-- =============================================================================
-- Verificación rápida
-- =============================================================================
PROMPT '=== Resumen de datos insertados ===';
SELECT 'APP_USER' t, COUNT(*) c FROM APP_USER
UNION ALL SELECT 'ORDER_HEADER', COUNT(*) FROM ORDER_HEADER
UNION ALL SELECT 'ORDER_ITEM', COUNT(*) FROM ORDER_ITEM
UNION ALL SELECT 'PART', COUNT(*) FROM PART
UNION ALL SELECT 'PART_REVIEW', COUNT(*) FROM PART_REVIEW
UNION ALL SELECT 'PART_ENGAGEMENT_LOG', COUNT(*) FROM PART_ENGAGEMENT_LOG;

PROMPT 'Datos dummy completos insertados. Listo para probar múltiples compras.';
