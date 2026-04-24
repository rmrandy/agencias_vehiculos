-- Solución al problema de APP_USER con USERID y USER_ID.
-- Ejecutar como FABRICA.

-- OPCIÓN A: Si USERID es la PK real, eliminar USER_ID (columna redundante):
-- ALTER TABLE APP_USER DROP COLUMN USER_ID;
-- COMMIT;

-- OPCIÓN B: Si USER_ID es la PK real, eliminar USERID y cambiar el backend a usar USER_ID:
-- ALTER TABLE APP_USER DROP COLUMN USERID;
-- COMMIT;
-- (y en AppUser.java cambiar @Column(name = "USERID") a @Column(name = "USER_ID"))

-- OPCIÓN C: Si necesitas ambas columnas, crear un trigger para sincronizarlas:
CREATE OR REPLACE TRIGGER trg_app_user_sync_ids
BEFORE INSERT ON APP_USER
FOR EACH ROW
BEGIN
  IF :NEW.USERID IS NOT NULL AND :NEW.USER_ID IS NULL THEN
    :NEW.USER_ID := :NEW.USERID;
  ELSIF :NEW.USER_ID IS NOT NULL AND :NEW.USERID IS NULL THEN
    :NEW.USERID := :NEW.USER_ID;
  END IF;
END;
/

COMMIT;
