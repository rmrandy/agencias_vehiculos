# 🛒 Carrito de Compras - Mejoras Implementadas

## ✅ Nuevas funcionalidades

### 1. Validación de stock en tiempo real

Antes de proceder al checkout, el sistema verifica automáticamente el stock disponible de todos los productos en el carrito.

**Tipos de alertas:**

🔴 **Sin stock** (rojo)
```
❌ Producto sin stock
```
- El producto ya no está disponible
- Debes eliminarlo del carrito

🟡 **Stock insuficiente** (amarillo)
```
⚠️ Solo hay X disponibles
[Botón: Ajustar a X]
```
- Hay stock, pero menos de lo que pediste
- Botón para ajustar automáticamente a la cantidad máxima

🔵 **Bajo inventario** (azul)
```
ℹ️ Bajo inventario (X disponibles)
```
- Hay stock suficiente para tu pedido
- Pero quedan pocas unidades (alerta informativa)

### 2. Botón "Ajustar cantidad"

Si un producto tiene stock insuficiente, aparece un botón para ajustar automáticamente la cantidad al máximo disponible.

**Ejemplo:**
- Tienes 10 unidades en el carrito
- Solo hay 5 disponibles
- Aparece: "⚠️ Solo hay 5 disponibles [Ajustar a 5]"
- Haces clic → La cantidad se ajusta a 5

### 3. Bloqueo de checkout

El botón "Proceder al pago" se deshabilita automáticamente si:
- ❌ Hay productos sin stock
- ❌ Hay productos con stock insuficiente
- ✅ Solo se habilita cuando todo está OK

### 4. Botón "Continuar comprando"

Nuevo botón para volver a la tienda sin perder el carrito.

### 5. Límites en selector de cantidad

- Botón "−" deshabilitado cuando cantidad = 1
- Validación de cantidad mínima (1)

## 🎯 Flujo de uso

### Escenario 1: Todo OK

```
1. Usuario tiene productos en el carrito
2. Hace clic en "Proceder al pago"
   ↓
3. Sistema verifica stock de todos los productos
4. ✅ Todo OK → Crea el pedido
5. Limpia el carrito
6. Redirige a detalle del pedido
```

### Escenario 2: Stock insuficiente

```
1. Usuario tiene 10 unidades de un producto
2. Hace clic en "Proceder al pago"
   ↓
3. Sistema verifica stock
4. ⚠️ Solo hay 5 disponibles
5. Muestra alerta: "⚠️ Solo hay 5 disponibles [Ajustar a 5]"
6. Botón "Proceder al pago" deshabilitado
   ↓
7. Usuario hace clic en "Ajustar a 5"
8. Cantidad se actualiza a 5
9. Alerta desaparece
10. Botón "Proceder al pago" se habilita
11. ✅ Puede continuar
```

### Escenario 3: Producto sin stock

```
1. Usuario tiene producto en el carrito
2. Mientras tanto, otro usuario compró todo el stock
3. Hace clic en "Proceder al pago"
   ↓
4. Sistema verifica stock
5. ❌ Producto sin stock
6. Muestra alerta: "❌ Producto sin stock"
7. Botón "Proceder al pago" deshabilitado
   ↓
8. Usuario debe eliminar el producto (🗑️)
9. Botón se habilita
10. ✅ Puede continuar con otros productos
```

## 📝 Código implementado

### Verificación de stock

```javascript
async function checkStockAvailability() {
  stockWarnings.value = []
  
  for (const item of cartItems.value) {
    const product = await getRepuesto(item.partId)
    
    if (!product.inStock) {
      stockWarnings.value.push({
        partId: item.partId,
        title: item.title,
        message: 'Producto sin stock',
        type: 'out'
      })
    } else if (product.availableQuantity < item.qty) {
      stockWarnings.value.push({
        partId: item.partId,
        title: item.title,
        message: `Solo hay ${product.availableQuantity} disponibles`,
        type: 'insufficient',
        maxQty: product.availableQuantity
      })
    } else if (product.lowStock) {
      stockWarnings.value.push({
        partId: item.partId,
        title: item.title,
        message: `Bajo inventario (${product.availableQuantity} disponibles)`,
        type: 'low'
      })
    }
  }
}
```

### Validación antes de checkout

```javascript
async function proceedToCheckout() {
  // Verificar stock antes de proceder
  await checkStockAvailability()
  
  if (hasStockIssues.value) {
    showError('Algunos productos no tienen stock suficiente. Por favor, ajusta las cantidades.')
    return
  }
  
  // Crear pedido...
}
```

### Template con alertas

```vue
<div v-for="item in cartItems" :key="item.partId" class="cart-item-wrapper">
  <!-- Tarjeta del producto -->
  <div class="cart-item">
    <!-- ... contenido ... -->
  </div>
  
  <!-- Alerta de stock (si existe) -->
  <div v-if="getWarningForItem(item.partId)" class="stock-alert">
    <span class="alert-icon">⚠️</span>
    <span class="alert-message">{{ warning.message }}</span>
    <button @click="adjustQuantity(item.partId, warning.maxQty)">
      Ajustar a {{ warning.maxQty }}
    </button>
  </div>
</div>
```

## 🎨 Estilos visuales

### Alertas de stock

**Sin stock (rojo):**
```css
background: #fee2e2;
color: #991b1b;
border: 1px solid #fecaca;
```

**Stock insuficiente (amarillo):**
```css
background: #fef3c7;
color: #92400e;
border: 1px solid #fde68a;
```

**Bajo inventario (azul):**
```css
background: #dbeafe;
color: #1e40af;
border: 1px solid #bfdbfe;
```

### Botón "Ajustar cantidad"

```css
background: #f59e0b; (naranja)
color: white;
font-weight: 600;
```

## 🧪 Testing

### Test 1: Carrito normal (todo OK)

1. Agrega productos con stock suficiente
2. Ve al carrito
3. ✅ No hay alertas
4. ✅ Botón "Proceder al pago" habilitado
5. Haz clic → Crea el pedido exitosamente

### Test 2: Stock insuficiente

1. En DBeaver, reduce el stock de un producto:
   ```sql
   UPDATE PART SET stock_quantity = 2 WHERE part_id = 2;
   COMMIT;
   ```
2. En el carrito, ese producto tiene qty = 4
3. Haz clic en "Proceder al pago"
4. ⚠️ Aparece alerta: "Solo hay 2 disponibles [Ajustar a 2]"
5. ✅ Botón "Proceder al pago" deshabilitado
6. Haz clic en "Ajustar a 2"
7. ✅ Cantidad se ajusta
8. ✅ Alerta desaparece
9. ✅ Botón se habilita

### Test 3: Producto sin stock

1. En DBeaver, pon stock en 0:
   ```sql
   UPDATE PART SET stock_quantity = 0 WHERE part_id = 2;
   COMMIT;
   ```
2. Haz clic en "Proceder al pago"
3. ❌ Aparece alerta: "Producto sin stock"
4. ✅ Botón "Proceder al pago" deshabilitado
5. Elimina el producto (🗑️)
6. ✅ Botón se habilita para otros productos

### Test 4: Bajo inventario (informativo)

1. Producto tiene stock = 4, qty en carrito = 2
2. Haz clic en "Proceder al pago"
3. ℹ️ Aparece alerta azul: "Bajo inventario (4 disponibles)"
4. ✅ Botón "Proceder al pago" sigue habilitado
5. ✅ Puede continuar normalmente

## 🔄 Flujo completo de checkout

```
Usuario en carrito
   ↓
Hace clic en "Proceder al pago"
   ↓
Sistema verifica stock de TODOS los productos
   ↓
   ├─ ✅ Todo OK
   │  ├─ Reserva stock
   │  ├─ Crea pedido
   │  ├─ Confirma venta (reduce stock)
   │  ├─ Limpia carrito
   │  └─ Redirige a detalle del pedido
   │
   └─ ❌ Problemas de stock
      ├─ Muestra alertas específicas
      ├─ Deshabilita botón de checkout
      └─ Usuario debe ajustar/eliminar productos
```

## ✅ Checklist de funcionalidades

- [x] Validación de stock antes de checkout
- [x] Alertas visuales por tipo de problema
- [x] Botón "Ajustar cantidad" automático
- [x] Bloqueo de checkout si hay problemas
- [x] Botón "Continuar comprando"
- [x] Límites en selector de cantidad
- [x] Botón "−" deshabilitado en qty = 1
- [x] Confirmación visual de alertas
- [x] Estilos diferenciados por tipo de alerta
- [x] Persistencia del carrito en localStorage

## 🔮 Mejoras futuras sugeridas

1. **Cupones de descuento**
   - Campo para ingresar código
   - Validación y aplicación de descuento

2. **Estimación de envío**
   - Calcular costo según ubicación
   - Opciones de envío (estándar, express)

3. **Guardar para después**
   - Mover productos a "lista de deseos"
   - Recuperar después

4. **Notificaciones de stock**
   - Avisar cuando un producto sin stock vuelva a estar disponible
   - Email o notificación push

5. **Productos relacionados**
   - Sugerencias en el carrito
   - "Otros también compraron..."

---

**Implementado:** Febrero 2026  
**Estado:** ✅ Carrito completo con validación de stock
