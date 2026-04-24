-- =====================================================
-- EJECUTAR ESTE SCRIPT EN DBEAVER
-- =====================================================
-- 1. Abre DBeaver
-- 2. Conecta a la base de datos FABRICA
-- 3. Copia y pega este script completo
-- 4. Ejecuta (Ctrl+Enter o botón Execute)
-- =====================================================

-- Agregar campos de inventario a PART
ALTER TABLE PART ADD (
    stock_quantity NUMBER(10) DEFAULT 0 NOT NULL,
    low_stock_threshold NUMBER(10) DEFAULT 5 NOT NULL,
    reserved_quantity NUMBER(10) DEFAULT 0 NOT NULL
);

-- Comentarios en las columnas
COMMENT ON COLUMN PART.stock_quantity IS 'Cantidad disponible en inventario';
COMMENT ON COLUMN PART.low_stock_threshold IS 'Umbral para considerar inventario bajo (default 5)';
COMMENT ON COLUMN PART.reserved_quantity IS 'Cantidad reservada en pedidos pendientes';

-- Actualizar productos existentes con stock inicial (para testing)
UPDATE PART SET stock_quantity = 100;

COMMIT;

-- Verificar que se agregaron las columnas
SELECT 
    part_id, 
    part_number, 
    title, 
    stock_quantity, 
    low_stock_threshold, 
    reserved_quantity,
    (stock_quantity - reserved_quantity) as disponible
FROM PART
ORDER BY part_id;

-- ✅ Si ves la tabla con los nuevos campos, la migración fue exitosa
-- Ahora REINICIA EL BACKEND y recarga el frontend
