# Backend Distribuidores

Documentación de API generada desde los comentarios XML (`///`) del proyecto **BackendDistribuidores** (.NET 9).

Use el índice lateral para abrir **API** y navegar por controladores, modelos y servicios documentados.

## Generar de nuevo el sitio

Desde la carpeta `docs` del backend:

```bash
cd distribuidores/backend/docs
dotnet tool restore --tool-manifest ../.config/dotnet-tools.json
dotnet tool run docfx -- docfx.json
```

El sitio estático queda en `_site/index.html`.
