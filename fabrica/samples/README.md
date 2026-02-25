# Archivos JSON para probar importación

## 1. Importar repuestos (`repuestos-import-ejemplo.json`)

Formato aceptado por **Importar JSON** en Catálogo:

- **Opción A:** Array directo de objetos:
  ```json
  [
    { "partNumber": "OBLIGATORIO", "title": "...", ... },
    ...
  ]
  ```
- **Opción B:** Objeto con propiedad `items`:
  ```json
  {
    "items": [
      { "partNumber": "OBLIGATORIO", "title": "...", ... },
      ...
    ]
  }
  ```

Campos por repuesto:

| Campo             | Obligatorio | Descripción                          |
|-------------------|------------|--------------------------------------|
| `partNumber`     | Sí         | Código único. Si existe, se actualiza; si no, se crea. |
| `title`          | No         | Nombre del repuesto                  |
| `description`    | No         | Descripción / especificaciones       |
| `categoryId`      | No*        | ID de categoría (*necesario al crear si no hay default) |
| `brandId`        | No*        | ID de marca (*idem)                  |
| `price`          | No         | Precio (número)                      |
| `weightLb`       | No         | Peso en libras                       |
| `stockQuantity`  | No         | Cantidad en stock                    |
| `lowStockThreshold` | No      | Umbral de “bajo stock” (default 5)   |
| `active`         | No         | 1 = activo, 0 = inactivo             |

---

## 2. Importar inventario (`inventario-import-ejemplo.json`)

Formato aceptado por **Importar inventario** en Catálogo:

- Array de objetos con `partNumber` y `stockQuantity` (o objeto con propiedad `items` igual a ese array).

Campos por fila:

| Campo           | Obligatorio | Descripción                                  |
|-----------------|------------|----------------------------------------------|
| `partNumber`    | Sí         | Código del repuesto (debe existir en BD)     |
| `stockQuantity` | Sí         | Nueva cantidad en stock (entero ≥ 0)         |

Si un `partNumber` no existe, esa fila se registra como error y el resto se procesa.

---

## Cómo probar

1. **Importar repuestos:** Catálogo → Repuestos → “Importar JSON” → elegir `repuestos-import-ejemplo.json`.
2. **Importar inventario:** Catálogo → Repuestos → “Importar inventario” → elegir `inventario-import-ejemplo.json`.  
   En el ejemplo, la fila con `CODIGO-INEXISTENTE` fallará a propósito para ver el detalle de errores.

Ajusta `partNumber`, `categoryId` y `brandId` a los que existan en tu base antes de importar.
