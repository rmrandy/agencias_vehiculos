# Documentación — Backend Fábrica

API REST del sistema **fábrica** en **Java 17**, **Maven**, **JAX-RS (Jersey)**, **JPA/Hibernate** y base **Oracle**. El servidor embebido es **Jetty**; los recursos REST se publican bajo **`/api/*`**.

---

## Stack y requisitos

| Componente | Notas |
|------------|--------|
| Java | 17+ |
| Build | Maven (`pom.xml` en `fabrica/backend`) |
| REST | Jersey 3.x (`JerseyConfig` registra paquetes y filtros) |
| Persistencia | JPA + Hibernate → Oracle |

---

## Arranque

Compilar:

```bash
cd fabrica/backend
mvn clean package
```

Ejecutar (ajustar `PORT` y variables de BD según tu entorno):

```bash
export PORT=8080
java -jar target/backend-1.0.0.jar
```

O con Maven:

```bash
mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
```

Health check:

```bash
curl http://localhost:8080/api/health
```

---

## Arquitectura en código

### Punto de entrada: Jetty + Jersey

`Main.java` carga propiedades, inicializa el `EntityManagerFactory`, crea Jetty y monta Jersey en el path `/api/*`:

```java
public class Main {
    public static void main(String[] args) {
        try {
            Properties props = ConfigLoader.loadProperties();
            String portEnv = System.getenv("PORT");
            int port = portEnv != null ? Integer.parseInt(portEnv)
                      : Integer.parseInt(props.getProperty("PORT", "8080"));

            DatabaseConfig.getEntityManagerFactory();

            JerseyConfig jerseyConfig = new JerseyConfig();
            Server server = new Server(port);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(jerseyConfig));
            jerseyServlet.setInitOrder(0);
            context.addServlet(jerseyServlet, "/api/*");

            server.setHandler(context);
            server.start();
            // ...
            server.join();
        } catch (Exception e) {
            // ...
        }
    }
}
```

(Fuente: `fabrica/backend/src/main/java/com/agencias/backend/Main.java` — revisar el archivo completo por el `shutdown hook` y los `println` de arranque.)

### Registro de recursos JAX-RS

`JerseyConfig` fija `@ApplicationPath("/api")`, escanea el paquete `com.agencias.backend.controller` y registra Jackson, multipart, CORS y filtro de API key para distribuidores:

```java
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        packages("com.agencias.backend.controller");
        register(JacksonFeature.class);
        register(MultiPartFeature.class);
        register(CorsFilter.class);
        register(DistributorApiKeyFilter.class);
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
    }
}
```

Las clases anotadas con `@Path` bajo ese paquete quedan disponibles como **`/api` + path del recurso**. Por ejemplo, `@Path("/auth")` en `AuthResource` → **`/api/auth/...`**.

### Ejemplo de recurso: autenticación

```java
@Path("/auth")
@jakarta.inject.Singleton
public class AuthResource {
    private final UserService userService;

    public AuthResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.userService = new UserService(emf);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest req) {
        if (req == null || req.getEmail() == null || req.getPassword() == null) {
            return Response.status(400).entity(new ErrorResponse(400, "Email y contraseña son obligatorios")).build();
        }
        AppUser user = userService.login(req.getEmail(), req.getPassword());
        if (user == null) {
            return Response.status(401).entity(new ErrorResponse(401, "Credenciales incorrectas")).build();
        }
        return Response.ok(UsuarioResponse.from(user)).build();
    }
}
```

(Fuente: `fabrica/backend/src/main/java/com/agencias/backend/controller/AuthResource.java`.)

### Ejemplo de recurso: repuestos

```java
@Path("/repuestos")
@jakarta.inject.Singleton
public class PartResource {
    private final PartService service;

    public PartResource() {
        EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
        this.service = new PartService(emf, new MailService());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Map<String, Object> body) {
        // categoryId, brandId, partNumber, title, price, imagen en base64, etc.
        // ...
    }
    // GET, PUT, DELETE según implementación
}
```

(Fuente: `fabrica/backend/src/main/java/com/agencias/backend/controller/PartResource.java`.)

---

## Recursos principales (paquete `controller`)

Entre otros, el proyecto incluye recursos para:

- `HealthResource`, `ApiResource`, `DbResource` — salud e información  
- `AuthResource`, `UsuarioResource`, `RoleResource` — usuarios y roles  
- `CategoryResource`, `BrandResource`, `VehicleResource`, `PartResource` — catálogo  
- `OrderResource` — pedidos y flujo de estados  
- `ImageResource`, `PartReviewResource` — multimedia y reseñas  
- `ReporteriaResource` — reportería  

La documentación ampliada de endpoints, variables `DB_*`, ejemplos JSON y notas de Oracle está en `fabrica/backend/README.md`.

---

## Configuración

Variables típicas (también en `application.properties`):

```bash
export DB_HOST=localhost
export DB_PORT=1521
export DB_SERVICE=XEPDB1
export DB_USER=...
export DB_PASS=...
export PORT=8080
```

---

## Referencias en el repositorio

- `fabrica/backend/src/main/java/com/agencias/backend/Main.java`  
- `fabrica/backend/src/main/java/com/agencias/backend/config/JerseyConfig.java`  
- `fabrica/backend/src/main/java/com/agencias/backend/controller/*.java`  
- `fabrica/backend/src/main/java/com/agencias/backend/service/*.java`  
- `fabrica/backend/README.md` — guía detallada de API y despliegue  
