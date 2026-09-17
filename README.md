# Modulo2-Prueba

# Framework de automatización de APIs — restful-api.dev

Framework de pruebas de API sobre el recurso `/objects` de [restful-api.dev](https://restful-api.dev/), construido con **Java 21, RestAssured, Maven, TestNG y Allure Report**. Valida los campos de la petición antes de enviarla, la estructura de la respuesta (JSON Schema), el funcionamiento del servicio (status, body, headers, SLA) e integra casos de éxito y de excepción.

## 1. Inicio rápido

Requisitos: JDK 21 y Maven 3.9+ (o solo Docker).

| Acción | Linux / macOS | Windows |
|---|---|---|
| Solo pruebas | `mvn clean test` | `mvn clean test` |
| Pruebas + reporte + correo | `./run-tests.sh qa` | `run-tests.bat qa` |
| Ver reporte en navegador | `mvn allure:serve` | `mvn allure:serve` |
| Docker con SMTP de pruebas | `docker compose up --build --abort-on-container-exit api-tests` | ídem |

Salidas en `target/`: `allure-report/`, `allure-report.zip`, `logs/api-tests.log`, `email-outbox/*.eml`.

> La API pública de restful-api.dev permite **50 peticiones diarias** (100 con API Key),si se supera el límite, el servicio responde 429 y el framework lo registra y clasifica como "Respuesta inesperada" en el reporte.

## 2. Casos de prueba (6)

| ID | Tipo | Operación | Validaciones principales |
| TC01 | Éxito | `POST /objects` | Payload validado antes del envío, 200, SLA, headers, JSON Schema, `id` y `createdAt` generados, `name`/`data` reflejan lo enviado |
| TC02 | Éxito | `GET /objects/{id}` | 200, SLA, headers, schema, datos idénticos a los creados |
| TC03 | Éxito | `PUT /objects/{id}` | 200, schema con `updatedAt`, todos los campos actualizados |
| TC04 | Éxito | `DELETE /objects/{id}` | 200, mensaje de borrado con el id, y verificación posterior de 404 |
| TC05 | Excepción | `GET /objects/{idInexistente}` | 404, schema de error, mensaje "was not found" con el id |
| TC06 | Excepción | `PUT /objects/7` (id reservado) | 405, schema de error, mensaje "reserved id" |


## 3. Arquitectura por capas

```
src/main/java/com/restfulapi/automation
├── config/        ConfigManager: properties + variables de entorno + -D, con placeholders ${VAR:default}
├── endpoints/     ApiEndpoint: catálogo central de rutas (valores en endpoints.properties)
├── auth/          Strategy + Factory: NONE, API_KEY, BEARER (estático o JWT dinámico), OAUTH2
├── client/        ApiClient (transporte RestAssured), ApiRequest (builder), ApiLoggingFilter
├── models/        request (records serializables) y response (records deserializables)
├── builders/      ObjectRequestBuilder, DeviceDataBuilder (plantillas desde testdata JSON)
├── services/      ObjectsService: operaciones de negocio del recurso /objects
├── validators/    RequestValidator (petición) y ResponseValidator (respuesta, API fluida)
├── listeners/     Trazabilidad, clasificación de fallos, reintentos por fallo de red
├── notification/  Resumen del reporte, zip y envío por correo (SMTP / simulado)
├── exceptions/    Jerarquía de errores del framework
└── utils/         JSON y carga de datos de prueba
src/test/java      BaseTest + clases de prueba (solo describen el escenario)
src/test/resources testdata/, schemas/, suites/testng.xml
```

| `src/main/resources/config/config.properties` |
| Rutas de endpoints | `config/endpoints.properties` (sobrescribibles por entorno) |
| Entornos | `config/env/qa.properties`, `config/env/qa-auth.properties` (`-Denv=...` o `TEST_ENV`) |
| Credenciales (API Key, JWT, OAuth2, SMTP) | Variables de entorno / GitHub Secrets, referenciadas como `${API_KEY:}` |
| Payloads, ids de prueba, status y mensajes esperados | `src/test/resources/testdata/objects.json` |
| Contratos de respuesta | `src/test/resources/schemas/*.json` |

## 3.1 Patrones de diseño aplicados
Builder   Payloads legibles a partir de plantillas, con variaciones por escenario y sin duplicar datos
Strategy    AuthStrategy y sus implementaciones, cada mecanismo de autenticación es intercambiable
Factory    AuthFactory construye la estrategia desde la con
figuración en tiempo de ejecución

## 4. Logs, reportería y manejo de errores

**Logs.** `ApiLoggingFilter` registra método, URI, headers y body del request, y status, tiempo, headers y body del response, en consola y en `target/logs/api-tests.log`, con un `traceId` que correlaciona ambos. Los headers sensibles (`Authorization`, `x-api-key`) se enmascaran.

**Allure.**
- `allure-rest-assured` adjunta automáticamente el **request y response HTTP** de cada llamada al paso correspondiente, sin código adicional.
- genera la línea de tiempo de cada test (validar payload → POST → status → SLA → schema...), por lo que el resultado de cada aserción es visible.
- Ofrece **categorías de fallo** historial de tendencia, severidad, epics/features y widget de entorno.
- Es agnóstico del runner y tiene integración con Maven, GitHub Actions y GitHub Pages.

**Manejo de errores sin abortar la suite.**
- Timeouts de conexión y lectura configurables: una API colgada no bloquea la ejecución.
- `ApiClient` reintenta con backoff ante `IOException` (conexión rechazada, timeout, DNS) y, si persiste, lanza `ApiCallException` con la causa raíz y el stacktrace adjunto al reporte.
- `ConnectionRetryAnalyzer` reintenta el test **solo** por fallos de infraestructura, nunca por aserciones funcionales (para no ocultar defectos reales).
- Respuestas no JSON o corruptas producen `UnexpectedResponseException` / fallo de aserción con status, content-type y body.
- Cada caso falla de forma aislada; TestNG continúa con el siguiente y `TestExecutionListener` clasifica el tipo de fallo en el log.

## 5. Envío del reporte por correo

**Funcionamiento.** Tras las pruebas, el perfil `report-and-notify` genera el reporte (`allure:report`) y ejecuta `ReportNotifier` (`exec:java`), que:
1. Lee el resumen (`widgets/summary.json`; si no existe, cuenta los `*-result.json`).
2. Comprime `target/allure-report` en `allure-report.zip`.
3. Construye un correo MIME con cuerpo HTML (estado, totales, duración, enlace a la ejecución de CI) y adjunta el zip y el log.
4. Lo envía según `email.mode`:
   - `SMTP`: envío real (Gmail con contraseña de aplicación, Office 365, SES, etc.).
   - `SIMULATED` (por defecto): escribe el **mismo mensaje MIME** en `target/email-outbox/report-<fecha>.eml`, que se abre con Outlook o Thunderbird para ver cuerpo y adjuntos. Permite demostrar el flujo sin credenciales.

Un fallo de correo se registra pero no invalida el resultado de las pruebas (`email.fail.on.error=false`).

Ejemplo Gmail:
```bash
export EMAIL_MODE=SMTP EMAIL_SMTP_AUTH=true EMAIL_SMTP_STARTTLS=true
export SMTP_HOST=smtp.gmail.com SMTP_PORT=587
export SMTP_USERNAME=cuenta@gmail.com SMTP_PASSWORD=<app-password>
export EMAIL_FROM=cuenta@gmail.com EMAIL_TO=equipo@empresa.com
./run-tests.sh qa
```

Con Docker Compose el envío es real contra **Mailpit** (SMTP local): abrir `http://localhost:8025` para ver el correo y descargar el adjunto.

## 6. Docker

- `Dockerfile`: imagen `maven:3.9.9-eclipse-temurin-21`; cachea
 dependencias en una capa separada y ejecuta pruebas → reporte → correo.
- `docker-compose.yml`: levanta `mailpit` + `api-tests` y monta `./target` para dejar reporte y logs en el host.

```bash
docker compose up --build --abort-on-container-exit api-tests
# o solo la imagen:
docker build -t restful-api-tests . && docker run --rm -v "$PWD/target:/app/target" restful-api-tests
```
En Windows PowerShell usar `-v "${PWD}/target:/app/target"`.

## 7. Ejecutar la siute de pruebas (MAVEN)

1.- mvn clean test 
2.- mvn allure:serve
3.- mvn clean verify -Preport-and-notify
4.- mvn test -Dtest=ObjectsSuccessTest
5.- mvn test -Dtest=ObjectsExceptionTest

## 78. Compatibilidad Linux / Windows

Rutas con `java.nio.file.Path`, entradas del zip normalizadas a `/`, codificación UTF-8 forzada, scripts `run-tests.sh` y `run-tests.bat`, `.gitattributes` para finales de línea, y steps de CI con `shell: bash` en ambos sistemas.


