-- Conceder a FABRICA todos los permisos necesarios (cuota en USERS + roles).
-- Ejecutar SIEMPRE como SYS (o otro DBA) con SYSDBA.
-- Úsalo si obtienes ORA-01950 (no privileges on tablespace USERS) u ORA-01045 (CREATE SESSION).

-- 1) Cuota en el tablespace USERS (evita ORA-01950)
ALTER USER fabrica QUOTA UNLIMITED ON users;

-- 2) Conexión y objetos (evita ORA-01045 y permite crear tablas/secuencias/vistas)
GRANT CREATE SESSION TO fabrica;
GRANT CONNECT TO fabrica;
GRANT RESOURCE TO fabrica;
GRANT CREATE VIEW TO fabrica;
GRANT CREATE SEQUENCE TO fabrica;

COMMIT;
