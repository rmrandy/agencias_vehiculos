# Backend API - Agencias Vehículos

Backend REST API desarrollado en Java 17 usando Maven, JAX-RS (Jersey), JPA con Hibernate y Oracle Database.

## Requisitos

- Java 17 o superior
- Maven 3.6+
- Oracle Database (o acceso a una instancia de Oracle)

## Estructura del Proyecto

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/agencias/backend/
│   │   │       ├── Main.java                    # Clase principal que arranca el servidor
│   │   │       ├── config/                      # Configuración
│   │   │       │   ├── DatabaseConfig.java     # Configuración de JPA/Hibernate
│   │   │       │   ├── ConfigLoader.java       # Carga de properties
│   │   │       │   └── JerseyConfig.java       # Configuración de JAX-RS
│   │   │       ├── controller/                  # Recursos REST (JAX-RS)
│   │   │       │   ├── HealthResource.java     # Endpoint /health
│   │   │       │   ├── RepuestoResource.java   # Endpoints CRUD de Repuestos
│   │   │       │   └── ErrorResponse.java      # Modelo de respuesta de errores
│   │   │       ├── model/                       # Entidades JPA
│   │   │       │   └── Repuesto.java           # Entidad Repuesto
│   │   │       ├── repository/                  # Capa de acceso a datos
│   │   │       │   └── RepuestoRepository.java
│   │   │       └── service/                     # Lógica de negocio
│   │   │           └── RepuestoService.java
│   │   └── resources/
│   │       ├── application.properties           # Configuración de la aplicación
│   │       └── META-INF/
│   │           └── persistence.xml              # Configuración de JPA
│   └── test/
└── pom.xml                                       # Configuración Maven
```

## Configuración

### Variables de Entorno

El proyecto puede configurarse mediante variables de entorno o el archivo `application.properties`. Las variables de entorno tienen prioridad sobre el archivo de propiedades.

#### Variables de Base de Datos

```bash
export DB_HOST=localhost
export DB_PORT=1521
export DB_SERVICE=XEPDB1
export DB_USER=SYS
export DB_PASS=123
```

#### Variable de Puerto del Servidor

```bash
export PORT=8081
```

Si no se define `PORT`, el servidor usará el puerto `8080` por defecto.

### Archivo application.properties

El archivo `src/main/resources/application.properties` contiene valores por defecto:

```properties
# Database Configuration
DB_HOST=localhost
DB_PORT=1521
DB_SERVICE=XEPDB1
DB_USER=SYS
DB_PASS=123

# Server Configuration
PORT=8080

# Hibernate Configuration
hibernate.dialect=org.hibernate.dialect.OracleDialect
hibernate.hbm2ddl.auto=update
hibernate.show_sql=true
hibernate.format_sql=true
```

## Compilación y Ejecución

### Compilar el proyecto

```bash
cd fabrica/backend
mvn clean package
```

Esto generará un JAR ejecutable en `target/backend-1.0.0.jar`.

### Ejecutar el proyecto

#### Opción 1: Con variables de entorno

```bash
export PORT=8081
export DB_HOST=localhost
export DB_PORT=1521
export DB_SERVICE=XEPDB1
export DB_USER=SYS
export DB_PASS=123

java -jar target/backend-1.0.0.jar
```

#### Opción 2: Usando valores por defecto de application.properties

```bash
java -jar target/backend-1.0.0.jar
```

#### Opción 3: Ejecutar directamente con Maven

```bash
mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
```

## Endpoints de la API

La API está disponible bajo el prefijo `/api`.

### Health Check

**GET** `/api/health`

Verifica el estado del servidor.

**Respuesta exitosa (200):**
```json
{
  "status": "ok"
}
```

**Ejemplo con curl:**
```bash
curl http://localhost:8080/api/health
```

### Repuestos

#### Crear un repuesto

**POST** `/api/repuestos`

**Body (JSON):**
```json
{
  "nombre": "Filtro de aceite",
  "precio": 25.50
}
```

**Respuesta exitosa (201):**
```json
{
  "id": 1,
  "nombre": "Filtro de aceite",
  "precio": 25.50
}
```

**Ejemplo con curl:**
```bash
curl -X POST http://localhost:8080/api/repuestos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Filtro de aceite","precio":25.50}'
```

#### Obtener todos los repuestos

**GET** `/api/repuestos`

**Respuesta exitosa (200):**
```json
[
  {
    "id": 1,
    "nombre": "Filtro de aceite",
    "precio": 25.50
  },
  {
    "id": 2,
    "nombre": "Pastillas de freno",
    "precio": 45.00
  }
]
```

**Ejemplo con curl:**
```bash
curl http://localhost:8080/api/repuestos
```

#### Obtener un repuesto por ID

**GET** `/api/repuestos/{id}`

**Respuesta exitosa (200):**
```json
{
  "id": 1,
  "nombre": "Filtro de aceite",
  "precio": 25.50
}
```

**Ejemplo con curl:**
```bash
curl http://localhost:8080/api/repuestos/1
```

## Manejo de Errores

La API devuelve respuestas JSON con información de error:

### Error 400 - Bad Request

Cuando los datos enviados son inválidos:

```json
{
  "status": 400,
  "message": "El nombre del repuesto es requerido"
}
```

### Error 404 - Not Found

Cuando un recurso no existe:

```json
{
  "status": 404,
  "message": "Repuesto no encontrado"
}
```

### Error 500 - Internal Server Error

Cuando ocurre un error interno:

```json
{
  "status": 500,
  "message": "Error interno del servidor"
}
```

## Tecnologías Utilizadas

- **Java 17**: Lenguaje de programación
- **Maven**: Gestión de dependencias y construcción
- **JAX-RS (Jersey 3.1.3)**: Framework REST
- **Jetty 11.0.20**: Servidor HTTP embebido
- **JPA (Jakarta Persistence 3.1.0)**: API de persistencia
- **Hibernate 6.4.1**: Proveedor JPA
- **Oracle JDBC (ojdbc11)**: Driver de conexión a Oracle
- **Jackson 2.16.1**: Serialización/deserialización JSON

## Notas Importantes

1. **Base de Datos Oracle**: Asegúrate de que Oracle Database esté corriendo y accesible antes de iniciar la aplicación.

2. **Secuencia de Base de Datos**: La entidad `Repuesto` utiliza una secuencia llamada `REPUESTO_SEQ`. Asegúrate de crearla en Oracle:
   ```sql
   CREATE SEQUENCE REPUESTO_SEQ START WITH 1 INCREMENT BY 1;
   ```

3. **Hibernate DDL**: La configuración `hibernate.hbm2ddl.auto=update` crea/actualiza automáticamente las tablas. Para producción, considera usar `validate` o `none`.

4. **Puerto**: El puerto puede configurarse mediante la variable de entorno `PORT` o el archivo `application.properties`.

## Solución de Problemas

### Error de conexión a la base de datos

Verifica que:
- Oracle Database esté corriendo
- Las credenciales sean correctas
- El servicio/host/puerto sean accesibles
- El driver JDBC esté en el classpath (incluido automáticamente por Maven)

### Puerto ya en uso

Cambia el puerto usando la variable de entorno:
```bash
export PORT=8081
java -jar target/backend-1.0.0.jar
```

### Error al compilar

Asegúrate de tener Java 17 instalado:
```bash
java -version
```

Debe mostrar versión 17 o superior.
