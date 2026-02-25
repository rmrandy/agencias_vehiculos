# ✅ CRUD Completo de Productos - Implementado

## 📋 Resumen

Se ha implementado el CRUD completo (Crear, Leer, Actualizar, Eliminar) para productos/repuestos, incluyendo gestión de inventario desde el panel de administración.

## 🎯 Funcionalidades implementadas

### ✅ Crear producto
- Formulario completo con todos los campos
- Campos de inventario: `stockQuantity` y `lowStockThreshold`
- Upload de imagen opcional
- Validaciones en frontend y backend

### ✅ Leer/Listar productos
- Tabla con todos los productos
- Columnas: ID, Número, Título, Categoría, Marca, Precio, Stock, Estado
- Badges visuales de estado de inventario:
  - 🟢 "Disponible" - Stock normal
  - 🟡 "Bajo stock" - Stock <= threshold
  -🔴 "Sin stock" - Stock = 0

### ✅ Actualizar producto
- Botón "✏️ Editar" en cada fila
- Formulario pre-llenado con datos actuales
- Permite actualizar todos los campos incluyendo inventario
- Número de parte deshabilitado (no se puede cambiar)
- Imagen opcional (si no se proporciona, mantiene la actual)

### ✅ Eliminar producto
- Botón "🗑️ Eliminar" en cada fila
- Confirmación antes de eliminar
- Eliminación permanente de la base de datos

## 📝 Cambios realizados

### Frontend (`Catalogo.vue`)

**Nuevos campos en el formulario:**
```vue
<div class="form-group">
  <label>Stock disponible *</label>
  <input v-model="repuestoForm.stockQuantity" type="number" min="0" required />
</div>
<div class="form-group">
  <label>Umbral bajo stock *</label>
  <input v-model="repuestoForm.lowStockThreshold" type="number" min="1" required />
  <small>Se mostrará alerta cuando el stock sea menor o igual a este valor</small>
</div>
```

**Nuevas funciones:**
```javascript
// Editar producto
function editRepuesto(repuesto) {
  editingRepuesto.value = repuesto
  repuestoForm.value = { ...repuesto }
  showRepuestoForm.value = true
}

// Eliminar producto
async function deleteRepuesto(repuesto) {
  if (!confirm(`¿Estás seguro de eliminar "${repuesto.title}"?`)) return
  await deleteRepuestoApi(repuesto.partId)
  success('Producto eliminado')
  await loadData()
}

// Resetear formulario
function resetRepuestoForm() {
  repuestoForm.value = { /* valores por defecto */ }
  editingRepuesto.value = null
  showRepuestoForm.value = false
}
```

**Tabla actualizada:**
```vue
<table class="data-table">
  <thead>
    <tr>
      <th>ID</th>
      <th>Número</th>
      <th>Título</th>
      <th>Categoría</th>
      <th>Marca</th>
      <th>Precio</th>
      <th>Stock</th>        <!-- NUEVO -->
      <th>Estado</th>       <!-- NUEVO -->
      <th>Acciones</th>     <!-- NUEVO -->
    </tr>
  </thead>
  <tbody>
    <tr v-for="r in repuestos" :key="r.partId">
      <!-- ... otros campos ... -->
      <td>
        <strong>{{ r.availableQuantity || 0 }}</strong>
        <span v-if="r.reservedQuantity > 0" class="text-muted">
          ({{ r.reservedQuantity }} reservado)
        </span>
      </td>
      <td>
        <span v-if="!r.inStock" class="badge badge-danger">Sin stock</span>
        <span v-else-if="r.lowStock" class="badge badge-warning">Bajo stock</span>
        <span v-else class="badge badge-success">Disponible</span>
      </td>
      <td class="actions">
        <button @click="editRepuesto(r)" class="btn-icon">✏️</button>
        <button @click="deleteRepuesto(r)" class="btn-icon">🗑️</button>
      </td>
    </tr>
  </tbody>
</table>
```

**Nuevos estilos:**
```css
.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.badge-success { background: #d1fae5; color: #065f46; }
.badge-warning { background: #fef3c7; color: #92400e; }
.badge-danger { background: #fee2e2; color: #991b1b; }

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 4px 8px;
  transition: transform 0.2s;
}

.btn-icon:hover {
  transform: scale(1.2);
}
```

### Backend (`PartResource.java`)

**Endpoint PUT actualizado:**
```java
@PUT
@Path("/{id}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response update(@PathParam("id") Long id, Map<String, Object> body) {
    // Extraer campos básicos
    Long categoryId = ...;
    Long brandId = ...;
    String title = ...;
    BigDecimal price = ...;
    Integer stockQuantity = ...;  // NUEVO
    Integer lowStockThreshold = ...; // NUEVO
    
    // Actualizar datos básicos
    Part p = service.update(id, categoryId, brandId, title, description, weightLb, price, active);
    
    // Actualizar inventario si se proporcionó
    if (stockQuantity != null || lowStockThreshold != null) {
        p = service.updateInventory(id, stockQuantity, lowStockThreshold);
    }
    
    // Actualizar imagen si se proporcionó
    if (body.containsKey("imageData") && body.get("imageData") != null) {
        String base64Data = (String) body.get("imageData");
        String imageType = (String) body.get("imageType");
        byte[] imageData = Base64.getDecoder().decode(base64Data);
        p = service.updateImage(id, imageData, imageType);
    }
    
    return Response.ok(p).build();
}
```

## 🎬 Cómo usar

### 1. Crear un producto

1. Ve al panel de **Catálogo**
2. Haz clic en **"+ Nuevo repuesto"**
3. Llena el formulario:
   - Categoría *
   - Marca *
   - Número de parte *
   - Título *
   - Descripción
   - Peso (lb)
   - Precio *
   - **Stock disponible * (ej: 100)**
   - **Umbral bajo stock * (ej: 5)**
   - Imagen (opcional)
4. Haz clic en **"Crear repuesto"**
5. ✅ El producto se crea con inventario

### 2. Editar un producto

1. En la tabla de repuestos, haz clic en **✏️** (Editar)
2. El formulario se abre con los datos actuales
3. Modifica los campos que necesites (incluyendo stock)
4. La imagen es opcional:
   - Si no subes imagen nueva, mantiene la actual
   - Si subes imagen nueva, reemplaza la anterior
5. Haz clic en **"Actualizar repuesto"**
6. ✅ Los cambios se guardan

### 3. Eliminar un producto

1. En la tabla de repuestos, haz clic en **🗑️** (Eliminar)
2. Confirma la eliminación en el diálogo
3. ✅ El producto se elimina permanentemente

### 4. Ver estado de inventario

En la tabla, la columna **"Estado"** muestra:
- 🟢 **"Disponible"** - Hay stock suficiente
- 🟡 **"Bajo stock"** - Stock <= umbral (ej: <= 5)
- 🔴 **"Sin stock"** - Stock = 0

La columna **"Stock"** muestra:
- Cantidad disponible (stock - reservado)
- Si hay reservas, muestra: "50 (10 reservado)"

## 🧪 Testing

### Caso 1: Crear producto con inventario

```bash
# 1. Ir al panel de Catálogo
# 2. Crear nuevo repuesto con:
#    - Stock: 100
#    - Umbral: 5
# 3. Verificar en BD:

SELECT part_id, part_number, title, stock_quantity, low_stock_threshold
FROM PART
WHERE part_number = 'ABC-123';
```

### Caso 2: Editar inventario

```bash
# 1. Hacer clic en ✏️ Editar
# 2. Cambiar stock a 3
# 3. Guardar
# 4. Verificar que el badge cambió a "🟡 Bajo stock"
```

### Caso 3: Producto sin stock

```bash
# 1. Editar producto
# 2. Poner stock en 0
# 3. Guardar
# 4. Verificar:
#    - Badge: "🔴 Sin stock"
#    - En la tienda: botón "Agregar" deshabilitado
#    - En detalle: "❌ Fuera de Stock"
```

### Caso 4: Eliminar producto

```bash
# 1. Hacer clic en 🗑️ Eliminar
# 2. Confirmar
# 3. Verificar que desapareció de la tabla
# 4. Verificar en BD:

SELECT * FROM PART WHERE part_id = 123;
-- Debe devolver 0 filas
```

## 📊 Endpoints API

### Crear producto
```http
POST /api/repuestos
Content-Type: application/json

{
  "categoryId": 1,
  "brandId": 1,
  "partNumber": "ABC-123",
  "title": "Filtro de aceite",
  "description": "Filtro de alta calidad",
  "weightLb": 0.5,
  "price": 25.99,
  "stockQuantity": 100,
  "lowStockThreshold": 5,
  "imageData": "base64...",
  "imageType": "image/jpeg"
}
```

### Actualizar producto
```http
PUT /api/repuestos/{id}
Content-Type: application/json

{
  "categoryId": 1,
  "brandId": 1,
  "title": "Filtro de aceite Premium",
  "price": 29.99,
  "stockQuantity": 50,
  "lowStockThreshold": 10
  // Campos opcionales: description, weightLb, imageData, imageType
}
```

### Eliminar producto
```http
DELETE /api/repuestos/{id}
```

### Listar productos
```http
GET /api/repuestos
```

### Obtener producto por ID
```http
GET /api/repuestos/{id}
```

## ✅ Checklist de funcionalidades

- [x] Crear producto con inventario
- [x] Listar productos con estado de stock
- [x] Editar producto (todos los campos)
- [x] Editar inventario
- [x] Actualizar imagen (opcional)
- [x] Eliminar producto
- [x] Validaciones en frontend
- [x] Validaciones en backend
- [x] Badges visuales de estado
- [x] Confirmación antes de eliminar
- [x] Toast notifications de éxito/error
- [x] Scroll automático al formulario al editar
- [x] Deshabilitar número de parte al editar
- [x] Mostrar cantidad reservada en tabla

## 🎨 Mejoras visuales

- Botones con iconos emoji (✏️ 🗑️)
- Badges de colores para estado de stock
- Hover effects en botones
- Formulario con grid responsive
- Tabla con hover en filas
- Confirmación de eliminación
- Scroll suave al editar

## 🔮 Próximas mejoras sugeridas

1. **Búsqueda y filtros**
   - Buscar por título o número de parte
   - Filtrar por categoría/marca
   - Filtrar por estado de stock

2. **Paginación**
   - Mostrar 20 productos por página
   - Navegación entre páginas

3. **Exportar datos**
   - Exportar a CSV/Excel
   - Incluir inventario

4. **Historial de cambios**
   - Log de modificaciones
   - Quién cambió qué y cuándo

5. **Importación masiva**
   - Subir CSV con productos
   - Actualizar inventario en lote

---

**Implementado:** Febrero 2026  
**Estado:** ✅ CRUD Completo y funcional
