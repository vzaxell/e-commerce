# E-Commerce Gateway & Dashboard: Composite API

Proyecto de la asignatura **Desarrollo Web Integral, Unidad 3**, del programa educativo de Desarrollo y Gestión de Software de la Universidad Tecnológica de Chihuahua (UTCH).

El sistema implementa el patrón **Composite API (API Aggregator)**: un único endpoint consulta de forma concurrente un catálogo de productos, un inventario y una API externa de tipos de cambio, y devuelve una sola respuesta JSON consolidada. Incluye un dashboard web que consume exclusivamente dicho endpoint.

---

## Contenido

1. [Planteamiento del problema](#1-planteamiento-del-problema)
2. [Arquitectura](#2-arquitectura)
3. [Tecnologías utilizadas](#3-tecnologías-utilizadas)
4. [Requisitos previos](#4-requisitos-previos)
5. [Instrucciones de ejecución](#5-instrucciones-de-ejecución)
6. [Endpoints disponibles](#6-endpoints-disponibles)
7. [Ejemplos de respuesta](#7-ejemplos-de-respuesta)
8. [Manejo de errores y resiliencia](#8-manejo-de-errores-y-resiliencia)
9. [Pruebas](#9-pruebas)
10. [Estructura del proyecto](#10-estructura-del-proyecto)
11. [Flujo de ramas (Gitflow)](#11-flujo-de-ramas-gitflow)
12. [Solución de problemas](#12-solución-de-problemas)
13. [Evidencias](#13-evidencias)
14. [Autoría](#14-autoría)

---

## 1. Planteamiento del problema

La presentación de la ficha de un producto requiere información proveniente de tres fuentes distintas. Si el cliente (web o móvil) las consume de forma directa, se presenta el anti-patrón conocido como *Chatty I/O*: múltiples peticiones de pequeño tamaño, mayor latencia percibida, duplicación de lógica de negocio en cada cliente y exposición directa a servicios de terceros.

La Composite API centraliza la orquestación. El cliente realiza una sola petición, y el servidor ejecuta las tres consultas de manera concurrente, transforma los datos y entrega un contrato unificado.

## 2. Arquitectura

```text
Cliente / Dashboard
        |  GET /api/v1/product-summary/{id}
        v
+---------------- Composite API -----------------+
| ProductSummaryService (CompletableFuture +     |
| hilos virtuales)                               |
+-------+-----------------+-----------------+----+
        v                 v                 v
  MS1 Catálogo      MS2 Inventario    MS3 Tipo de cambio (externo)
  /microservices/   /microservices/   Frankfurter
  products/{id}     inventory/{id}    (respaldo: ExchangeRate-API)
        |                 |
        +-- PostgreSQL 16 (Docker, puerto 5433) --+
```

| Microservicio / API | Tipo | Protocolo / formato | Endpoint | Función |
|---|---|---|---|---|
| 1. Catálogo | Propia | REST / JSON | `/microservices/products/{id}` | Información general del producto y precio base en USD |
| 2. Inventario | Propia | REST / JSON | `/microservices/inventory/{id}` | Stock disponible, almacén y días estimados de entrega |
| 3. Divisas | Terceros (Frankfurter) | REST / JSON | `https://api.frankfurter.app/latest?from=USD&to=MXN` | Tipo de cambio USD/MXN. Respaldo: `https://open.er-api.com/v6/latest/USD` |
| 4. Composite API | Propia (agregadora) | REST / JSON | `/api/v1/product-summary/{id}` | Orquesta las tres consultas y unifica el contenido de la respuesta |

Los microservicios 1 y 2 se ejecutan dentro de la misma aplicación Spring Boot; no obstante, la API compuesta los consume mediante HTTP (propiedad `app.services.base-url`), de modo que pueden separarse en aplicaciones independientes sin modificar la lógica de orquestación.

## 3. Tecnologías utilizadas

- Java 21 y Spring Boot (Maven Wrapper incluido).
- Spring Web y Spring Data JPA.
- PostgreSQL 16 mediante Docker Compose.
- `RestClient` con tiempos de espera de 3000 ms.
- `CompletableFuture` con hilos virtuales (`Executors.newVirtualThreadPerTaskExecutor()`).
- Dashboard estático en HTML y JavaScript (`src/main/resources/static/index.html`).

## 4. Requisitos previos

| Herramienta | Versión | Comando de verificación |
|---|---|---|
| JDK | 21 | `java -version` |
| Docker y Docker Compose | Reciente | `docker --version` |
| Git | Cualquiera | `git --version` |
| Conexión a internet | No aplica | Requerida para el tipo de cambio; sin ella la API responde `PARTIAL_SUCCESS` |

No es necesario instalar Maven, ya que el proyecto incluye `mvnw` y `mvnw.cmd`.

## 5. Instrucciones de ejecución

### 5.1 Clonar el repositorio

```bash
git clone https://github.com/vzaxell/e-commerce.git
cd e-commerce
git checkout develop
```

### 5.2 Iniciar PostgreSQL con Docker

```bash
docker compose up -d
```

Este comando crea el contenedor `pg-ecommerce` (PostgreSQL 16) con la base de datos `ecommerce`, expuesto en el puerto **5433** del equipo anfitrión. Para verificar que se encuentra activo:

```bash
docker ps
```

### 5.3 Ejecutar la aplicación

Linux y macOS:

```bash
./mvnw spring-boot:run
```

Windows (PowerShell o CMD):

```powershell
mvnw.cmd spring-boot:run
```

Durante el arranque, JPA crea las tablas necesarias (`ddl-auto=update`) y el archivo `data.sql` carga los datos de ejemplo de forma automática.

### 5.4 Acceder al dashboard

Abrir en el navegador: <http://localhost:8080/>

Ingresar un identificador (`P-101`, `P-102` o `P-103`) y seleccionar **Consultar**.

### 5.5 Detener el sistema

```bash
# Aplicación: presionar Ctrl + C en la terminal donde se ejecuta
docker compose down
```

### 5.6 Configuración

Archivo `src/main/resources/application.properties`:

| Propiedad | Valor por defecto | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto de la aplicación |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/ecommerce` | Cadena de conexión a PostgreSQL |
| `spring.datasource.username` / `password` | `postgres` / `postgres` | Credenciales de la base de datos |
| `app.services.base-url` | `http://localhost:8080` | URL base de los microservicios propios |
| `app.external.timeout-ms` | `3000` | Tiempo máximo de conexión y lectura (ms) |

## 6. Endpoints disponibles

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/microservices/products/{id}` | Microservicio 1: catálogo |
| GET | `/microservices/inventory/{productId}` | Microservicio 2: inventario |
| GET | `/api/v1/product-summary/{id}` | Composite API: resumen unificado |
| GET | `/api/v1/products/{id}/summary` | Ruta alterna de la Composite API |

### Datos de ejemplo

| ID | Producto | Precio USD | Unidades | Almacén | Entrega (días) |
|---|---|---|---|---|---|
| `P-101` | Laptop Gamer XYZ | 1,200.00 | 15 | Almacén Chihuahua, Pasillo B3 | 2 |
| `P-102` | Mouse Inalámbrico Pro | 45.50 | 0 (agotado) | Almacén CDMX, Pasillo A1 | 7 |
| `P-103` | Monitor 27" 4K | 380.00 | 8 | Almacén Monterrey, Pasillo C2 | 3 |

## 7. Ejemplos de respuesta

### 7.1 Caso exitoso: `GET /api/v1/product-summary/P-101`

```json
{
  "product_id": "P-101",
  "title": "Laptop Gamer XYZ",
  "price_usd": 1200.00,
  "price_mxn": 22497.00,
  "stock_status": "IN_STOCK",
  "available_units": 15,
  "currency_conversion_source": "Frankfurter External API",
  "status": "SUCCESS"
}
```

El valor de `price_mxn` varía según el tipo de cambio vigente.

### 7.2 Producto agotado: `GET /api/v1/product-summary/P-102`

La respuesta presenta `stock_status` igual a `OUT_OF_STOCK` y `available_units` igual a `0`.

### 7.3 Respuesta parcial (sin acceso a internet)

```json
{
  "product_id": "P-101",
  "title": "Laptop Gamer XYZ",
  "price_usd": 1200.00,
  "price_mxn": null,
  "stock_status": "IN_STOCK",
  "available_units": 15,
  "currency_conversion_source": "N/A",
  "status": "PARTIAL_SUCCESS",
  "warnings": ["Tipo de cambio no disponible"]
}
```

### 7.4 Producto inexistente: `GET /api/v1/product-summary/P-999`

```json
{
  "timestamp": "2026-10-29T20:22:33Z",
  "status": 404,
  "error": "Not Found",
  "message": "Producto no encontrado: P-999",
  "path": "/api/v1/product-summary/P-999",
  "apiStatus": "ERROR"
}
```

### 7.5 Descripción del contrato

| Campo | Descripción |
|---|---|
| `price_mxn` | Resultado de `price_usd` por la tasa de cambio, redondeado a dos decimales; `null` si no se obtuvo la tasa |
| `stock_status` | `IN_STOCK`, `OUT_OF_STOCK` o `UNKNOWN` (inventario no disponible) |
| `currency_conversion_source` | `Frankfurter External API`, `ExchangeRate-API (fallback)` o `N/A` |
| `status` | `SUCCESS` o `PARTIAL_SUCCESS` |
| `warnings` | Se incluye únicamente cuando existen advertencias |

## 8. Manejo de errores y resiliencia

| Escenario | Comportamiento | Resultado |
|---|---|---|
| Producto inexistente | Se lanza `ResourceNotFoundException` | 404 con `ApiError` |
| Catálogo no disponible o con tiempo de espera agotado | Se lanza `ServiceUnavailableException` | 503 con `ApiError` |
| Inventario con falla o sin registro | Se absorbe el error; `stock_status` igual a `UNKNOWN` | 200 con `PARTIAL_SUCCESS` |
| Falla de Frankfurter | Se utiliza ExchangeRate-API como respaldo | 200 con `SUCCESS` (fuente de respaldo) |
| Falla de Frankfurter y del respaldo | `price_mxn` igual a `null` y advertencia | 200 con `PARTIAL_SUCCESS` |
| Error no previsto | Manejador global con mensaje genérico | 500 con `ApiError` |

El catálogo constituye una dependencia obligatoria. El inventario y el tipo de cambio son dependencias opcionales: su falla degrada la respuesta sin impedir su entrega.

## 9. Pruebas

### 9.1 Mediante `curl`

```bash
# Caso exitoso
curl http://localhost:8080/api/v1/product-summary/P-101

# Producto sin stock
curl http://localhost:8080/api/v1/product-summary/P-102

# Producto inexistente (404)
curl -i http://localhost:8080/api/v1/product-summary/P-999

# Ruta alterna
curl http://localhost:8080/api/v1/products/P-101/summary

# Microservicios individuales
curl http://localhost:8080/microservices/products/P-101
curl http://localhost:8080/microservices/inventory/P-101

# API externa directa
curl "https://api.frankfurter.app/latest?from=USD&to=MXN"
```

### 9.2 Mediante Postman

Definir la variable `baseUrl` con el valor `http://localhost:8080` y ejecutar las rutas indicadas en la sección 6.

### 9.3 Verificación de la tolerancia a fallos

1. **API externa no disponible:** desconectar el equipo de internet y consultar `P-101`. Se obtiene `PARTIAL_SUCCESS` con `price_mxn` igual a `null`.
2. **Servidor detenido:** detener la aplicación y consultar desde el dashboard. Se muestra el mensaje "Error de conexión con el servidor".

### 9.4 Pruebas automatizadas

```bash
./mvnw test        # Windows: mvnw.cmd test
```

## 10. Estructura del proyecto

```text
.
|-- docker-compose.yml
|-- pom.xml
|-- mvnw / mvnw.cmd
|-- docs/
|   |-- comands/          Capturas de comandos Git y Pull Requests
|   |-- endpoints/        Capturas de pruebas en Postman
|   `-- dashboard/        Capturas del dashboard web
`-- src/main/
    |-- java/e/comerce/microservices/
    |   |-- catalog/      MS1: Product, ProductDto, ProductRepository, ProductController
    |   |-- inventory/    MS2: InventoryItem, InventoryDto, InventoryRepository, InventoryController
    |   |-- composite/    Composite API: controlador, servicio, respuesta y clientes
    |   |   `-- client/   CatalogClient, InventoryClient, ExchangeRateClient
    |   |-- config/       AsyncConfig (hilos virtuales), RestClientConfig
    |   `-- exception/    GlobalExceptionHandler, ApiError y excepciones propias
    `-- resources/
        |-- application.properties
        |-- data.sql      Datos de ejemplo
        `-- static/index.html   Dashboard
```

## 11. Flujo de ramas (Gitflow)

```text
master <- develop <- feat/catalog-service
                  <- feat/inventory-service
                  <- feat/composite-api
                  <- feat/error-handling
                  <- feat/frontend-dashboard
                  <- fix/postgres-port
                  <- feat/docs
```

Se emplea la convención de mensajes de commit `feat`, `fix`, `chore` y `docs`. Procedimiento para incorporar un cambio:

```bash
git checkout develop
git checkout -b feat/NOMBRE
git add .
git commit -m "feat: descripción del cambio"
git push -u origin feat/NOMBRE
# Posteriormente, crear un Pull Request hacia develop en GitHub
```

## 12. Solución de problemas

| Problema | Causa probable | Solución |
|---|---|---|
| `Connection refused` hacia PostgreSQL | El contenedor no está en ejecución | Ejecutar `docker compose up -d` y verificar con `docker ps` |
| `port 5433 is already allocated` | Otro contenedor utiliza el mismo puerto | Detener dicho contenedor, o modificar el puerto en `docker-compose.yml` y en `spring.datasource.url` |
| `Port 8080 already in use` | Otra aplicación utiliza el puerto 8080 | Cerrarla, o modificar `server.port` y `app.services.base-url` |
| `invalid target release: 21` | Versión de JDK inferior a la 21 | Instalar JDK 21 y verificar con `java -version` |
| `price_mxn` igual a `null` | Sin acceso a las APIs de divisas | Verificar la conexión a internet; es el comportamiento esperado sin acceso |
| Se requiere restablecer la base de datos | Datos modificados | Ejecutar `docker compose down -v` y volver a iniciar el contenedor |

## 13. Evidencias

Las capturas de pantalla de las pruebas se encuentran en la carpeta [`docs/`](docs/):

- [`docs/endpoints/`](docs/endpoints): pruebas en Postman (catálogo, inventario, Composite API, errores y API externa no disponible).
- [`docs/dashboard/`](docs/dashboard): dashboard con los productos P-101, P-102 y P-103, producto inexistente y servidor detenido.
- [`docs/comands/`](docs/comands): comandos de Git y Pull Requests.

## 14. Autoría

**[Nombre del alumno]**
Universidad Tecnológica de Chihuahua (UTCH)
Programa Educativo: Desarrollo y Gestión de Software
Grupo: [Grupo]
