# Base de datos - Sistema Fábrica (fabrica_2)

El backend en **`fabrica_2`** apunta al **esquema FABRICA2** (usuario `FABRICA2`, contraseña `123`). La conexión se configura en `backend/src/main/resources/application.properties` y puede sobrescribirse con variables de entorno.

## Cómo crear la base de datos

### 1. Crear el usuario FABRICA2 (solo una vez)

Conéctate como **SYS** (o un DBA) con rol SYSDBA y ejecuta:

```bash
# En DBeaver: abrir 01_create_user.sql y ejecutar con la conexión SYS.
# O por línea de comandos:
sqlcl sys/password@localhost:1521/XEPDB1 as sysdba @01_create_user.sql
```

Eso crea el usuario `FABRICA2` con contraseña `123` y permisos para crear tablas y secuencias.

### 2. Conectar a Oracle como FABRICA2

En DBeaver (o tu cliente) crea una conexión:

- **Host / Puerto / Service:** los mismos que usa el backend (ej. localhost, 1521, XEPDB1)
- **Usuario:** `FABRICA2`
- **Contraseña:** `123`

### 3. Ejecutar el DDL del esquema

**Opción A – DBeaver**

1. Conéctate con el usuario **FABRICA2** (contraseña `123`).
2. Abre `schema.sql` y ejecútalo completo en esa conexión.

**Opción B – Línea de comandos**

```bash
cd fabrica_2/database
sqlcl FABRICA2/123@localhost:1521/XEPDB1 @schema.sql
# o
sqlplus FABRICA2/123@localhost:1521/XEPDB1 @schema.sql
```

### 4. Verificar

```sql
SELECT table_name FROM user_tables ORDER BY table_name;
SELECT * FROM role;
```

---

## Backend (fabrica_2)

Valores por defecto en `application.properties`:

- `DB_USER=FABRICA2`
- `DB_PASS=123`

Variables de entorno opcionales: `DB_HOST`, `DB_PORT`, `DB_SERVICE`, `DB_USER`, `DB_PASS`.

---

## Error ORA-01400 en `APP_USER` (`USERID` o `USER_ID` NULL)

Si Hibernate creó la tabla con **`hibernate.hbm2ddl.auto=update`**, puede existir la columna duplicada **`USERID`** además de **`USER_ID`**. El backend solo rellena `USER_ID` y Oracle falla al exigir `USERID`.

**Solución:** ejecuta `10_app_user_drop_redundant_userid.sql` (puede ser como **FABRICA2** o como **SYS**). El script usa **`ALL_TAB_COLUMNS`** y el nombre calificado **`FABRICA2.APP_USER`**, así detecta la columna aunque tu sesión de DBeaver no sea el esquema FABRICA2 (antes, con `USER_TAB_COLUMNS` desde SYS salía “no tiene USERID” por error).

Si tu esquema no se llama `FABRICA2`, edita la constante `v_schema` dentro del bloque PL/SQL del script.

---

## Scripts opcionales

Los comentarios en scripts SQL pueden decir "FABRICA"; en **fabrica_2** debes ejecutarlos conectado como **FABRICA2** (mismo esquema, otro nombre de usuario).

- `02_roles_data.sql`, `05_datos_dummy.sql`, etc.: ejecutar como **FABRICA2** si aplica.
