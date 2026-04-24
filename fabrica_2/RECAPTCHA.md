# 🔐 Google reCAPTCHA v3 - Implementación

## 📋 Descripción

El sistema utiliza **Google reCAPTCHA v3** (invisible) para proteger el registro de nuevos usuarios contra bots y spam. A diferencia de v2, no requiere que el usuario haga clic en "No soy un robot".

## 🔑 Configuración

### Claves utilizadas

- **Site Key (pública):** `6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr`
- **Secret Key (privada):** `6LdQB2ssAAAAAG0iu_AuFvMI5ny6sECS17rbESo2`

### Frontend

**Archivo:** `fabrica/frontend/.env`

```env
VITE_RECAPTCHA_SITE_KEY=6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr
```

**Archivo:** `fabrica/frontend/index.html`

```html
<script src="https://www.google.com/recaptcha/api.js?render=6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr"></script>
```

### Backend

**Archivo:** `fabrica/backend/src/main/resources/application.properties`

```properties
RECAPTCHA_SECRET_KEY=6LdQB2ssAAAAAG0iu_AuFvMI5ny6sECS17rbESo2
```

## 🎯 Funcionamiento

### 1. Frontend (Register.vue)

Cuando el usuario envía el formulario de registro:

1. Se carga el script de Google reCAPTCHA v3 desde el CDN (invisible)
2. Al hacer submit, se ejecuta automáticamente `grecaptcha.execute()`
3. Google analiza el comportamiento del usuario y genera un token con un **score** (0.0 a 1.0)
4. El token se envía junto con los datos del formulario al backend
5. **No hay widget visible** - todo es automático

```javascript
// reCAPTCHA v3 se ejecuta al enviar el formulario
async function onSubmit() {
  // Obtener token de forma invisible
  const recaptchaToken = await window.grecaptcha.execute(
    RECAPTCHA_SITE_KEY, 
    { action: 'register' }
  )

  // El token se envía en el request
  await register({
    email: email.value,
    password: password.value,
    fullName: fullName.value,
    phone: phone.value,
    recaptchaToken: recaptchaToken  // ← Token de reCAPTCHA v3
  })
}
```

### 2. Backend (RecaptchaService.java)

El backend valida el token con Google y verifica el score:

1. Recibe el token del frontend
2. Hace una petición POST a `https://www.google.com/recaptcha/api/siteverify`
3. Envía la clave secreta y el token
4. Google responde con `{ "success": true/false, "score": 0.0-1.0 }`
5. Se verifica que `success = true` y `score >= 0.5`
6. Si el score es bajo, se rechaza el registro

```java
public boolean verify(String recaptchaResponse) {
    // Construir request a Google
    String params = "secret=" + secretKey + "&response=" + recaptchaResponse;
    
    // Hacer POST request
    HttpURLConnection conn = ...;
    
    // Parsear respuesta JSON
    JsonNode jsonNode = objectMapper.readTree(response);
    boolean success = jsonNode.get("success").asBoolean();
    
    // Verificar score (v3)
    if (success && jsonNode.has("score")) {
        double score = jsonNode.get("score").asDouble();
        return score >= 0.5;  // Mínimo 50% de confianza
    }
    
    return success;
}
```

### 3. Endpoint de registro (UsuarioResource.java)

```java
@POST
public Response create(UsuarioCreateRequest req) {
    // 1. Validar reCAPTCHA primero
    recaptchaService.verifyOrThrow(req.getRecaptchaToken());
    
    // 2. Si pasa, crear usuario
    AppUser user = userService.createUser(...);
    
    return Response.status(201).entity(user).build();
}
```

## 🛡️ Seguridad

### ¿Por qué reCAPTCHA v3?

- **Invisible**: No interrumpe la experiencia del usuario
- **Basado en comportamiento**: Analiza cómo el usuario interactúa con el sitio
- **Score de confianza**: Asigna un puntaje de 0.0 (bot) a 1.0 (humano)
- Previene registro masivo de cuentas falsas
- Protege contra bots automatizados
- Reduce spam y abuso del sistema

### Sistema de Score

reCAPTCHA v3 asigna un score basado en:
- Movimientos del mouse
- Tiempo en la página
- Interacciones con formularios
- Historial de navegación
- Otros factores de comportamiento

**Umbrales recomendados:**
- `>= 0.7`: Muy probablemente humano
- `>= 0.5`: Probablemente humano (nuestro mínimo)
- `< 0.5`: Sospechoso, probablemente bot
- `< 0.3`: Muy probablemente bot

### Validación en backend

1. **Token único**: Cada token solo puede usarse una vez
2. **Verificación con Google**: Se valida directamente con los servidores de Google
3. **Score mínimo**: Rechazamos registros con score < 0.5
4. **Timeout**: Los tokens expiran después de 2 minutos

## 🔄 Flujo completo

```
Usuario → Formulario de registro
   ↓
Completa campos (reCAPTCHA v3 analiza en segundo plano)
   ↓
Usuario hace clic en "Registrarme"
   ↓
grecaptcha.execute() genera token automáticamente
   ↓
Frontend envía: { email, password, ..., recaptchaToken }
   ↓
Backend recibe request
   ↓
RecaptchaService valida con Google
   ↓
Google responde: { success: true, score: 0.8 }
   ↓
   ├─ ✅ Score >= 0.5 → Crear usuario
   └─ ❌ Score < 0.5 → Error 400 "Actividad sospechosa"
```

## 📝 Mensajes de error

Si el token es inválido, expiró, o el score es bajo:
```
"Verificación de reCAPTCHA fallida. Tu actividad parece sospechosa. Por favor, intenta de nuevo."
```

**Nota:** En reCAPTCHA v3 no hay validación frontend de "completar CAPTCHA" porque es invisible.

## 🧪 Testing

### Modo de prueba

Google reCAPTCHA v3 funciona en localhost sin configuración adicional.

### Casos de prueba

1. **Usuario normal navega y se registra** → ✅ Score alto (0.7-1.0), registro exitoso
2. **Bot automatizado** → ❌ Score bajo (<0.5), registro rechazado
3. **Token expirado (>2 min)** → ❌ Error en backend
4. **Token reutilizado** → ❌ Error en backend
5. **Sin conexión a internet** → ❌ Script no carga, error en frontend

### Simular diferentes scores

Para testing, puedes ajustar el umbral `MIN_SCORE` en `RecaptchaService.java`:
- `MIN_SCORE = 0.3` → Más permisivo (acepta más usuarios)
- `MIN_SCORE = 0.7` → Más restrictivo (solo usuarios muy confiables)

## 🔧 Troubleshooting

### Badge de reCAPTCHA visible en la esquina

Es normal. reCAPTCHA v3 muestra un badge flotante en la esquina inferior derecha. Puedes ocultarlo con CSS si quieres:

```css
.grecaptcha-badge { 
  visibility: hidden; 
}
```

**Importante:** Si lo ocultas, debes mostrar el texto legal (ya incluido en el formulario).

### Error "Verificación fallida"

- El token puede haber expirado (>2 min)
- El token ya fue usado anteriormente
- La clave secreta en el backend es incorrecta
- El score es demasiado bajo (<0.5)
- Problemas de red con los servidores de Google

### Score siempre bajo en desarrollo

Durante desarrollo local, los scores pueden ser más bajos. En producción con un dominio real, los scores suelen ser más altos.

### Error al ejecutar grecaptcha.execute()

- Verifica que el script esté cargado: `window.grecaptcha`
- Confirma que la site key sea correcta
- Revisa la consola del navegador por errores de CORS

## 📚 Recursos

- [Documentación oficial de reCAPTCHA v3](https://developers.google.com/recaptcha/docs/v3)
- [API de verificación](https://developers.google.com/recaptcha/docs/verify)
- [Consola de administración](https://www.google.com/recaptcha/admin)
- [Interpretando el score](https://developers.google.com/recaptcha/docs/v3#interpreting_the_score)

---

**Implementado:** Febrero 2026  
**Versión:** reCAPTCHA v3 (invisible, score-based)
