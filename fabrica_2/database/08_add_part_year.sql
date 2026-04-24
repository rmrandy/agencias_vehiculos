-- Año del repuesto (requerimiento DOC)
-- Ejecutar después del schema base y migraciones previas.

ALTER TABLE part ADD (part_year NUMBER(4));
COMMENT ON COLUMN part.part_year IS 'Año del repuesto (ej. compatibilidad o año de referencia)';

-- Opcional: si la tabla se creó como PART en mayúsculas:
-- ALTER TABLE PART ADD (part_year NUMBER(4));
