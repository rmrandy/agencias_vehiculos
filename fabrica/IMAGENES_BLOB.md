# 🖼️ Sistema de Imágenes con BLOB

## Descripción

Las imágenes se almacenan directamente en la base de datos Oracle como **BLOB** (Binary Large Object), no como archivos en el sistema de archivos ni URLs externas.

---

## 📊 Estructura de Base de Datos

### Columnas agregadas a cada tabla

```sql
-- PART (Repuestos)
ALTER TABLE part ADD image_data BLOB;
ALTER TABLE part ADD image_type VARCHAR2(50);

-- CATEGORY (Categorías)
ALTER TABLE category ADD image_data BLOB;
ALTER TABLE category ADD image_type VARCHAR2(50);

-- BRAND (Marcas)
ALTER TABLE brand ADD image_data BLOB;
ALTER TABLE brand ADD image_type VARCHAR2(50);

-- VEHICLE (Vehículos)
ALTER TABLE vehicle ADD image_data BLOB;
ALTER TABLE vehicle ADD image_type VARCHAR2(50);
```

**Campos:**
- `image_data`: BLOB que contiene los bytes de la imagen
- `image_type`: VARCHAR2 que almacena el MIME type (ej: `image/jpeg`, `image/png`)

---

## 🔄 Flujo de Datos

### 1. Frontend → Backend (Subir imagen)

**Frontend:**
```javascript
// Usuario selecciona imagen
const file = event.target.files[0]

// Se lee como base64
const reader = new FileReader()
reader.onload = (e) => {
  const base64Data = e.target.result
  // Formato: "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
  
  // Se envía al backend
  emit('update:modelValue', {
    imageData: base64Data,
    imageType: file.type // "image/jpeg"
  })
}
reader.readAsDataURL(file)
```

**Backend recibe:**
```json
{
  "name": "Filtro de aceite",
  "categoryId": 1,
  "brandId": 2,
  "imageData": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "imageType": "image/jpeg"
}
```

**Backend procesa:**
```java
// Remover prefijo "data:image/jpeg;base64,"
if (base64Data.contains(",")) {
    base64Data = base64Data.split(",")[1];
}

// Decodificar base64 a bytes
byte[] imageBytes = Base64.getDecoder().decode(base64Data);

// Guardar en entidad
part.setImageData(imageBytes);
part.setImageType("image/jpeg");

// JPA persiste en Oracle como BLOB
em.persist(part);
```

---

### 2. Backend → Frontend (Mostrar imagen)

**Opción A: Endpoint dedicado**
```
GET /api/images/{entityType}/{id}

Ejemplo:
GET /api/images/part/123
GET /api/images/category/5
GET /api/images/brand/8
```

**Backend responde:**
```java
// Obtener entidad
Part part = em.find(Part.class, id);
byte[] imageData = part.getImageData();
String imageType = part.getImageType();

// Retornar bytes directamente
return Response.ok(imageData)
    .type(imageType) // Content-Type: image/jpeg
    .header("Cache-Control", "max-age=86400")
    .build();
```

**Frontend usa:**
```html
<img :src="`/api/images/part/${partId}`" alt="Repuesto" />
```

**Opción B: Incluir base64 en JSON**
```json
{
  "partId": 123,
  "title": "Filtro de aceite",
  "imageData": "/9j/4AAQSkZJRg...",
  "imageType": "image/jpeg"
}
```

**Frontend usa:**
```html
<img :src="`data:${part.imageType};base64,${part.imageData}`" alt="Repuesto" />
```

---

## ✅ Ventajas del Almacenamiento BLOB

### 1. **Simplicidad**
- ✅ No hay sistema de archivos que gestionar
- ✅ No hay rutas relativas/absolutas
- ✅ No hay problemas de permisos de archivos
- ✅ Backup de BD incluye las imágenes

### 2. **Integridad**
- ✅ Transacciones ACID: si falla el insert, no queda imagen huérfana
- ✅ Relaciones FK garantizan consistencia
- ✅ DELETE CASCADE elimina imagen automáticamente

### 3. **Seguridad**
- ✅ Control de acceso a nivel de BD
- ✅ No hay acceso directo al filesystem
- ✅ Encriptación de BD protege las imágenes

### 4. **Portabilidad**
- ✅ Fácil migrar entre servidores (solo BD)
- ✅ No hay rutas hardcodeadas
- ✅ Funciona en contenedores sin volúmenes

---

## ⚠️ Consideraciones

### Tamaño
- **Límite actual**: 5MB por imagen
- **Oracle BLOB**: Soporta hasta 4GB por campo
- **Recomendación**: Comprimir imágenes antes de subir

### Performance
- **Lectura**: Ligeramente más lenta que filesystem
- **Cache**: Usar `Cache-Control` headers
- **Índices**: No indexar columnas BLOB

### Escalabilidad
- **Hasta ~10,000 imágenes**: BLOB es óptimo
- **Más de 100,000 imágenes**: Considerar CDN
- **Solución híbrida**: BLOB para thumbnails, CDN para originales

---

## 🔧 Configuración

### 1. Ejecutar migración
```sql
-- Como usuario FABRICA
@06_add_image_url_columns.sql
```

### 2. Validaciones en Backend
```java
private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

private boolean isValidImageType(String imageType) {
    return imageType.equals("image/jpeg") || 
           imageType.equals("image/jpg") ||
           imageType.equals("image/png") || 
           imageType.equals("image/gif") ||
           imageType.equals("image/webp");
}
```

### 3. Validaciones en Frontend
```javascript
// Tamaño
if (file.size > 5 * 1024 * 1024) {
  error.value = 'El archivo excede el tamaño máximo de 5MB'
  return
}

// Tipo
const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
if (!validTypes.includes(file.type)) {
  error.value = 'Formato no válido. Use: JPG, PNG, GIF o WEBP'
  return
}
```

---

## 📝 Ejemplo Completo

### Crear repuesto con imagen

**1. Frontend - Seleccionar imagen**
```vue
<ImageUpload v-model="repuestoForm.imageData" />
```

**2. Frontend - Enviar formulario**
```javascript
const payload = {
  categoryId: 1,
  brandId: 2,
  partNumber: "ABC-123",
  title: "Filtro de aceite",
  price: 15.99,
  imageData: "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  imageType: "image/jpeg"
}

await createRepuesto(payload)
```

**3. Backend - Procesar**
```java
// Decodificar base64
String base64Data = (String) body.get("imageData");
if (base64Data.contains(",")) {
    base64Data = base64Data.split(",")[1];
}
byte[] imageBytes = Base64.getDecoder().decode(base64Data);

// Crear repuesto
Part part = service.create(...);
part.setImageData(imageBytes);
part.setImageType("image/jpeg");
service.update(part.getPartId(), part);
```

**4. Base de datos**
```sql
SELECT part_id, title, 
       DBMS_LOB.GETLENGTH(image_data) as image_size,
       image_type
FROM part
WHERE part_id = 123;

-- Resultado:
-- PART_ID | TITLE            | IMAGE_SIZE | IMAGE_TYPE
-- 123     | Filtro de aceite | 245678     | image/jpeg
```

**5. Frontend - Mostrar imagen**
```vue
<img :src="`/api/images/part/${part.partId}`" alt="Repuesto" />
```

---

## 🚀 APIs

### GET /api/images/{entityType}/{id}
Obtiene la imagen de una entidad.

**Parámetros:**
- `entityType`: `part`, `category`, `brand`, `vehicle`
- `id`: ID de la entidad

**Respuesta:**
- Content-Type: `image/jpeg`, `image/png`, etc.
- Body: Bytes de la imagen
- Headers: `Cache-Control: max-age=86400`

**Ejemplo:**
```bash
curl http://localhost:8080/api/images/part/123 --output filtro.jpg
```

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

## 🔍 Consultas Útiles

### Ver tamaño de imágenes
```sql
SELECT 
    'PART' as tabla,
    COUNT(*) as con_imagen,
    ROUND(AVG(DBMS_LOB.GETLENGTH(image_data))/1024, 2) as promedio_kb,
    ROUND(SUM(DBMS_LOB.GETLENGTH(image_data))/1024/1024, 2) as total_mb
FROM part
WHERE image_data IS NOT NULL
UNION ALL
SELECT 
    'CATEGORY',
    COUNT(*),
    ROUND(AVG(DBMS_LOB.GETLENGTH(image_data))/1024, 2),
    ROUND(SUM(DBMS_LOB.GETLENGTH(image_data))/1024/1024, 2)
FROM category
WHERE image_data IS NOT NULL;
```

### Exportar imagen
```sql
-- Desde SQL Developer, click derecho en la celda BLOB > Save As...
SELECT image_data FROM part WHERE part_id = 123;
```

### Limpiar imágenes
```sql
-- Eliminar imagen de un repuesto específico
UPDATE part SET image_data = NULL, image_type = NULL WHERE part_id = 123;

-- Eliminar todas las imágenes de repuestos inactivos
UPDATE part SET image_data = NULL, image_type = NULL WHERE active = 0;
```

---

## 📚 Referencias

- [Oracle BLOB Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/BLOB.html)
- [JPA @Lob Annotation](https://docs.oracle.com/javaee/7/api/javax/persistence/Lob.html)
- [Base64 Encoding](https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL)
