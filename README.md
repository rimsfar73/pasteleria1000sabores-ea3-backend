# Pastelería 1000 Sabores — Backend API

**Desarrollo Fullstack II | Evaluación EA3**

Backend REST desarrollado con **Java 21, Spring Boot y PostgreSQL**, destinado a gestionar los procesos comerciales y administrativos de la aplicación web *Pastelería 1000 Sabores*.

El proyecto implementa una arquitectura monolítica modular, organizada por dominios funcionales, con persistencia mediante Spring Data JPA, migraciones Flyway, autenticación y autorización, documentación OpenAPI y pruebas automatizadas.

## 1. Tecnologías utilizadas

| Tecnología | Propósito |
|---|---|
| Java 21 | Lenguaje de programación |
| Spring Boot | Framework principal del backend |
| Spring Web MVC | Implementación de API REST |
| Spring Data JPA / Hibernate | Persistencia y acceso a datos |
| PostgreSQL | Base de datos relacional principal |
| H2 | Base de datos alternativa para desarrollo |
| Flyway | Control de versiones del esquema SQL |
| Spring Security | Seguridad de endpoints |
| JWT | Autenticación basada en tokens |
| Maven | Gestión de dependencias y compilación |
| JUnit | Pruebas automatizadas |
| JaCoCo | Análisis de cobertura de código |
| OpenAPI / Swagger | Documentación de servicios REST |
| Docker | Empaquetado y despliegue |

## 2. Arquitectura del proyecto

El backend utiliza una **arquitectura monolítica modular**: una única aplicación Spring Boot organizada en paquetes que separan responsabilidades.

```text
src/main/java/cl/milsabores/api/
├── Application.java
├── auditoria/
├── auth/
│   ├── controller/
│   └── service/
├── catalogo/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
├── common/
│   └── exception/
├── config/
├── descuento/
├── dtos/
├── security/
├── usuario/
│   ├── controller/
│   ├── entity/
│   ├── repository/
│   └── service/
└── venta/
    ├── controller/
    ├── entity/
    ├── repository/
    └── service/
```

La separación por módulos facilita el mantenimiento y la evolución del código sin introducir la complejidad operativa de los microservicios.

### Componentes principales

- **Auth:** registro, inicio de sesión, renovación de sesión y cierre de sesión.
- **Catálogo:** productos, categorías y consultas comerciales.
- **Usuarios:** gestión de usuarios y reglas de negocio relacionadas.
- **Ventas:** pedidos, detalle de pedidos y cálculos comerciales.
- **Descuentos:** gestión de cupones.
- **Auditoría:** trazabilidad de operaciones asociadas a pedidos.
- **Configuración:** parámetros comerciales, documentación API y configuración de la aplicación.
- **Seguridad:** control de acceso y validación de autenticación.

## 3. Base de datos

El sistema utiliza PostgreSQL como motor relacional principal.

La configuración de conexión se obtiene mediante variables de entorno, evitando almacenar las credenciales de producción directamente en el código.

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
├── V1__esquema.sql
└── V2__trazabilidad_comercial_ea3.sql
```

Flyway permite mantener una evolución versionada del esquema de base de datos.

## 4. Configuración del entorno

### Requisitos

- JDK 21.
- PostgreSQL.
- Maven o Maven Wrapper incluido.
- Git.

### Variables de entorno

El repositorio contiene un archivo `.env.example` como referencia.

Variables principales:

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USER` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de PostgreSQL |
| `ADMIN_PASSWORD` | Contraseña inicial de administración, cuando corresponda |

Ejemplo:

```env
DB_URL=jdbc:postgresql://localhost:5432/pasteleria
DB_USER=pasteleria_app
DB_PASSWORD=REEMPLAZAR_CON_CLAVE_LOCAL
ADMIN_PASSWORD=REEMPLAZAR_CON_CLAVE_SEGURA
```

**Importante:** no publicar contraseñas reales, archivos `.env` privados, claves JWT ni certificados privados.

### Archivos de configuración

```text
src/main/resources/
├── application.yml
├── application-h2.yml
└── application-prod.yml

src/test/resources/
└── application-test.yml
```

Los perfiles permiten separar la configuración de ejecución, pruebas y producción.

## 5. Ejecución local

Desde la carpeta raíz del backend, con PostgreSQL disponible y las variables de entorno configuradas:

**Git Bash:**

```bash
./mvnw spring-boot:run
```

**PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación utiliza Spring Boot y expone sus servicios HTTP según la configuración activa.

### Compilación

```bash
./mvnw clean package
```

El artefacto generado se encuentra en:

```text
target/pasteleria-api-1.0.0.jar
```

Para ejecutar el JAR:

```bash
java -jar target/pasteleria-api-1.0.0.jar
```

## 6. API REST

La aplicación incluye controladores para autenticación, catálogo, usuarios, pedidos y consultas.

### Autenticación

Rutas identificadas:

| Método | Endpoint |
|---|---|
| GET | `/api/v1/auth/csrf` |
| POST | `/api/v1/auth/register` |
| POST | `/api/v1/auth/login` |
| POST | `/api/v1/auth/refresh` |
| POST | `/api/v1/auth/logout` |
| GET | `/api/v1/auth/me` |

La autenticación utiliza JWT y contempla mecanismos de renovación de sesión mediante cookies HttpOnly.

Los permisos, cuerpos de solicitud y respuestas de cada endpoint deben consultarse en la documentación OpenAPI y validarse contra la configuración de seguridad activa.

## 7. Seguridad

El backend incorpora mecanismos de seguridad orientados a proteger las operaciones de la aplicación:

- Autenticación de usuarios.
- Autorización de endpoints mediante Spring Security.
- Tokens JWT.
- Renovación de sesión.
- Cookies HttpOnly para el mecanismo de refresh.
- Configuración de credenciales mediante variables de entorno.
- Separación entre configuración de desarrollo y producción.

La presencia de estos mecanismos no sustituye una auditoría de seguridad ni pruebas de penetración.

## 8. Pruebas automatizadas

El proyecto contiene cuatro clases de pruebas:

```text
src/test/java/cl/milsabores/api/
├── CalculadoraVentaTest.java
├── ConfiguracionProduccionTest.java
├── IntegracionTest.java
└── PagoSimuladoTest.java
```

### Resultado de la validación

| Clase | Pruebas aprobadas |
|---|---:|
| CalculadoraVentaTest | 4 |
| ConfiguracionProduccionTest | 2 |
| IntegracionTest | 33 |
| PagoSimuladoTest | 5 |
| **Total** | **44** |

**Última ejecución validada:** 44 pruebas aprobadas, compilación Maven exitosa.

Para ejecutar la validación:

```bash
./mvnw clean verify
```

Las pruebas de integración requieren una base de datos PostgreSQL de pruebas y credenciales válidas.

Variables utilizadas:

```text
TEST_DB_URL
TEST_DB_USER
TEST_DB_PASSWORD
```

La base de pruebas debe mantenerse separada de la base utilizada por la aplicación.

### Cobertura de código

El proyecto utiliza JaCoCo para generar informes de cobertura.

Ruta habitual del informe HTML:

```text
target/site/jacoco/index.html
```

La cobertura permite identificar partes del código que no fueron ejecutadas por las pruebas. No constituye por sí sola una garantía de ausencia de defectos.

## 9. Integración con el frontend

Este backend está diseñado para integrarse con un frontend desarrollado con React y Vite.

La integración contempla operaciones relacionadas con:

- Autenticación de usuarios.
- Consulta del catálogo.
- Gestión de productos.
- Procesamiento de pedidos.
- Operaciones administrativas autorizadas.

La integración completa debe verificarse mediante pruebas funcionales entre ambos proyectos.

## 10. Control de versiones

Repositorio:

https://github.com/rimsfar73/pasteleria1000sabores-ea3-backend

Rama de desarrollo validada:

```text
refactor/backend-packages
```

Commits relevantes:

```text
00b53ee  chore: baseline candidato EA3 v6.1
6020314  refactor: reorganizar paquetes Java antes de imports
af6d194  refactor: consolidar backend EA3 y validar pruebas PostgreSQL
2ee5896  chore: excluir configuraciones locales de IntelliJ
```

La rama `refactor/backend-packages` contiene la versión consolidada. La rama `master` conserva la línea base inicial mientras no se realice una integración posterior.

## 11. Alcance y consideraciones

El proyecto corresponde a una implementación académica de backend para comercio electrónico.

Se han validado la compilación, las migraciones y las pruebas automatizadas del backend. Sin embargo, antes de considerarlo listo para un despliegue productivo deben completarse las verificaciones de seguridad, configuración, integración frontend-backend y operación.

---

**Proyecto:** Pastelería 1000 Sabores  
**Asignatura:** Desarrollo Fullstack II  
**Evaluación:** EA3  
**Arquitectura:** Monolito modular con API REST  
**Stack principal:** Java 21 · Spring Boot · PostgreSQL · React/Vite
