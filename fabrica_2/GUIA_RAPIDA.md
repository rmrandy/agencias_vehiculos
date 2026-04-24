# 🚀 Guía Rápida - Sistema Fábrica

## ✅ Lo que está implementado

### Backend (Java + JAX-RS + JPA + Oracle)
- ✅ Usuarios y roles (ADMIN, REGISTERED, ENTERPRISE)
- ✅ Login/registro con BCrypt
- ✅ **Google reCAPTCHA v3** (invisible, score-based) para registro de usuarios
- ✅ Panel de administración de usuarios
- ✅ Catálogo completo: Categorías, Marcas, Vehículos, Repuestos
- ✅ APIs REST para todo el CRUD
- ✅ CORS configurado
- ✅ Manejo de errores mejorado
- ✅ **Almacenamiento de imágenes como BLOB** en Oracle
- ✅ Upload de imágenes (JPG, PNG, GIF, WEBP, máx 5MB)
- ✅ Endpoint para servir imágenes desde BD

### Frontend (Vue 3 + Vue Router)
- ✅ Login y registro
- ✅ **Google reCAPTCHA v3 invisible** en registro (sin widget visible)
- ✅ Dashboard
- ✅ Panel de usuarios (asignar roles)
- ✅ Panel de catálogo (gestionar categorías, marcas, repuestos)
- ✅ Navbar con enlaces según rol
- ✅ Protección de rutas
- ✅ **Sistema de notificaciones toast animadas**
- ✅ **Componente de upload de imágenes con preview**
- ✅ **Imágenes se envían como base64 y se guardan como BLOB**

---

## 🎬 Inicio rápido

### 1. Configurar Oracle

Como **SYS** (SYSDBA):
```sql
@00_grant_all_fabrica.sql
```

Como **FABRICA**:
```sql
@02_roles_data.sql
@06_add_image_url_columns.sql  -- Soporte de imágenes
@05_datos_dummy.sql
```

Si tienes problemas con columnas duplicadas (USERID/USER_ID), ejecuta el trigger:
```sql
@04_fix_app_user_columns.sql  -- opción C (trigger)
```

### 2. Iniciar backend

```bash
cd fabrica/backend
mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
```

Backend en: `http://localhost:8080`

### 3. Iniciar frontend

```bash
cd fabrica/frontend
npm run dev
```

Frontend en: `http://localhost:5173`

### 4. Crear primer usuario (ADMIN)

1. Ve a `http://localhost:5173/register`
2. Completa el formulario
3. Haz clic en "Registrarme" (reCAPTCHA v3 se ejecuta automáticamente en segundo plano)
4. Serás redirigido al Dashboard como ADMIN
5. Verás enlaces a **Usuarios** y **Catálogo** en el navbar

---

## 📊 Datos de prueba incluidos

Después de ejecutar `05_datos_dummy.sql` tendrás:

**6 Categorías:**
- Motor
- Transmisión
- Frenos
- Suspensión
- Eléctrico
- Filtros

**6 Marcas:**
- Bosch
- Denso
- NGK
- Brembo
- Mann Filter
- Monroe

**5 Vehículos:**
- Toyota Camry 2020
- Honda Civic 2019
- Ford F-150 2021
- Chevrolet Silverado 2018
- Nissan Altima 2020

**8 Repuestos:**
- Filtro de aceite Mann Filter ($15.99)
- Pastillas de freno Brembo ($89.99)
- Bujías NGK ($32.50)
- Amortiguador Monroe ($125.00)
- Alternador Bosch ($245.00)
- Sensor O2 Denso ($67.50)
- Filtro de aire Mann Filter ($22.99)
- Discos de freno Brembo ($189.99)

---

## 🔑 Funcionalidades del panel de admin

### Panel de Usuarios
- Ver lista de usuarios
- Ver roles de cada usuario
- Asignar/cambiar roles (modal con checkboxes)
- **Notificaciones animadas** al actualizar roles

### Panel de Catálogo
- **Pestaña Repuestos**: Listar repuestos, crear nuevos con imagen
- **Pestaña Categorías**: Listar categorías, crear nuevas (con categoría padre opcional e imagen)
- **Pestaña Marcas**: Listar marcas, crear nuevas con imagen
- **Upload de imágenes**: Click para seleccionar, preview en tiempo real, validación de formato y tamaño
- **Imágenes como BLOB**: Se almacenan directamente en Oracle, no en archivos
- **Notificaciones animadas** al crear categorías, marcas o repuestos

---

## 🌐 APIs disponibles

Ver documentación completa en **`API_REFERENCE.md`**

**Principales endpoints:**

```bash
# Autenticación
POST /api/auth/login

# Usuarios
POST /api/usuarios (registro)
GET  /api/usuarios (listar - admin)
PUT  /api/usuarios/{id}/roles (asignar roles - admin)

# Imágenes
GET  /api/images/{entityType}/{id} (obtener imagen desde BD)
POST /api/images/validate (validar imagen base64)

# Catálogo (todos soportan imageData + imageType como BLOB)
GET  /api/categorias
POST /api/categorias
GET  /api/marcas
POST /api/marcas
GET  /api/vehiculos
POST /api/vehiculos
GET  /api/repuestos
POST /api/repuestos
GET  /api/repuestos/numero/{partNumber}
GET  /api/repuestos?categoryId=1
GET  /api/repuestos?brandId=2
```

---

## 🐛 Troubleshooting

### Puerto 8080 ocupado
```bash
lsof -i :8080
kill <PID>
```

### ORA-01045: lacks CREATE SESSION
```bash
# Como SYS:
GRANT CREATE SESSION TO fabrica;
```

### ORA-01950: no privileges on tablespace
```bash
# Como SYS:
ALTER USER fabrica QUOTA UNLIMITED ON users;
```

### ORA-01400: cannot insert NULL into USERID/USER_ID
Ejecutar el trigger en `04_fix_app_user_columns.sql` (opción C)

### Columnas ROLE_ID y ROLEID
El script `02_roles_data.sql` ya maneja ambas columnas usando NEXTVAL y CURRVAL

---

## 📁 Estructura de archivos importantes

```
fabrica/
├── backend/
│   ├── src/main/java/com/agencias/backend/
│   │   ├── model/          # Entidades JPA
│   │   ├── repository/     # Acceso a datos
│   │   ├── service/        # Lógica de negocio
│   │   ├── controller/     # APIs REST
│   │   └── config/         # Configuración (CORS, DB, Jersey)
│   └── src/main/resources/
│       ├── application.properties
│       └── META-INF/persistence.xml
├── frontend/
│   └── src/
│       ├── views/          # Vistas (Login, Register, Usuarios, Catalogo)
│       ├── components/     # Navbar
│       ├── api/            # Cliente API
│       ├── composables/    # useAuth
│       └── router/         # Vue Router
├── database/
│   ├── 00_grant_all_fabrica.sql      # Permisos
│   ├── 01_create_user.sql            # Crear usuario
│   ├── 02_roles_data.sql             # Roles
│   ├── 04_fix_app_user_columns.sql   # Trigger USERID/USER_ID
│   ├── 05_datos_dummy.sql            # Datos de prueba
│   └── schema.sql                    # DDL completo
├── API_REFERENCE.md        # Documentación de APIs
└── GUIA_RAPIDA.md         # Este archivo
```

---

## 🎯 Próximos pasos sugeridos

1. ✅ **Usuarios y catálogo** - Completado
2. 🔄 **Inventario**: Entidades INVENTORY_MOVEMENT y PART_STOCK
3. 🔄 **Pedidos**: ORDER_HEADER, ORDER_ITEM, ORDER_STATUS_HISTORY
4. 🔄 **Pagos**: PAYMENT
5. 🔄 **Reviews**: PART_REVIEW (multinivel)
6. 🔄 **Portal web público**: Vistas para clientes (catálogo, carrito, checkout)
7. 🔄 **API empresarial**: Autenticación por api_key, endpoints para pedidos B2B
8. 🔄 **Reportes**: Evaluación de mercado, ventas, analytics

---

## 💡 Tips

- **Primer usuario**: Siempre es ADMIN
- **Panel de admin**: Solo visible si tienes rol ADMIN
- **Datos dummy**: Ejecuta `05_datos_dummy.sql` para tener datos de prueba
- **CORS**: Ya configurado para funcionar con frontend en cualquier puerto
- **Errores detallados**: El backend devuelve mensajes específicos en los 500
