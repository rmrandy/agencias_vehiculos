-- Galería BLOB por repuesto (2–5 imágenes). Ejecutar en esquema Oracle existente.
ALTER TABLE part_image ADD (image_data BLOB, image_type VARCHAR2(50));
ALTER TABLE part_image MODIFY url_path NULL;
