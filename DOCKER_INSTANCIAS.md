# Docker: multiples instancias (5 fabricas + 2 distribuidoras)

Este setup levanta:

- 5 fabricas (un Oracle compartido con esquemas por fábrica + backend + frontend)
- 2 distribuidoras (cada una con SQL Server + backend + frontend)

## 1) Preparar variables

```bash
cp .env.instances.example .env.instances
```

Edita `.env.instances` y cambia puertos/passwords si quieres.

## 2) Levantar todo

```bash
docker compose -f docker-compose.instances.yml --env-file .env.instances up -d --build
```

O con script:

```bash
./up-instances.sh
```

## 3) Apagar todo

```bash
docker compose -f docker-compose.instances.yml --env-file .env.instances down
```

## 3.1) Levantar stack minimo (1 fabrica + 1 distribuidora)

```bash
./up-minimal.sh
```

## 4) Apagar y borrar datos (volumenes DB)

```bash
docker compose -f docker-compose.instances.yml --env-file .env.instances down -v
```

## 5) URLs por defecto

- Fabrica 1: backend `http://localhost:5051`, frontend `http://localhost:5052`
- Fabrica 2: backend `http://localhost:5061`, frontend `http://localhost:5062`
- Fabrica 3: backend `http://localhost:5071`, frontend `http://localhost:5072`
- Fabrica 4: backend `http://localhost:5081`, frontend `http://localhost:5082`
- Fabrica 5: backend `http://localhost:5091`, frontend `http://localhost:5092`

- Distribuidora 1: backend `http://localhost:5180`, frontend `http://localhost:5273`
- Distribuidora 2: backend `http://localhost:5280`, frontend `http://localhost:5373`

## 6) Asignar puertos por instancia

Solo cambia los valores en `.env.instances`, por ejemplo:

```env
FABRICA_3_BE_PORT=6101
FABRICA_3_FE_PORT=6102
DIST_2_BE_PORT=6201
DIST_2_FE_PORT=6202
```

Vuelve a levantar:

```bash
docker compose -f docker-compose.instances.yml --env-file .env.instances up -d --build
```

## Notas

- Las fábricas comparten un único Oracle (`oracle-shared`) y cada fábrica usa su propio esquema: `FABRICA1..FABRICA5`.
- SQL Server sí mantiene un volumen por distribuidora (`sql_d1_data`, `sql_d2_data`).
- Distribuidora 1 apunta por defecto a Fabrica 1, y Distribuidora 2 a Fabrica 2 (`FabricaApiUrl`).
- Si quieres emparejar diferente (por ejemplo distribuidora 2 -> fabrica 5), cambia `FabricaApiUrl` en `docker-compose.instances.yml`.
