# ✅ Cambio Implementado: Imágenes como BLOB

## 🎯 Cambio Realizado

Las imágenes ahora se almacenan **directamente en la base de datos Oracle como BLOB**, no como archivos en el sistema ni URLs.

---

## 📊 Diferencias Clave

### ❌ Antes (URLs)
```
Frontend → Sube archivo → Backend guarda en /uploads/ → Retorna URL
Frontend muestra: <img src="/uploads/images/abc123.jpg" />
```

### ✅ Ahora (BLOB)
```
Frontend → Lee archivo como base64 → Backend decodifica y guarda en BD
Frontend muestra: <img src="/api/images/part/123" />
```

---

## 🗄️ Estructura de Base de Datos

### Columnas agregadas

```sql
-- Ejecutar: @06_add_image_url_columns.sql

ALTER TABLE part ADD image_data BLOB;
ALTER TABLE part ADD image_type VARCHAR2(50);

ALTER TABLE category ADD image_data BLOB;
ALTER TABLE category ADD image_type VARCHAR2(50);

ALTER TABLE brand ADD image_data BLOB;
ALTER TABLE brand ADD image_type VARCHAR2(50);

ALTER TABLE vehicle ADD image_data BLOB;
ALTER TABLE vehicle ADD image_type VARCHAR2(50);
```

---

## 🔄 Flujo Completo

### 1. Usuario selecciona imagen en frontend

```javascript
// ImageUpload.vue
const file = event.target.files[0]

// Leer como base64
const reader = new FileReader()
reader.onload = (e) => {
  const base64Data = e.target.result
  // "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
  
  emit('update:modelValue', {
    imageData: base64Data,
    imageType: file.type // "image/jpeg"
  })
}
reader.readAsDataURL(file)
```

### 2. Frontend envía al backend

```javascript
// Catalogo.vue
const payload = {
  name: "Filtro de aceite",
  categoryId: 1,
  brandId: 2,
  imageData: "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  imageType: "image/jpeg"
}

await createRepuesto(payload)
```

### 3. Backend procesa y guarda

```java
// PartResource.java
String base64Data = (String) body.get("imageData");
String imageType = (String) body.get("imageType");

// Remover prefijo "data:image/jpeg;base64,"
if (base64Data.contains(",")) {
    base64Data = base64Data.split(",")[1];
}

// Decodificar base64 a bytes
byte[] imageBytes = Base64.getDecoder().decode(base64Data);

// Crear repuesto
Part part = service.create(...);

// Guardar imagen
part = service.updateImage(part.getPartId(), imageBytes, imageType);
```

### 4. Oracle almacena como BLOB

```sql
-- La imagen queda guardada en la tabla
SELECT part_id, title, 
       DBMS_LOB.GETLENGTH(image_data) as size_bytes,
       image_type
FROM part
WHERE part_id = 123;

-- Resultado:
-- PART_ID | TITLE            | SIZE_BYTES | IMAGE_TYPE
-- 123     | Filtro de aceite | 245678     | image/jpeg
```

### 5. Frontend muestra imagen

```vue
<!-- Opción A: Endpoint dedicado -->
<img :src="`/api/images/part/${part.partId}`" alt="Repuesto" />

<!-- El backend sirve la imagen desde BD -->
GET /api/images/part/123
Response: (bytes de la imagen)
Content-Type: image/jpeg
```

---

## 🆕 Nuevos Endpoints

### GET /api/images/{entityType}/{id}

Obtiene la imagen de una entidad desde la base de datos.

**Parámetros:**
- `entityType`: `part`, `category`, `brand`, `vehicle`
- `id`: ID de la entidad

**Ejemplo:**
```bash
curl http://localhost:8080/api/images/part/123 --output filtro.jpg
curl http://localhost:8080/api/images/category/5 --output motor.png
curl http://localhost:8080/api/images/brand/8 --output bosch.jpg
```

**Respuesta:**
- Content-Type: `image/jpeg`, `image/png`, etc.
- Body: Bytes de la imagen
- Headers: `Cache-Control: max-age=86400` (cache 1 día)

### POST /api/images/validate

Valida una imagen en base64 sin guardarla.

**Request:**
```json
{
  "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "imageType": "image/jpeg"
}
```

**Response:**
```json
{
  "valid": true,
  "size": 245678,
  "sizeKB": 239
}
```

---

## ✅ Ventajas

### 1. Simplicidad
- ✅ No hay sistema de archivos que gestionar
- ✅ No hay rutas relativas/absolutas
- ✅ No hay problemas de permisos
- ✅ Backup de BD incluye las imágenes

### 2. Integridad
- ✅ Transacciones ACID
- ✅ Si falla el INSERT, no queda imagen huérfana
- ✅ DELETE CASCADE elimina imagen automáticamente

### 3. Seguridad
- ✅ Control de acceso a nivel de BD
- ✅ No hay acceso directo al filesystem
- ✅ Encriptación de BD protege las imágenes

### 4. Portabilidad
- ✅ Fácil migrar entre servidores (solo BD)
- ✅ No hay rutas hardcodeadas
- ✅ Funciona en contenedores sin volúmenes

---

## 📝 Archivos Modificados

### Backend
- ✅ `Part.java`, `Category.java`, `Brand.java`, `Vehicle.java` - Campos `imageData` y `imageType`
- ✅ `PartService.java`, `CategoryService.java`, `BrandService.java` - Método `updateImage()`
- ✅ `PartResource.java`, `CategoryResource.java`, `BrandResource.java` - Procesamiento base64
- ✅ `ImageResource.java` - Endpoint GET para servir imágenes desde BD
- ✅ `Main.java` - Eliminado ResourceHandler (ya no se necesita)

### Frontend
- ✅ `ImageUpload.vue` - Lee archivos como base64, no sube a servidor
- ✅ `Catalogo.vue` - Envía `imageData` y `imageType` en lugar de `imageUrl`

### Base de Datos
- ✅ `06_add_image_url_columns.sql` - Columnas BLOB en lugar de VARCHAR2

### Documentación
- ✅ `IMAGENES_BLOB.md` - Documentación completa del sistema
- ✅ `CAMBIO_A_BLOB.md` - Este archivo
- ✅ `GUIA_RAPIDA.md` - Actualizada

---

## 🚀 Cómo Usar

### 1. Ejecutar migración
```sql
-- Como usuario FABRICA en SQL Developer
@06_add_image_url_columns.sql
```

### 2. Reiniciar backend
```bash
cd fabrica/backend
mvn clean compile
mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
```

### 3. Probar en frontend
1. Ir a `http://localhost:5173/catalogo`
2. Click en "Nuevo repuesto"
3. Llenar formulario
4. Click en área de imagen
5. Seleccionar archivo JPG/PNG
6. Ver preview inmediato
7. Click "Crear repuesto"
8. Ver notificación de éxito
9. La imagen queda guardada en Oracle como BLOB

---

## 🔍 Verificar en Base de Datos

```sql
-- Ver repuestos con imagen
SELECT part_id, title, 
       CASE WHEN image_data IS NOT NULL THEN 'SÍ' ELSE 'NO' END as tiene_imagen,
       ROUND(DBMS_LOB.GETLENGTH(image_data)/1024, 2) as size_kb,
       image_type
FROM part;

-- Ver tamaño total de imágenes
SELECT 
    ROUND(SUM(DBMS_LOB.GETLENGTH(image_data))/1024/1024, 2) as total_mb
FROM part
WHERE image_data IS NOT NULL;

-- Exportar imagen (desde SQL Developer)
-- Click derecho en celda BLOB > Save As...
SELECT image_data FROM part WHERE part_id = 123;
```

---

## 📚 Documentación Adicional

- **Guía completa**: `IMAGENES_BLOB.md`
- **Guía rápida**: `GUIA_RAPIDA.md`
- **API Reference**: `API_REFERENCE.md`

---

## ✅ Checklist de Verificación

- [x] Migración SQL ejecutada
- [x] Backend compila sin errores
- [x] Entidades actualizadas con BLOB
- [x] Servicios con método `updateImage()`
- [x] Controladores procesan base64
- [x] ImageResource sirve imágenes desde BD
- [x] Frontend lee archivos como base64
- [x] Frontend envía imageData + imageType
- [x] Notificaciones funcionan
- [x] Documentación actualizada

---

## 🎉 ¡Listo!

El sistema ahora almacena imágenes como BLOB en Oracle. No se necesitan archivos en el filesystem, todo está en la base de datos.

**Ventajas principales:**
- 🔒 Más seguro
- 📦 Más simple
- 🔄 Más portable
- ✅ Más consistente
