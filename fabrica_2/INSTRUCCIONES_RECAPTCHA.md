# 🔧 Instrucciones para activar reCAPTCHA v3

## ⚠️ Importante: Reiniciar el frontend

Para que reCAPTCHA v3 funcione correctamente, **debes reiniciar el servidor de desarrollo del frontend**.

### Pasos:

1. **Detén el servidor frontend** (si está corriendo):
   - Presiona `Ctrl + C` en la terminal donde está corriendo `npm run dev`

2. **Reinicia el servidor**:
   ```bash
   cd fabrica/frontend
   npm run dev
   ```

3. **Recarga la página** en el navegador:
   - Presiona `Cmd + Shift + R` (Mac) o `Ctrl + Shift + R` (Windows/Linux)
   - Esto hace un "hard reload" que limpia la caché

4. **Abre la consola del navegador** (F12 o clic derecho → Inspeccionar):
   - Deberías ver el mensaje: `"reCAPTCHA v3 cargado correctamente"`
   - Si ves errores, revisa que la Site Key sea correcta

## ✅ Cómo verificar que funciona

### En el formulario de registro:

1. Al cargar la página, verás brevemente: **"⏳ Cargando verificación de seguridad..."**
2. Después de 1-2 segundos, ese mensaje desaparecerá
3. El botón "Registrarme" se activará (ya no estará deshabilitado)
4. En la esquina inferior derecha verás el badge de reCAPTCHA (pequeño logo azul)

### Al registrar un usuario:

1. Llena el formulario normalmente
2. Haz clic en "Registrarme"
3. reCAPTCHA v3 se ejecuta **automáticamente** (no verás nada)
4. Si todo va bien, se crea el usuario
5. Si el score es bajo, verás: "Tu actividad parece sospechosa"

## 🐛 Troubleshooting

### Error: "reCAPTCHA no encontrado"

**Causa:** El frontend no se reinició después de crear el archivo `.env`

**Solución:** 
1. Detén el servidor (`Ctrl + C`)
2. Reinicia: `npm run dev`
3. Recarga la página con `Cmd + Shift + R`

### El botón dice "Cargando..." y nunca se activa

**Causa:** El script de Google no se está cargando

**Solución:**
1. Abre la consola del navegador (F12)
2. Busca errores relacionados con `grecaptcha` o `recaptcha`
3. Verifica tu conexión a internet
4. Verifica que la Site Key sea correcta en `.env`

### Error: "Site key inválida"

**Causa:** La Site Key en `.env` no coincide con la del script en `index.html`

**Solución:**
1. Verifica que ambas usen: `6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr`
2. Reinicia el frontend

### El registro falla con "Verificación fallida"

**Causa:** El score de reCAPTCHA es muy bajo (<0.5)

**Posibles razones:**
- Estás en localhost (los scores suelen ser más bajos en desarrollo)
- Navegación muy rápida (parece comportamiento de bot)
- VPN o proxy activo

**Solución temporal para desarrollo:**
- Edita `RecaptchaService.java`
- Cambia `MIN_SCORE = 0.5` a `MIN_SCORE = 0.3`
- Reinicia el backend

## 📋 Checklist de verificación

- [ ] Archivo `.env` existe en `fabrica/frontend/`
- [ ] `.env` contiene `VITE_RECAPTCHA_SITE_KEY=6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr`
- [ ] Frontend reiniciado después de crear `.env`
- [ ] Página recargada con hard reload (`Cmd + Shift + R`)
- [ ] Consola del navegador muestra "reCAPTCHA v3 cargado correctamente"
- [ ] Badge de reCAPTCHA visible en esquina inferior derecha
- [ ] Botón "Registrarme" se activa después de cargar
- [ ] Backend tiene la Secret Key en `application.properties`
- [ ] Backend reiniciado

## 🎯 Configuración actual

### Frontend
- **Site Key:** `6LdQB2ssAAAAAJNnPoFQLnf9lSaB6OseUvShnujr`
- **Ubicación:** `fabrica/frontend/.env`
- **Script:** `fabrica/frontend/index.html`

### Backend
- **Secret Key:** `6LdQB2ssAAAAAG0iu_AuFvMI5ny6sECS17rbESo2`
- **Ubicación:** `fabrica/backend/src/main/resources/application.properties`
- **Score mínimo:** `0.5` (50% de confianza)

---

**Nota:** reCAPTCHA v3 es **invisible**. No hay checkbox ni widget para marcar. Todo sucede automáticamente cuando haces clic en "Registrarme".
