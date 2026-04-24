-- Crear usuario/esquema FABRICA para el backend de la fábrica.
-- Ejecutar conectado como SYS (u otro DBA) con SYSDBA.
-- Luego ejecutar schema.sql conectado como FABRICA.

CREATE USER fabrica IDENTIFIED BY 123
  DEFAULT TABLESPACE users
  QUOTA UNLIMITED ON users;

-- Necesario para que la aplicación pueda conectarse (logon).
GRANT CREATE SESSION TO fabrica;
GRANT CONNECT, RESOURCE TO fabrica;
GRANT CREATE VIEW TO fabrica;
GRANT CREATE SEQUENCE TO fabrica;

-- Si usas Oracle 23c+ o necesitas referencias a tablas de otros esquemas:
-- GRANT REFERENCES ON otro_esquema.tabla TO fabrica;

-- Si FABRICA ya existe pero da ORA-01950 (no privileges on tablespace USERS), ejecuta como SYS:
-- ALTER USER fabrica QUOTA UNLIMITED ON users;
-- (y opcionalmente los GRANT de abajo si faltan)

-- Si FABRICA ya existe pero da ORA-01045 (lacks CREATE SESSION), ejecuta solo los GRANT como SYS:
-- GRANT CREATE SESSION TO fabrica;
-- GRANT CONNECT, RESOURCE TO fabrica;
-- GRANT CREATE VIEW TO fabrica;
-- GRANT CREATE SEQUENCE TO fabrica;
