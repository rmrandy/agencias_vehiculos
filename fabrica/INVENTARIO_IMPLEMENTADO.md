# ✅ Sistema de Inventario - Implementado

## 📋 Resumen

Se ha implementado un sistema completo de gestión de inventario para los repuestos, incluyendo:
- Validación de stock disponible
- Reserva de inventario al crear pedidos
- Confirmación de venta (reducción de stock)
- Indicadores visuales de disponibilidad
- Alertas de bajo inventario

## 🗄️ Cambios en la base de datos

### Nuevos campos en tabla `PART`

```sql
stock_quantity        NUMBER(10)  DEFAULT 0 NOT NULL
low_stock_threshold   NUMBER(10)  DEFAULT 5 NOT NULL
reserved_quantity     NUMBER(10)  DEFAULT 0 NOT NULL
```

**Descripción:**
- `stock_quantity`: Cantidad total en inventario
- `low_stock_threshold`: Umbral para considerar "bajo inventario" (default 5)
- `reserved_quantity`: Cantidad reservada en pedidos pendientes

### Script de migración

**Archivo:** `database/07_add_inventory_fields.sql`

**Para ejecutar:**

```bash
# 1. Conectar a Oracle como usuario FABRICA
sqlplus FABRICA/123@localhost:1521/XEPDB1

# 2. Ejecutar el script
@database/07_add_inventory_fields.sql

# 3. Verificar
SELECT part_id, part_number, title, stock_quantity, low_stock_threshold, reserved_quantity
FROM PART
ORDER BY part_id;
```

**Nota:** El script inicializa todos los productos existentes con `stock_quantity = 100` para testing.

## 🔧 Cambios en el backend

### 1. Modelo `Part.java`

**Nuevos campos:**
```java
private Integer stockQuantity = 0;
private Integer lowStockThreshold = 5;
private Integer reservedQuantity = 0;
```

**Campos calculados (Transient):**
```java
@Transient
private Boolean inStock;        // true si availableQuantity > 0

@Transient
private Boolean lowStock;       // true si availableQuantity <= lowStockThreshold

@Transient
private Integer availableQuantity;  // stockQuantity - reservedQuantity
```

Estos campos se calculan automáticamente en `@PostLoad` y se incluyen en el JSON de respuesta.

### 2. Servicio `PartService.java`

**Nuevos métodos:**

```java
// Actualizar inventario
Part updateInventory(Long id, Integer stockQuantity, Integer lowStockThreshold)

// Reservar stock para un pedido
boolean reserveStock(Long id, Integer quantity)

// Confirmar venta (reducir stock)
void confirmSale(Long id, Integer quantity)

// Liberar stock reservado (si se cancela pedido)
void releaseStock(Long id, Integer quantity)

// Verificar disponibilidad
boolean checkAvailability(Long id, Integer quantity)
```

### 3. Controlador `PartResource.java`

**Nuevos endpoints:**

```java
// Actualizar inventario de un repuesto
PUT /api/repuestos/{id}/inventario
Body: { "stockQuantity": 50, "lowStockThreshold": 10 }

// Verificar disponibilidad
GET /api/repuestos/{id}/disponibilidad?cantidad=5
Response: { "available": true, "quantity": 5 }
```

### 4. Servicio `OrderService.java`

**Flujo actualizado al crear pedido:**

1. **Validar** disponibilidad de stock para todos los items
2. **Reservar** stock (incrementar `reserved_quantity`)
3. Crear orden en BD
4. Crear items del pedido
5. **Confirmar venta** (reducir `stock_quantity` y `reserved_quantity`)
6. Crear estado inicial del pedido

**Manejo de errores:**
- Si no hay suficiente stock → Error 400 con mensaje detallado
- Si falla la reserva → Rollback de todas las reservas

## 🎨 Cambios en el frontend

### 1. Vista `DetalleProducto.vue`

**Indicadores de stock:**

```vue
<!-- Badge de estado -->
<div v-if="!producto.inStock" class="stock-badge out-of-stock">
  ❌ Fuera de Stock
</div>
<div v-else-if="producto.lowStock" class="stock-badge low-stock">
  ⚠️ Bajo inventario ({{ producto.availableQuantity }} disponibles)
</div>
<div v-else class="stock-badge in-stock">
  ✅ En Stock ({{ producto.availableQuantity }} disponibles)
</div>
```

**Selector de cantidad:**
- Máximo limitado a `availableQuantity`
- Botón "+" deshabilitado si se alcanza el máximo
- Hint mostrando cantidad máxima disponible

**Botón agregar al carrito:**
- Deshabilitado si no hay stock
- Texto dinámico según estado:
  - Sin stock: "❌ Fuera de Stock"
  - Sin login: "🔒 Iniciar sesión para comprar"
  - Normal: "🛒 Agregar al carrito"

### 2. Vista `Tienda.vue`

**Badges en tarjetas de productos:**
- 🟢 "Disponible" (verde) - Stock normal
- 🟡 "Bajo stock" (amarillo) - Stock <= threshold
- 🔴 "Agotado" (rojo) - Sin stock

**Botón agregar:**
- Deshabilitado si no hay stock
- Texto: "❌ Agotado" o "🛒 Agregar"

## 🎯 Flujo de inventario

### Escenario 1: Compra exitosa

```
1. Usuario selecciona producto (stock: 100, reservado: 0)
2. Agrega 5 unidades al carrito
3. Procede al checkout
   ↓
4. Sistema valida: disponible = 100 - 0 = 100 ≥ 5 ✅
5. Sistema reserva: reservado = 0 + 5 = 5
   (stock: 100, reservado: 5, disponible: 95)
6. Se crea el pedido
7. Sistema confirma venta:
   - stock = 100 - 5 = 95
   - reservado = 5 - 5 = 0
   (stock: 95, reservado: 0, disponible: 95)
```

### Escenario 2: Stock insuficiente

```
1. Producto tiene: stock = 3, reservado = 0
2. Usuario intenta comprar 5 unidades
   ↓
3. Sistema valida: disponible = 3 - 0 = 3 < 5 ❌
4. Error 400: "Stock insuficiente para: [Producto]
   (disponible: 3, solicitado: 5)"
5. No se crea el pedido
```

### Escenario 3: Múltiples usuarios comprando simultáneamente

```
Usuario A:
1. Producto: stock = 10, reservado = 0
2. Agrega 7 al carrito
3. Sistema reserva: reservado = 7
   (stock: 10, reservado: 7, disponible: 3)

Usuario B (simultáneamente):
1. Producto: stock = 10, reservado = 7
2. Intenta agregar 5 al carrito
3. Sistema valida: disponible = 10 - 7 = 3 < 5 ❌
4. Error: Stock insuficiente
```

## 🎨 Estilos visuales

### Badges de stock

**En stock (verde):**
```css
background: #d1fae5;
color: #065f46;
```

**Bajo stock (amarillo):**
```css
background: #fef3c7;
color: #92400e;
```

**Fuera de stock (rojo):**
```css
background: #fee2e2;
color: #991b1b;
```

## 🧪 Testing

### 1. Probar migración de BD

```sql
-- Verificar campos agregados
DESC PART;

-- Ver inventario actual
SELECT part_id, title, stock_quantity, reserved_quantity, 
       (stock_quantity - reserved_quantity) as disponible
FROM PART;

-- Actualizar stock de un producto
UPDATE PART SET stock_quantity = 5 WHERE part_id = 1;
COMMIT;
```

### 2. Probar en frontend

**Caso 1: Producto con stock normal**
1. Ir a la tienda
2. Ver badge "✅ Disponible"
3. Entrar al detalle
4. Ver cantidad disponible
5. Agregar al carrito → ✅ Funciona

**Caso 2: Producto con bajo stock**
1. Reducir stock a 3 unidades en BD
2. Recargar tienda
3. Ver badge "⚠️ Bajo stock"
4. Entrar al detalle
5. Ver "Bajo inventario (3 disponibles)"
6. Intentar agregar 5 → Máximo limitado a 3

**Caso 3: Producto sin stock**
1. Poner stock en 0 en BD
2. Recargar tienda
3. Ver badge "❌ Agotado"
4. Botón "Agregar" deshabilitado
5. Entrar al detalle
6. Ver "❌ Fuera de Stock"
7. Botón "Agregar al carrito" deshabilitado

### 3. Probar flujo de pedido

```bash
# 1. Crear pedido con stock suficiente
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"partId": 1, "qty": 2}
    ]
  }'

# 2. Verificar que se redujo el stock
SELECT stock_quantity, reserved_quantity FROM PART WHERE part_id = 1;

# 3. Intentar crear pedido sin stock suficiente
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "items": [
      {"partId": 1, "qty": 1000}
    ]
  }'
# Debe devolver error 400
```

## 📊 Gestión de inventario (Admin)

Los administradores pueden actualizar el inventario desde el panel de catálogo o via API:

```javascript
// Actualizar inventario
await fetch(`/api/repuestos/${partId}/inventario`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    stockQuantity: 100,
    lowStockThreshold: 10
  })
})
```

## 🔮 Mejoras futuras sugeridas

1. **Panel de inventario para admin**
   - Vista de productos con bajo stock
   - Alertas automáticas
   - Historial de movimientos

2. **Reabastecimiento automático**
   - Generar órdenes de compra cuando stock < threshold
   - Integración con proveedores

3. **Inventario por ubicación**
   - Múltiples almacenes
   - Transferencias entre ubicaciones

4. **Reservas con expiración**
   - Liberar stock si el usuario no completa la compra en X minutos
   - Implementar con jobs programados

---

**Implementado:** Febrero 2026  
**Estado:** ✅ Completo y funcional
