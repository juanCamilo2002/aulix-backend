# Aulix Backend

Backend de **Aulix**, una plataforma de aprendizaje online SaaS multi-tenant construida con Spring Boot.

## Stack

- Java 21
- Spring Boot 3.5.14
- Spring Security con JWT
- PostgreSQL con schema-per-tenant
- JPA / Hibernate
- Flyway migrations
- Tomcat Embed

## Requisitos

- Java 21
- PostgreSQL 15+
- Maven 3.9+

## Configuración

### Variables de Entorno

| Variable | Descripción | Default |
|---|---|---|
| `DB_HOST` | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `lms_saas` |
| `DB_USER` | Usuario de PostgreSQL | `postgres` |
| `DB_PASS` | Contraseña de PostgreSQL | `postgres` |
| `PORT` | Puerto del servidor | `8080` |
| `JWT_SECRET` | Clave secreta para JWT | (requerido en producción) |
| `CORS_ALLOWED_ORIGINS` | Origins permitidos para CORS | `http://localhost:3000` |

### Profiles

- `dev`: Configuración de desarrollo con `app.dev.default-tenant=acme`
- `prod`: Configuración de producción

## Desarrollo

```bash
./mvnw spring-boot:run
```

El servidor queda disponible en: `http://localhost:8080/api`

## API Endpoints

### Autenticación

| Método | Path | Descripción |
|---|---|---|
| POST | `/auth/register` | Registro de usuarios |
| POST | `/auth/login` | Login |
| POST | `/auth/refresh` | Renovar sesión |
| POST | `/auth/logout` | Cerrar sesión |
| GET | `/auth/me` | Datos del usuario actual |

### Cursos

| Método | Path | Descripción |
|---|---|---|
| GET | `/courses` | Cursos publicados |
| GET | `/courses/{slug}` | Detalle de curso |
| POST | `/courses` | Crear curso (INSTRUCTOR, ADMIN) |
| PATCH | `/courses/{id}/publish` | Publicar/ocultar curso |
| POST | `/courses/{id}/modules` | Agregar módulo |
| POST | `/courses/modules/{id}/lessons` | Agregar lección |

### Matrículas

| Método | Path | Descripción |
|---|---|---|
| POST | `/enrollments/courses/{id}` | Matricularse |
| GET | `/enrollments/my-courses` | Mis cursos |
| PUT | `/enrollments/courses/{courseId}/lessons/{lessonId}/progress` | Actualizar progreso |
| GET | `/enrollments/courses/{courseId}/progress` | Ver progreso |

### Tenants

| Método | Path | Descripción |
|---|---|---|
| POST | `/tenants` | Crear tenant (SUPERADMIN) |

## Seguridad

### Autenticación

- JWT en cookies HttpOnly (access + refresh tokens)
- Separación de access/refresh tokens por claim `type`
- Rate limiting en `/auth/login`, `/auth/register`
- CSRF protection con double-submit cookie

### Autorización

- Roles: `SUPERADMIN`, `ADMIN`, `INSTRUCTOR`, `STUDENT`
- Protección de contenido por matrícula
- Multi-tenant con PostgreSQL schema-per-tenant

## Arquitectura Multi-Tenant

- Cada tenant tiene su propio schema PostgreSQL
- Tenant resuelto por host (`subdomain.aulix.com`) o header `X-Tenant-ID` en dev
- `SchemaResolvingDataSource` aplica `search_path` por conexión

## Base de Datos

### Migraciones

- `db/migration/public/`: Tablas globales (tenants)
- `db/tenant-migration/`: Tablas por tenant (usuarios, cursos, matrículas)

### Tablas Principales

- `users`: Usuarios por tenant
- `courses`: Cursos
- `modules`: Módulos
- `lessons`: Lecciones
- `enrollments`: Matrículas
- `lesson_progress`: Progreso por lección
- `refresh_tokens`: Tokens de sesión

## Scripts

```bash
# Desarrollo
./mvnw spring-boot:run

# Tests
./mvnw test

# Build
./mvnw package -DskipTests
```

## Notas

- La API requiere `JWT_SECRET` en producción (no debe estar vacío)
- CORS configurado por propiedad `app.cors.allowed-origins`
- Rate limiting: 10 requests por 60 segundos en endpoints auth