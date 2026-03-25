-- Añadir api_key a enterprise_profile para usuarios empresariales.
-- Ejecutar como FABRICA si la tabla ya existe sin esta columna.

ALTER TABLE enterprise_profile ADD api_key VARCHAR2(255);
COMMIT;
