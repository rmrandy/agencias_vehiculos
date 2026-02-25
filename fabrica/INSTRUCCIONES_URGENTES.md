# 🚨 INSTRUCCIONES URGENTES - Arreglar el error

## ❌ El problema

Ves este error:
```
ORA-00904: "P1_0"."STOCK_QUANTITY": invalid identifier
```

**Causa:** La tabla `PART` en la base de datos NO tiene las columnas de inventario porque no ejecutaste el script de migración.

## ✅ Solución (3 pasos)

### Paso 1: Ejecutar script en DBeaver

1. **Abre DBeaver**
2. **Conecta a la base de datos** (usuario: FABRICA, password: 123)
3. **Abre el archivo:** `EJECUTAR_ESTO_EN_DBEAVER.sql`
4. **Selecciona TODO el contenido** (Cmd+A)
5. **Ejecuta** (Cmd+Enter o botón "Execute SQL Statement")

**Resultado esperado:**
```
Table PART altered.
Comment created.
Comment created.
Comment created.
X rows updated.
Commit complete.
```

Luego verás una tabla con las columnas:
- `part_id`
- `part_number`
- `title`
- `stock_quantity` ← NUEVA
- `low_stock_threshold` ← NUEVA
- `reserved_quantity` ← NUEVA
- `disponible` ← CALCULADA

### Paso 2: Reiniciar el backend

1. **Detén el backend** (Ctrl+C en la terminal donde corre)
2. **Reinicia:**
   ```bash
   cd fabrica/backend
   mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
   ```

### Paso 3: Recargar el frontend

1. En el navegador, presiona **Cmd+Shift+R** (hard reload)
2. Ve a la tienda
3. **Debería funcionar sin errores**

## 🧪 Verificar que funcionó

### En DBeaver:
```sql
-- Verificar que las columnas existen
DESC PART;

-- Ver los datos
SELECT part_id, part_number, stock_quantity, low_stock_threshold
FROM PART;
```

### En el frontend:
1. Ve a la **Tienda**
2. NO deberías ver errores rojos
3. Deberías ver productos con badges de inventario

### En el panel de Catálogo:
1. Ve a **Catálogo** → **Repuestos**
2. Deberías ver columnas "Stock" y "Estado"
3. Crea un producto nuevo con stock = 50
4. Debería aparecer con badge "🟢 Disponible"

## 🔍 Si aún hay problemas

### Error: "ORA-00904: column already exists"

Significa que ya ejecutaste el script antes. Ignora el error y continúa con el Paso 2 (reiniciar backend).

### Error: "Table or view does not exist"

Verifica que estás conectado como usuario **FABRICA**, no como SYS.

### El frontend sigue mostrando error

1. Verifica que el backend se reinició correctamente
2. Busca en los logs del backend si hay errores
3. Recarga el frontend con Cmd+Shift+R

## 📋 Checklist

- [ ] Script ejecutado en DBeaver sin errores
- [ ] Columnas `stock_quantity`, `low_stock_threshold`, `reserved_quantity` existen en tabla PART
- [ ] Backend reiniciado
- [ ] Frontend recargado (hard reload)
- [ ] Tienda carga sin errores
- [ ] Panel de Catálogo muestra columnas de inventario

---

**Una vez completado esto, el sistema funcionará perfectamente.**
