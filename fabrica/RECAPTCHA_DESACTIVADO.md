# ✅ reCAPTCHA Desactivado

## 📋 Cambios realizados

Se ha desactivado completamente el sistema de Google reCAPTCHA del registro de usuarios.

### Frontend

**1. `index.html`**
- ❌ Eliminado script de Google reCAPTCHA

**2. `Register.vue`**
- ❌ Eliminado import de `onMounted`
- ❌ Eliminada variable `recaptchaReady`
- ❌ Eliminada variable `RECAPTCHA_SITE_KEY`
- ❌ Eliminado código `onMounted()` que esperaba a reCAPTCHA
- ❌ Eliminada validación de `recaptchaReady` en `onSubmit()`
- ❌ Eliminada llamada a `window.grecaptcha.execute()`
- ❌ Eliminado campo `recaptchaToken` del payload
- ❌ Eliminado botón deshabilitado por reCAPTCHA
- ❌ Eliminado texto "Cargando..." del botón
- ❌ Eliminado aviso legal de reCAPTCHA
- ❌ Eliminados estilos `.recaptcha-notice`

**Código antes:**
```javascript
const recaptchaToken = await window.grecaptcha.execute(RECAPTCHA_SITE_KEY, { action: 'register' })

const user = await register({
  email: email.value.trim(),
  password: password.value,
  fullName: fullName.value.trim() || null,
  phone: phone.value.trim() || null,
  recaptchaToken: recaptchaToken,  // ← ELIMINADO
})
```

**Código ahora:**
```javascript
const user = await register({
  email: email.value.trim(),
  password: password.value,
  fullName: fullName.value.trim() || null,
  phone: phone.value.trim() || null,
})
```

### Backend

**1. `UsuarioResource.java`**
- ❌ Eliminado import de `ConfigLoader`
- ❌ Eliminado import de `RecaptchaService`
- ❌ Eliminado import de `Properties`
- ❌ Eliminado campo `recaptchaService`
- ❌ Eliminado código de inicialización de `recaptchaService`
- ❌ Eliminada llamada a `recaptchaService.verifyOrThrow()`
- ✅ Registro de usuarios ahora funciona sin validación de reCAPTCHA

**Código antes:**
```java
private final RecaptchaService recaptchaService;

public UsuarioResource() {
    // ...
    Properties props = ConfigLoader.loadProperties();
    String secretKey = props.getProperty("RECAPTCHA_SECRET_KEY");
    this.recaptchaService = new RecaptchaService(secretKey);
}

public Response create(UsuarioCreateRequest req) {
    // Validar reCAPTCHA
    recaptchaService.verifyOrThrow(req.getRecaptchaToken());  // ← ELIMINADO
    
    // Crear usuario
    AppUser user = userService.createUser(...);
}
```

**Código ahora:**
```java
public UsuarioResource() {
    EntityManagerFactory emf = DatabaseConfig.getEntityManagerFactory();
    this.userService = new UserService(emf);
}

public Response create(UsuarioCreateRequest req) {
    // Crear usuario (sin validación de reCAPTCHA)
    AppUser user = userService.createUser(...);
}
```

**2. `UsuarioCreateRequest.java`**
- ❌ Eliminado campo `recaptchaToken`
- ❌ Eliminados getters/setters de `recaptchaToken`

## 🚀 Para aplicar los cambios

### 1. Reiniciar el backend

```bash
# Detén el backend (Ctrl+C)
cd fabrica/backend
mvn exec:java -Dexec.mainClass="com.agencias.backend.Main"
```

### 2. Recargar el frontend

En el navegador: **Cmd+Shift+R** (hard reload)

## 🧪 Verificar que funciona

### Test de registro

1. Ve a `/register`
2. Llena el formulario:
   - Email: test@example.com
   - Password: 123456
   - Nombre: Usuario Test
   - Teléfono: 1234567890
3. Haz clic en **"Registrarme"**
4. ✅ Debería crear el usuario sin pedir reCAPTCHA
5. ✅ Redirige a la tienda (o dashboard si es admin)

### Verificar en consola

**Frontend (DevTools):**
- ❌ No debe haber errores de `grecaptcha`
- ❌ No debe aparecer badge de reCAPTCHA en la esquina

**Backend (logs):**
- ❌ No debe haber errores de `RecaptchaService`
- ✅ Debe mostrar: "Usuario creado exitosamente"

## 📊 Archivos modificados

### Frontend
- ✅ `frontend/index.html`
- ✅ `frontend/src/views/Register.vue`

### Backend
- ✅ `backend/src/main/java/com/agencias/backend/controller/UsuarioResource.java`
- ✅ `backend/src/main/java/com/agencias/backend/controller/dto/UsuarioCreateRequest.java`

## 📝 Archivos que puedes eliminar (opcional)

Si quieres limpiar completamente el código relacionado con reCAPTCHA:

### Frontend
- `frontend/.env` (contiene `VITE_RECAPTCHA_SITE_KEY`)

### Backend
- `backend/src/main/java/com/agencias/backend/service/RecaptchaService.java`
- En `application.properties`: línea `RECAPTCHA_SECRET_KEY=...`

### Documentación
- `RECAPTCHA.md`
- Referencias a reCAPTCHA en `GUIA_RAPIDA.md`

## ✅ Estado actual

**reCAPTCHA:** ❌ **DESACTIVADO**

**Registro de usuarios:** ✅ **Funciona sin reCAPTCHA**

**Ventajas:**
- ✅ Registro más rápido
- ✅ No requiere conexión a servicios de Google
- ✅ Funciona en localhost sin problemas
- ✅ Menos dependencias externas

**Desventajas:**
- ⚠️ Sin protección contra bots
- ⚠️ Vulnerable a registro masivo automatizado

## 🔮 Si quieres reactivarlo en el futuro

1. Revertir los cambios en este commit
2. Ejecutar el script de reCAPTCHA original
3. Reiniciar backend y frontend

---

**Desactivado:** Febrero 2026  
**Estado:** ✅ Sistema funcional sin reCAPTCHA
