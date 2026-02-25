# API Reference - Sistema Fábrica

Base URL: `http://localhost:8080/api`

## Autenticación

### Login
**POST** `/auth/login`

Body:
```json
{
  "email": "admin@ejemplo.com",
  "password": "miPassword123"
}
```

Respuesta (200):
```json
{
  "userId": 1,
  "email": "admin@ejemplo.com",
  "fullName": "Administrador",
  "phone": "+34 600 000 000",
  "status": "ACTIVE",
  "createdAt": "2026-02-13T00:00:00Z",
  "roles": ["ADMIN"]
}
```

---

## Usuarios

### Registrar usuario
**POST** `/usuarios`

Body:
```json
{
  "email": "nuevo@ejemplo.com",
  "password": "password123",
  "fullName": "Usuario Nuevo",
  "phone": "+34 600 111 222"
}
```

El primer usuario recibe rol **ADMIN**; el resto **REGISTERED**.

### Listar usuarios (admin)
**GET** `/usuarios`

### Obtener usuario por ID
**GET** `/usuarios/{id}`

### Asignar roles (admin)
**PUT** `/usuarios/{id}/roles`

Headers:
- `X-Admin-User-Id`: ID del usuario administrador

Body:
```json
{
  "roleIds": [1, 3]
}
```

### Listar roles
**GET** `/roles`

---

## Catálogo

### Categorías

**POST** `/categorias` - Crear
```json
{
  "name": "Motor",
  "parentId": null
}
```

**GET** `/categorias` - Listar todas  
**GET** `/categorias/{id}` - Obtener por ID  
**PUT** `/categorias/{id}` - Actualizar  
**DELETE** `/categorias/{id}` - Eliminar

---

### Marcas

**POST** `/marcas` - Crear
```json
{
  "name": "Bosch"
}
```

**GET** `/marcas` - Listar todas  
**GET** `/marcas/{id}` - Obtener por ID  
**PUT** `/marcas/{id}` - Actualizar  
**DELETE** `/marcas/{id}` - Eliminar

---

### Vehículos

**POST** `/vehiculos` - Crear
```json
{
  "universalVehicleCode": "UVC-TOY-CAM-2020",
  "make": "Toyota",
  "line": "Camry",
  "yearNumber": 2020
}
```

**GET** `/vehiculos` - Listar todos  
**GET** `/vehiculos/{id}` - Obtener por ID  
**PUT** `/vehiculos/{id}` - Actualizar  
**DELETE** `/vehiculos/{id}` - Eliminar

---

### Repuestos

**POST** `/repuestos` - Crear
```json
{
  "categoryId": 1,
  "brandId": 1,
  "partNumber": "ABC-123",
  "title": "Filtro de aceite",
  "description": "Filtro de alta eficiencia",
  "weightLb": 0.5,
  "price": 25.99
}
```

**GET** `/repuestos` - Listar todos (activos)  
**GET** `/repuestos?categoryId=1` - Filtrar por categoría  
**GET** `/repuestos?brandId=2` - Filtrar por marca  
**GET** `/repuestos/{id}` - Obtener por ID  
**GET** `/repuestos/numero/{partNumber}` - Buscar por número de parte  
**PUT** `/repuestos/{id}` - Actualizar
```json
{
  "title": "Nuevo título",
  "price": 29.99,
  "active": 1
}
```

**DELETE** `/repuestos/{id}` - Eliminar

---

## Health Check

**GET** `/health`

Respuesta:
```json
{
  "status": "ok"
}
```

---

## Códigos de respuesta

- **200** OK - Operación exitosa
- **201** Created - Recurso creado
- **204** No Content - Eliminación exitosa
- **400** Bad Request - Datos inválidos
- **401** Unauthorized - Credenciales incorrectas
- **403** Forbidden - Sin permisos
- **404** Not Found - Recurso no encontrado
- **500** Internal Server Error - Error del servidor

Formato de error:
```json
{
  "status": 400,
  "message": "Descripción del error"
}
```
