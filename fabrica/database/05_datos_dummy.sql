-- Datos dummy para probar el sistema de catálogo.
-- Ejecutar como FABRICA después de tener las tablas creadas.
-- Asume que las secuencias y tablas ya existen (CATEGORY, BRAND, VEHICLE, PART).

-- ============================================================================
-- CATEGORÍAS (jerárquicas)
-- ============================================================================
INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Motor', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Motor');

INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Transmisión', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Transmisión');

INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Frenos', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Frenos');

INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Suspensión', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Suspensión');

INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Eléctrico', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Eléctrico');

INSERT INTO category (category_id, name, parent_id)
SELECT category_seq.NEXTVAL, 'Filtros', NULL FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE name = 'Filtros');

-- ============================================================================
-- MARCAS DE REPUESTOS
-- ============================================================================
INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'Bosch' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'Bosch');

INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'Denso' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'Denso');

INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'NGK' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'NGK');

INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'Brembo' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'Brembo');

INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'Mann Filter' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'Mann Filter');

INSERT INTO brand (brand_id, name)
SELECT brand_seq.NEXTVAL, 'Monroe' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM brand WHERE name = 'Monroe');

-- ============================================================================
-- VEHÍCULOS (código universal + marca/línea/año)
-- ============================================================================
INSERT INTO vehicle (vehicle_id, universal_vehicle_code, make, line, year_number)
SELECT vehicle_seq.NEXTVAL, 'UVC-TOY-CAM-2020', 'Toyota', 'Camry', 2020 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE universal_vehicle_code = 'UVC-TOY-CAM-2020' AND year_number = 2020);

INSERT INTO vehicle (vehicle_id, universal_vehicle_code, make, line, year_number)
SELECT vehicle_seq.NEXTVAL, 'UVC-HON-CIV-2019', 'Honda', 'Civic', 2019 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE universal_vehicle_code = 'UVC-HON-CIV-2019' AND year_number = 2019);

INSERT INTO vehicle (vehicle_id, universal_vehicle_code, make, line, year_number)
SELECT vehicle_seq.NEXTVAL, 'UVC-FOR-F150-2021', 'Ford', 'F-150', 2021 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE universal_vehicle_code = 'UVC-FOR-F150-2021' AND year_number = 2021);

INSERT INTO vehicle (vehicle_id, universal_vehicle_code, make, line, year_number)
SELECT vehicle_seq.NEXTVAL, 'UVC-CHE-SIL-2018', 'Chevrolet', 'Silverado', 2018 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE universal_vehicle_code = 'UVC-CHE-SIL-2018' AND year_number = 2018);

INSERT INTO vehicle (vehicle_id, universal_vehicle_code, make, line, year_number)
SELECT vehicle_seq.NEXTVAL, 'UVC-NIS-ALT-2020', 'Nissan', 'Altima', 2020 FROM dual
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE universal_vehicle_code = 'UVC-NIS-ALT-2020' AND year_number = 2020);

-- ============================================================================
-- REPUESTOS (PART)
-- ============================================================================
-- Nota: Usaremos IDs 1-6 para categorías y marcas (ajusta si tus secuencias empezaron en otro número)
-- Si las categorías/marcas tienen otros IDs, consulta SELECT * FROM category; SELECT * FROM brand;
-- y ajusta los categoryId y brandId en los INSERT de abajo.

-- ============================================================================
-- REPUESTOS (PART)
-- ============================================================================
-- Nota: Usaremos IDs 1-6 para categorías y marcas (ajusta si tus secuencias empezaron en otro número)
-- Si las categorías/marcas tienen otros IDs, consulta SELECT * FROM category; SELECT * FROM brand;
-- y ajusta los categoryId y brandId en los INSERT de abajo.

-- Filtro de aceite (Categoría: Filtros=6, Marca: Mann Filter=5)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 6, 5, 'MF-OIL-001', 'Filtro de aceite Mann Filter', 'Filtro de aceite de alta eficiencia para motores gasolina y diesel', 0.5, 15.99, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'MF-OIL-001');

-- Pastillas de freno (Categoría: Frenos=3, Marca: Brembo=4)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 3, 4, 'BRE-PAD-F200', 'Pastillas de freno delanteras Brembo', 'Pastillas de freno cerámicas de alto rendimiento', 2.3, 89.99, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'BRE-PAD-F200');

-- Bujías (Categoría: Eléctrico=5, Marca: NGK=3)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 5, 3, 'NGK-SP-V4', 'Bujías NGK V-Power (set de 4)', 'Bujías de alto rendimiento con electrodo en V', 0.8, 32.50, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'NGK-SP-V4');

-- Amortiguadores (Categoría: Suspensión=4, Marca: Monroe=6)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 4, 6, 'MON-SHOCK-58620', 'Amortiguador delantero Monroe', 'Amortiguador de gas de alta presión para suspensión delantera', 5.2, 125.00, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'MON-SHOCK-58620');

-- Alternador (Categoría: Eléctrico=5, Marca: Bosch=1)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 5, 1, 'BSH-ALT-12V-90A', 'Alternador Bosch 12V 90A', 'Alternador remanufacturado con garantía de 2 años', 12.5, 245.00, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'BSH-ALT-12V-90A');

-- Sensor de oxígeno (Categoría: Eléctrico=5, Marca: Denso=2)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 5, 2, 'DEN-O2-234-4668', 'Sensor de oxígeno Denso', 'Sensor O2 de 4 cables compatible con múltiples modelos', 0.6, 67.50, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'DEN-O2-234-4668');

-- Filtro de aire (Categoría: Filtros=6, Marca: Mann Filter=5)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 6, 5, 'MF-AIR-C25114', 'Filtro de aire Mann Filter', 'Filtro de aire de papel de alta eficiencia', 0.9, 22.99, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'MF-AIR-C25114');

-- Discos de freno (Categoría: Frenos=3, Marca: Brembo=4)
INSERT INTO part (part_id, category_id, brand_id, part_number, title, description, weight_lb, price, active, created_at)
SELECT part_seq.NEXTVAL, 3, 4, 'BRE-DISC-09C84811', 'Discos de freno ventilados Brembo', 'Par de discos de freno ventilados para eje delantero', 18.5, 189.99, 1, SYSDATE FROM dual
WHERE NOT EXISTS (SELECT 1 FROM part WHERE part_number = 'BRE-DISC-09C84811');

COMMIT;
