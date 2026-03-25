-- Crear usuario/esquema FABRICA2 para el backend fabrica_2.
-- Ejecutar conectado como SYS (u otro DBA) con SYSDBA.
-- Luego ejecutar schema.sql conectado como FABRICA2.

CREATE USER fabrica2 IDENTIFIED BY 123
  DEFAULT TABLESPACE users
  QUOTA UNLIMITED ON users;

GRANT CREATE SESSION TO fabrica2;
GRANT CONNECT, RESOURCE TO fabrica2;
GRANT CREATE VIEW TO fabrica2;
GRANT CREATE SEQUENCE TO fabrica2;

-- Si FABRICA2 ya existe pero da ORA-01950 (no privileges on tablespace USERS), ejecuta como SYS:
-- ALTER USER fabrica2 QUOTA UNLIMITED ON users;

-- Si FABRICA2 ya existe pero da ORA-01045 (lacks CREATE SESSION), ejecuta solo los GRANT como SYS:
-- GRANT CREATE SESSION TO fabrica2;
-- GRANT CONNECT, RESOURCE TO fabrica2;
-- GRANT CREATE VIEW TO fabrica2;
-- GRANT CREATE SEQUENCE TO fabrica2;
