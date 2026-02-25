-- Todos los tipos de usuario/roles del sistema.
-- Ejecutar como FABRICA. Tabla ROLE con columnas: ROLE_ID, NAME, ROLEID.
-- Rellenamos ROLE_ID, ROLEID y NAME (mismo valor de secuencia para los dos IDs).

-- Si la secuencia no existe: CREATE SEQUENCE role_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
-- CURRVAL devuelve el mismo valor que NEXTVAL en la misma sentencia (mismo valor para role_id y roleid).

INSERT INTO role (role_id, roleid, name)
SELECT role_seq.NEXTVAL, role_seq.CURRVAL, 'ADMIN' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ADMIN');

INSERT INTO role (role_id, roleid, name)
SELECT role_seq.NEXTVAL, role_seq.CURRVAL, 'REGISTERED' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'REGISTERED');

INSERT INTO role (role_id, roleid, name)
SELECT role_seq.NEXTVAL, role_seq.CURRVAL, 'ENTERPRISE' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ENTERPRISE');

COMMIT;
