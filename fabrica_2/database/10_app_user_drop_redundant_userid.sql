-- =============================================================================
-- APP_USER: eliminar columna USERID duplicada (creada por Hibernate hbm2ddl).
-- El modelo correcto usa solo USER_ID como PK.
--
-- IMPORTANTE: Si ejecutaste antes este script y salió "no tiene columna USERID"
-- pero en DBeaver SÍ ves USERID, es porque la consulta se hizo con otro usuario
-- (p. ej. SYS): USER_TAB_COLUMNS solo mira el esquema de la sesión actual.
--
-- Este script usa siempre el esquema FABRICA2 (nombre calificado).
-- Puedes ejecutarlo conectado como FABRICA2 o como SYS (u otro con ALTER en FABRICA2).
-- =============================================================================

DECLARE
  v_schema   CONSTANT VARCHAR2(30) := 'FABRICA2';
  v_cnt      NUMBER;
  v_sql      VARCHAR2(500);
BEGIN
  -- Trigger de sincronización (si existía en ese esquema)
  BEGIN
    v_sql := 'DROP TRIGGER ' || v_schema || '.trg_app_user_sync_ids';
    EXECUTE IMMEDIATE v_sql;
    DBMS_OUTPUT.PUT_LINE('Trigger trg_app_user_sync_ids eliminado.');
  EXCEPTION
    WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('Trigger: ' || SQLERRM);
  END;

  SELECT COUNT(*) INTO v_cnt
  FROM all_tab_columns
  WHERE owner = v_schema
    AND table_name = 'APP_USER'
    AND column_name = 'USERID';

  IF v_cnt > 0 THEN
    v_sql := 'ALTER TABLE ' || v_schema || '.APP_USER DROP COLUMN USERID';
    EXECUTE IMMEDIATE v_sql;
    DBMS_OUTPUT.PUT_LINE('Columna USERID eliminada de ' || v_schema || '.APP_USER.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('En ' || v_schema || '.APP_USER no existe columna USERID; nada que hacer.');
  END IF;
END;
/

COMMIT;
