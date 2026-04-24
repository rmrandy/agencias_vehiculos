# Base de datos - Sistema Fábrica

El backend apunta al **esquema FABRICA** (usuario `FABRICA`, contraseña `123`). Scripts DDL para Oracle según el modelo entidad-relación del documento.

## Cómo crear la base de datos

### 1. Crear el usuario FABRICA (solo una vez)

Conéctate como **SYS** (o un DBA) con rol SYSDBA y ejecuta:

```bash
# En DBeaver: abrir 01_create_user.sql y ejecutar con la conexión SYS.
# O por línea de comandos:
sqlcl sys/password@localhost:1521/XEPDB1 as sysdba @01_create_user.sql
```

Eso crea el usuario `FABRICA` con contraseña `123` y permisos para crear tablas y secuencias.

### 2. Conectar a Oracle como FABRICA

En DBeaver (o tu cliente) crea una conexión:

- **Host / Puerto / Service:** los mismos que usa el backend (ej. localhost, 1521, XEPDB1)
- **Usuario:** `FABRICA`
- **Contraseña:** `123`

### 3. Ejecutar el DDL del esquema

**Opción A – DBeaver**

1. Conéctate con el usuario **FABRICA** (contraseña `123`).
2. Abre `schema.sql` y ejecútalo completo en esa conexión.

**Opción B – Línea de comandos**

```bash
cd fabrica/database
sqlcl fabrica/123@localhost:1521/XEPDB1 @schema.sql
# o
sqlplus fabrica/123@localhost:1521/XEPDB1 @schema.sql
```

### 3. Verificar

Después de ejecutar deberías tener:

- **Tablas:** `role`, `app_user`, `user_role`, `user_address`, `enterprise_profile`, `category`, `brand`, `vehicle`, `part`, `part_image`, `part_tech_spec`, `part_compatibility`, `inventory_movement`, `part_stock`, `order_header`, `order_item`, `order_status_history`, `payment`, `part_review`, `json_operation_log`, `market_feed_config`, `market_sales_snapshot`, `part_number_map`.
- **Secuencias:** una por cada PK (p. ej. `app_user_seq`, `part_seq`, etc.).
- **Datos iniciales:** 3 filas en `role` (ADMIN, REGISTERED, ENTERPRISE).
- **Empresariales:** la tabla `enterprise_profile` incluye la columna `api_key` para acceso por API.

**Scripts opcionales** (si el esquema ya existía sin datos o sin `api_key`):

- `02_roles_data.sql`: inserta los tres roles (ADMIN, REGISTERED, ENTERPRISE) si no existen.
- `03_add_api_key_enterprise.sql`: añade la columna `api_key` a `enterprise_profile` si faltaba.

Consulta en tu cliente:

```sql
SELECT table_name FROM user_tables ORDER BY table_name;
SELECT * FROM role;
```

---

## Orden recomendado para trabajar

1. **Crear el esquema en Oracle**  
   Ejecutar `schema.sql` como en el apartado anterior.

2. **Ajustar el backend Java**  
   - Añadir entidades JPA que mapeen a estas tablas (sustituir o complementar la entidad de ejemplo `Repuesto`).
   - Usar las mismas secuencias (p. ej. `part_seq` para `PART`) en las entidades.
   - Mantener `hibernate.hbm2ddl.auto=validate` (o `none`) para no pisar el DDL que ya creaste.

3. **Ir por módulos**  
   - Primero: catálogo (`Category`, `Brand`, `Vehicle`, `Part`, `PartImage`, `PartTechSpec`, `PartCompatibility`).
   - Luego: usuarios (`AppUser`, `Role`, `UserRole`, `UserAddress`, `EnterpriseProfile`).
   - Después: inventario (`InventoryMovement`, `PartStock`), pedidos (`OrderHeader`, `OrderItem`, etc.), reviews, logs y reportes de mercado.

---

## Tipos de usuario (roles)

| Rol         | Uso |
|------------|-----|
| **ADMIN**  | Administrador: puede listar usuarios y asignar roles. El primer usuario registrado recibe este rol. |
| **REGISTERED** | Usuario normal (registro desde la web). |
| **ENTERPRISE** | Usuario empresarial: tiene perfil en `enterprise_profile` y puede usar `api_key` para acceso por API. |

El backend crea automáticamente los tres roles si no existen (al listar roles o al registrar el primer usuario). Para usuarios ENTERPRISE se usa la tabla `enterprise_profile`, que incluye el campo **`api_key`** para autenticación en llamadas API.

---

## Notas del modelo

- La tabla de usuarios se llama **`app_user`** (en el DDL) porque `USER` es palabra reservada en Oracle.
- **`year_number`** en `vehicle`: el nombre de columna evita la reservada `YEAR`.
- **Pagos:** no se guarda tarjeta en claro; se usa `default_card_token` / tokenización según el documento.
- **Inventario:** la trazabilidad está en `inventory_movement`; `part_stock` es el resumen para consultas rápidas y alertas (p. ej. ≤ 5 unidades).

Si quieres, el siguiente paso puede ser definir juntos la primera entidad JPA (por ejemplo `Part` o `Category`) y el repositorio/servicio en el backend de la fábrica.
