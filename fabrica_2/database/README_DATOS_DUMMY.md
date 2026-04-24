# Datos Dummy para Pruebas

Este documento explica cómo cargar datos de prueba en el sistema de Fábrica.

## Orden de ejecución

Ejecuta los scripts **en este orden** como usuario **FABRICA**:

### 1. Roles (si no existen)

```bash
# En DBeaver: abrir 02_roles_data.sql y ejecutar como FABRICA
```

Esto crea los roles: **ADMIN**, **REGISTERED**, **ENTERPRISE**.

### 2. Datos del catálogo

```bash
# En DBeaver: abrir 05_datos_dummy.sql y ejecutar como FABRICA
```

Esto inserta:
- **6 categorías**: Motor, Transmisión, Frenos, Suspensión, Eléctrico, Filtros
- **6 marcas**: Bosch, Denso, NGK, Brembo, Mann Filter, Monroe
- **5 vehículos**: Toyota Camry 2020, Honda Civic 2019, Ford F-150 2021, Chevrolet Silverado 2018, Nissan Altima 2020
- **8 repuestos**: filtros, pastillas de freno, bujías, amortiguadores, alternador, sensor O2, discos de freno

## Verificar los datos

Después de ejecutar los scripts, verifica:

```sql
-- Ver categorías
SELECT * FROM category ORDER BY name;

-- Ver marcas
SELECT * FROM brand ORDER BY name;

-- Ver vehículos
SELECT * FROM vehicle ORDER BY make, line;

-- Ver repuestos
SELECT p.part_id, p.part_number, p.title, c.name AS categoria, b.name AS marca, p.price
FROM part p
LEFT JOIN category c ON p.category_id = c.category_id
LEFT JOIN brand b ON p.brand_id = b.brand_id
ORDER BY p.created_at DESC;

-- Ver roles
SELECT * FROM role ORDER BY name;
```

## Crear el primer usuario (ADMIN)

Desde el frontend:
1. Ve a **Registrarse** (`http://localhost:5173/register`)
2. Completa el formulario (el primer usuario será ADMIN automáticamente)
3. Después de registrarte, verás el enlace **Usuarios** y **Catálogo** en el navbar

## Probar las APIs

### Listar repuestos
```bash
curl http://localhost:8080/api/repuestos
```

### Listar categorías
```bash
curl http://localhost:8080/api/categorias
```

### Listar marcas
```bash
curl http://localhost:8080/api/marcas
```

### Buscar repuesto por número
```bash
curl http://localhost:8080/api/repuestos/numero/MF-OIL-001
```

### Crear un repuesto nuevo
```bash
curl -X POST http://localhost:8080/api/repuestos \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": 1,
    "brandId": 1,
    "partNumber": "TEST-001",
    "title": "Repuesto de prueba",
    "description": "Descripción de prueba",
    "weightLb": 1.5,
    "price": 49.99
  }'
```

## Notas importantes

- Los IDs de categorías y marcas en `05_datos_dummy.sql` asumen que las secuencias empiezan en 1. Si tus secuencias tienen otros valores, ajusta los `category_id` y `brand_id` en los INSERT de repuestos.
- Para ver los IDs reales: `SELECT category_id, name FROM category;` y `SELECT brand_id, name FROM brand;`
- El script usa `SELECT ... FROM dual WHERE NOT EXISTS` para evitar duplicados; puedes ejecutarlo múltiples veces sin problema.
