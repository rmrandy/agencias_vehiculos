# 📦 Detalle de Producto - Estado y Roadmap

## ✅ Problema corregido

**Error:** "Failed to fetch" al abrir detalle de producto

**Causa:** Las rutas API no incluían el prefijo `/api`

**Solución aplicada:**
```javascript
// ❌ Antes
producto.value = await apiFetch(`/repuestos/${partId}`)

// ✅ Ahora
producto.value = await apiFetch(`/api/repuestos/${partId}`)
```

## 📋 Estado actual de la página de detalle

### ✅ Implementado

1. **Información básica del repuesto**
   - ✅ Título
   - ✅ Descripción
   - ✅ Precio
   - ✅ Número de parte
   - ✅ Categoría
   - ✅ Marca
   - ✅ Peso (si existe)

2. **Imagen del producto**
   - ✅ Visualización de imagen desde BLOB
   - ✅ Placeholder si no hay imagen

3. **Acciones**
   - ✅ Selector de cantidad
   - ✅ Botón "Agregar al carrito"
   - ✅ Validación de login antes de agregar
   - ✅ Breadcrumb de navegación

4. **Diseño responsive**
   - ✅ Layout de 2 columnas (galería + info)
   - ✅ Estilos modernos

## 🚧 Pendiente de implementar

Según el documento del proyecto, faltan las siguientes funcionalidades:

### 1. Galería de imágenes (1-3 fotos)

**Estado:** ❌ No implementado (actualmente solo 1 imagen)

**Requiere:**
- Modificar modelo `Part` para soportar múltiples imágenes
- Crear tabla `part_images` con relación 1:N
- Actualizar frontend para mostrar thumbnails
- Implementar navegación entre imágenes

### 2. Características técnicas

**Estado:** ❌ No implementado

**Requiere:**
- Crear tabla `part_characteristics` con campos:
  - `characteristic_id` (PK)
  - `part_id` (FK)
  - `characteristic_type` (ej: "Material", "Voltaje", "Dimensiones")
  - `value` (ej: "Acero inoxidable", "12V", "10x5x3 cm")
- Crear endpoints CRUD para características
- Mostrar en frontend como lista o tabla

### 3. Información de compatibilidad

**Estado:** ❌ No implementado

**Requiere:**
- Crear tabla `part_compatibility` con campos:
  - `compatibility_id` (PK)
  - `part_id` (FK)
  - `vehicle_id` (FK) - opcional
  - `brand` (ej: "Toyota")
  - `line` (ej: "Camry")
  - `year_from` (ej: 2015)
  - `year_to` (ej: 2020)
  - `universal_code` (ej: código OEM)
- Crear endpoints para gestionar compatibilidad
- Mostrar en frontend como tabla filtrable

### 4. Sistema de inventario

**Estado:** ❌ No implementado

**Requiere:**
- Agregar campos a tabla `part`:
  - `stock_quantity` (INT)
  - `low_stock_threshold` (INT, default 5)
  - `in_stock` (BOOLEAN, computed)
- Lógica de validación:
  - `stock_quantity > 0` → Botón activo
  - `stock_quantity = 0` → Mostrar "Fuera de Stock", deshabilitar botón
  - `stock_quantity <= 5` → Mostrar badge "Bajo inventario"
- Actualizar stock al crear pedido (restar cantidad)

### 5. Sistema de ratings (0-5 estrellas)

**Estado:** ❌ No implementado

**Requiere:**
- Crear tabla `part_ratings`:
  - `rating_id` (PK)
  - `part_id` (FK)
  - `user_id` (FK)
  - `rating` (INT, 0-5)
  - `created_at` (TIMESTAMP)
- Crear endpoints:
  - `POST /api/repuestos/{id}/rating` - Crear/actualizar rating
  - `GET /api/repuestos/{id}/rating/average` - Obtener promedio
- Mostrar en frontend:
  - Promedio de estrellas (ej: ⭐⭐⭐⭐☆ 4.2/5)
  - Total de ratings (ej: "basado en 127 opiniones")
  - Selector de estrellas para dejar rating (solo usuarios registrados)

### 6. Sistema de comentarios multinivel

**Estado:** ❌ No implementado

**Requiere:**
- Crear tabla `part_comments`:
  - `comment_id` (PK)
  - `part_id` (FK)
  - `user_id` (FK)
  - `parent_comment_id` (FK, nullable) - para respuestas
  - `comment_text` (TEXT)
  - `created_at` (TIMESTAMP)
  - `updated_at` (TIMESTAMP)
- Crear endpoints:
  - `POST /api/repuestos/{id}/comentarios` - Crear comentario
  - `GET /api/repuestos/{id}/comentarios` - Listar comentarios (con estructura jerárquica)
  - `POST /api/comentarios/{id}/responder` - Responder a un comentario
  - `DELETE /api/comentarios/{id}` - Eliminar comentario (solo autor o admin)
- Mostrar en frontend:
  - Lista de comentarios con respuestas anidadas
  - Botón "Responder" en cada comentario
  - Formulario para nuevo comentario (solo usuarios registrados)
  - Indicador de autor y fecha

### 7. Endpoint REST para integración B2B

**Estado:** ❌ No implementado

**Requiere:**
- Crear endpoint especial:
  ```java
  @GET
  @Path("/{partNumber}/detalle-completo")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDetalleCompleto(@PathParam("partNumber") String partNumber)
  ```
- Debe devolver JSON con:
  - Información básica del repuesto
  - Imágenes (URLs o base64)
  - Características técnicas
  - Compatibilidad de vehículos
  - Inventario disponible
  - Rating promedio
  - Precio
- Documentar endpoint para distribuidores

## 📊 Priorización sugerida

### Fase 1: Inventario (crítico para e-commerce)
1. ✅ Agregar campos de stock a tabla `part`
2. ✅ Implementar validación de stock en backend
3. ✅ Mostrar estado de stock en frontend
4. ✅ Actualizar stock al crear pedido

### Fase 2: Ratings y comentarios (engagement)
1. ✅ Crear tablas `part_ratings` y `part_comments`
2. ✅ Implementar endpoints de ratings
3. ✅ Implementar endpoints de comentarios
4. ✅ Mostrar ratings en frontend
5. ✅ Mostrar comentarios en frontend

### Fase 3: Información técnica (calidad de datos)
1. ✅ Crear tabla `part_characteristics`
2. ✅ Crear tabla `part_compatibility`
3. ✅ Implementar endpoints CRUD
4. ✅ Mostrar en frontend

### Fase 4: Galería y B2B (mejoras)
1. ✅ Implementar galería de múltiples imágenes
2. ✅ Crear endpoint B2B para distribuidores
3. ✅ Documentar API B2B

## 🎯 Próximos pasos inmediatos

1. **Probar que el detalle de producto ahora carga correctamente**
   - Reiniciar frontend si es necesario
   - Navegar a un producto desde la tienda
   - Verificar que se muestran todos los datos

2. **Decidir qué fase implementar primero**
   - ¿Inventario? (recomendado para e-commerce funcional)
   - ¿Ratings/comentarios? (para engagement)
   - ¿Información técnica? (para calidad de catálogo)

3. **Crear scripts de migración de BD**
   - Para agregar las nuevas tablas y campos
   - Mantener compatibilidad con datos existentes

---

**Nota:** El error "Failed to fetch" ya está corregido. La página de detalle debería funcionar ahora. Las funcionalidades adicionales se implementarán progresivamente según prioridad.
