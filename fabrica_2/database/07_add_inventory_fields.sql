-- =====================================================
-- Script: 07_add_inventory_fields.sql
-- Descripción: Agregar campos de inventario a la tabla PART
-- Autor: Sistema
-- Fecha: Febrero 2026
-- =====================================================

-- Conectar como usuario FABRICA
-- @07_add_inventory_fields.sql

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
UPDATE PART SET stock_quantity = 100 WHERE stock_quantity = 0;

COMMIT;

-- Verificar
SELECT part_id, part_number, title, stock_quantity, low_stock_threshold, reserved_quantity
FROM PART
ORDER BY part_id;

PROMPT '✅ Campos de inventario agregados correctamente a PART';
