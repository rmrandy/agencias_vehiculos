# Backend Distribuidores

Documentación de API generada desde los comentarios XML (`///`) del proyecto **BackendDistribuidores** (.NET 9).

Use el índice lateral para abrir **API** y navegar por controladores, modelos y servicios documentados.

## Generar de nuevo el sitio

`docfx.json` está en **esta carpeta** (`docs`), no en la raíz del proyecto .NET. Si ejecutas DocFX desde `backend/`, fallará con “Cannot find config file …/backend/docfx.json”.

**Opción recomendada** (desde la raíz del repo `agencias_vehiculos`):

```bash
./distribuidores/backend/generate-docs.sh
```

**Manual** — el `cd` depende de dónde estés ahora:

| Estás en… | Comando |
|-----------|---------|
| Raíz del repo | `cd distribuidores/backend/docs` |
| Ya en `distribuidores` | `cd backend/docs` |
| Ya en `distribuidores/backend` | `cd docs` |

Luego:

```bash
dotnet tool restore --tool-manifest ../.config/dotnet-tools.json
dotnet tool run docfx -- docfx.json
```

El sitio estático queda en **`docs/_site/index.html`** (ruta completa: `distribuidores/backend/docs/_site/index.html`).
