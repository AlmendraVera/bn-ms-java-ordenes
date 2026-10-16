# bn-ms-java-ordenes

Microservicio de **órdenes** (Capa 3): crea órdenes, evita duplicados (webhooks repetidos) y entrega el tracking. Al crear una orden valida el stock en *inventario* y guarda el precio vigente de cada producto.

> Proyecto de la **Evaluación Parcial 2 (JVY0101)**. Forma parte de la arquitectura de 4 microservicios: usuarios, inventario, órdenes y picking-despacho.

## Modelo de datos

```
Orden 1 ───── N DetalleOrden        (productos de la orden)
Orden 1 ───── N SeguimientoOrden    (historial de estados = tracking)
```

- **Orden**: `id`, `codigoExterno` (único: id de la venta en Shopify u otro canal), `cliente`, `canal`, `estado`, `fechaCreacion`, `total`
- **DetalleOrden**: `id`, `sku`, `cantidad`, `precioUnitario` (lo toma de inventario)
- **SeguimientoOrden**: `id`, `estado`, `fecha`, `comentario`

### Estados y transiciones permitidas

```
CREADA ──> EN_PICKING ──> DESPACHADA
   └──────────┴──────────> CANCELADA
```

Cualquier otro cambio devuelve **409**. Solo se puede modificar una orden en estado `CREADA`.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| POST | `/api/ordenes` | Crea una orden (destino del webhook de venta) | 201, 400, 404, 409, 503 |
| GET | `/api/ordenes` | Lista órdenes (`?estado=CREADA` para filtrar) | 200 |
| GET | `/api/ordenes/{id}` | Busca por id | 200, 404 |
| GET | `/api/ordenes/codigo/{codigoExterno}` | Busca por código externo | 200, 404 |
| GET | `/api/ordenes/{id}/tracking` | Historial de estados de la orden | 200, 404 |
| PUT | `/api/ordenes/{id}` | Actualiza cliente/canal (solo si está CREADA) | 200, 400, 404, 409 |
| PUT | `/api/ordenes/{id}/estado` | Cambia el estado (`{"estado":"EN_PICKING","comentario":"..."}`) | 200, 400, 404, 409 |
| DELETE | `/api/ordenes/{id}` | Elimina (no si está EN_PICKING o DESPACHADA) | 204, 404, 409 |

### Ejemplo

```bash
curl -X POST localhost:8083/api/ordenes -H "Content-Type: application/json" -d '{
  "codigoExterno": "SHP-1001",
  "cliente": "Camila Rojas",
  "canal": "SHOPIFY",
  "detalles": [
    {"sku": "AUD-001", "cantidad": 2},
    {"sku": "CAR-002", "cantidad": 1}
  ]
}'
```

Si se repite el mismo `codigoExterno` responde **409** (evita duplicados). Si un SKU no existe responde **404**; si no hay stock suficiente, **409**.

## Requisitos

- JDK 17 o superior (`java -version`)
- Git
- No hace falta instalar Maven: el proyecto incluye el *Maven Wrapper* (`mvnw` / `mvnw.cmd`)

## Cómo clonar, instalar dependencias y ejecutar

```bash
git clone https://github.com/AlmendraVera/bn-ms-java-ordenes.git
cd bn-ms-java-ordenes

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Maven descarga las dependencias automáticamente la primera vez. Cuando termine verás
`Started Application` y el servicio quedará escuchando en **http://localhost:8083**.

**Depende de `bn-ms-java-inventario`** (puerto 8082): debe estar levantado para poder crear órdenes. Si no responde, la API devuelve **503**. La URL se configura en `application.properties` (`servicios.inventario.url`).

## Base de datos

H2 en memoria (se crea al iniciar y se borra al detener). Consola web: **http://localhost:8083/h2-console**

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:ordenesdb;DB_CLOSE_DELAY=-1` |
| User Name | `sa` |
| Password | *(vacía)* |

La configuración está en `src/main/resources/application.properties`.

## Compilar y empaquetar con Maven

```bash
./mvnw clean      # borra la carpeta target/
./mvnw install    # compila, ejecuta los tests e instala el jar en el repositorio local
./mvnw package    # genera target/bn-ms-java-ordenes-0.0.1-SNAPSHOT.jar

java -jar target/bn-ms-java-ordenes-0.0.1-SNAPSHOT.jar
```

## Estructura (arquitectura en capas)

```
src/main/java/cl/duoc/bn/ordenes/
├── controller/   endpoints REST (reciben la petición y devuelven la respuesta HTTP)
├── service/      lógica de negocio y validaciones
├── repository/   acceso a datos (Spring Data JPA)
├── model/        entidades JPA
├── exception/    ApiException + ManejadorExcepciones (respuestas de error en JSON)
├── client/       InventarioClient: llamadas HTTP al microservicio de inventario
└── Application.java
```

## Formato de los errores

Todos los errores devuelven el mismo JSON:

```json
{
  "timestamp": "2026-10-15T21:03:08",
  "status": 404,
  "error": "Not Found",
  "mensaje": "No existe un recurso con id 99",
  "ruta": "/api/..."
}
```

| Código | Cuándo ocurre |
|---|---|
| 400 | Datos faltantes o inválidos, JSON mal formado, parámetro con tipo incorrecto |
| 404 | El recurso no existe |
| 409 | Conflicto: dato duplicado o acción no permitida en el estado actual |
| 503 | El microservicio de inventario no responde |

## Integrantes

- *(Nombre y apellido del integrante 1)*
- *(Nombre y apellido del integrante 2)*
