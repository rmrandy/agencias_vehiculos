-- ============================================================================
-- Agregar soporte para imágenes en las entidades del catálogo
-- Las imágenes se almacenan como BLOB directamente en la base de datos
-- ============================================================================

-- Agregar columnas de imagen a PART (repuestos)
ALTER TABLE part ADD image_data BLOB;
ALTER TABLE part ADD image_type VARCHAR2(50);

-- Agregar columnas de imagen a CATEGORY (categorías)
ALTER TABLE category ADD image_data BLOB;
ALTER TABLE category ADD image_type VARCHAR2(50);

-- Agregar columnas de imagen a BRAND (marcas)
ALTER TABLE brand ADD image_data BLOB;
ALTER TABLE brand ADD image_type VARCHAR2(50);

-- Agregar columnas de imagen a VEHICLE (vehículos)
ALTER TABLE vehicle ADD image_data BLOB;
ALTER TABLE vehicle ADD image_type VARCHAR2(50);

COMMIT;
