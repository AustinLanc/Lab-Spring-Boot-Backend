# LabBackend

A Spring Boot 4.0.2 REST API for managing grease/lubricant manufacturing operations — batch tracking, quality control logging, retain sample management, laboratory testing data, and automated batch reminder notifications via Telegram.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Getting Started](#getting-started)
5. [Docker Deployment](#docker-deployment)
6. [Configuration Reference](#configuration-reference)
7. [Authentication \& Security](#authentication--security)
8. [Database Schema](#database-schema)
9. [Batch Naming Convention](#batch-naming-convention)
10. [API Reference](#api-reference)
11. [Reminder System](#reminder-system)
12. [Telegram Bot Integration](#telegram-bot-integration)
13. [Monthly Statistics Engine](#monthly-statistics-engine)

---

## Architecture Overview

The application follows a standard layered Spring Boot architecture:

```
┌─────────────────────────────────────────────────────────┐
│  Clients (Angular on localhost:4200/4201, Telegram Bot) │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTPS / Telegram API
┌──────────────────────────▼──────────────────────────────┐
│                    Security Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  CORS Filter │→ │ JWT Filter   │→ │ Authorization │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                   Controller Layer                      │
│  AuthController · MonthlyBatchController · QcController │
│  RetainController · ReminderController · TestingData... │
│  ProductNameController · RouteController (SPA forward)  │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    Service Layer                        │
│  JwtService · LdapAuthService · TelegramBotService      │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                  Repository Layer (JPA)                 │
│  6 repositories with custom JPQL + native SQL queries   │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                     MySQL Database                      │
│  Tables: monthly\_batches, qc, retains, testing\_data,  │
│          reminders, names                               │
└─────────────────────────────────────────────────────────┘
```

**Request lifecycle:** Every incoming HTTP request passes through the CORS filter, then the JWT authentication filter (which extracts and validates the token from the `Authorization` header or `auth\_token` cookie), then Spring Security's authorization rules. Public endpoints under `/api/auth/\*\*` bypass authentication. All other `/api/\*\*` endpoints require a valid JWT.

---

## Technology Stack

|Component|Technology|Version|
|-|-|-|
|Language|Java|25|
|Framework|Spring Boot|4.0.2|
|Build|Maven|wrapper included|
|Database|MySQL|any 8.x+|
|ORM|Hibernate (JPA)|managed by Spring Boot|
|Security|Spring Security + JWT (jjwt)|jjwt 0.12.6|
|Auth Provider|LDAP (Active Directory)|Spring LDAP|
|Notifications|Telegram Bot API (direct HTTP)|RestTemplate + httpclient5|
|Health Checks|Spring Boot Actuator|managed by Spring Boot|
|Code Generation|Lombok|managed by Spring Boot|
|Env Management|dotenv-java|3.0.0|
|Containerization|Docker|multi-stage build|

---

## Project Structure

```
src/main/java/org/example/labbackend/
├── LabBackendApplication.java             # Entry point, loads .env, enables scheduling
│
├── config/
│   ├── EnvConfig.java                     # Loads .env file into system properties
│   ├── SecurityConfig.java                # JWT filter chain, CORS, endpoint authorization
│   ├── JwtAuthFilter.java                 # Per-request JWT extraction and validation
│   ├── TelegramBotConfig.java             # Conditional Telegram bot startup logging
│   └── WebConfig.java                     # Global CORS mapping for /api/**
│
├── controller/
│   ├── AuthController.java                # Login, logout, session check (4 endpoints)
│   ├── MonthlyBatchController.java        # Batch CRUD + statistics (10 endpoints)
│   ├── QcController.java                  # QC log CRUD + search (7 endpoints)
│   ├── RetainController.java              # Retain CRUD + search (8 endpoints)
│   ├── ReminderController.java            # Reminder CRUD + batch creation + test-telegram (11 endpoints)
│   ├── TestingDataController.java         # Testing data CRUD + search (9 endpoints)
│   ├── ProductNameController.java         # Product name CRUD + search (5 endpoints)
│   └── RouteController.java               # SPA route forwarding to index.html
│
├── dto/
│   └── MonthlyStatsDTO.java               # Aggregated monthly production statistics
│
├── model/
│   ├── MonthlyBatch.java                  # Production batch entity
│   ├── QcLog.java                         # Quality control log entity
│   ├── Retain.java                        # Retain sample entity
│   ├── Reminder.java                      # Scheduled reminder entity
│   ├── TestingData.java                   # Lab testing data entity (30+ fields)
│   └── ProductName.java                   # Product code-to-name mapping entity
│
├── repository/
│   ├── MonthlyBatchRepository.java        # Includes native SQL stats queries
│   ├── QcLogRepository.java
│   ├── RetainRepository.java
│   ├── ReminderRepository.java            # Includes pending/cleanup queries
│   ├── TestingDataRepository.java
│   └── ProductNameRepository.java
│
└── service/
    ├── JwtService.java                    # Token generation, validation, claim extraction
    ├── LdapAuthService.java               # LDAP bind authentication + user info retrieval
    └── TelegramBotService.java            # HTTP-based Telegram bot with polling + scheduled checks

Dockerfile                                 # Multi-stage Docker build (Maven → JRE Alpine)
.dockerignore                              # Files excluded from Docker context
src/main/resources/
├── application.properties                 # All configuration (env-var driven, includes actuator)
└── import_reminders.sql                   # Sample reminder data (132+ rows)
```

---

## Getting Started

### Prerequisites

* **Java 25** (or compatible JDK)
* **MySQL 8.x+** running and accessible
* **Maven** (or use the included `mvnw` / `mvnw.cmd` wrapper)
* An **LDAP/Active Directory** server for authentication (or modify auth for local dev)
* (Optional) A **Telegram bot** token if you want reminder notifications

### 1\. Clone and configure

```bash
git clone <repository-url>
cd Lab-Spring-Boot-Backend
cp .env.example .env
```

Edit `.env` with your actual values:

```dotenv
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=grease_data
DB_USERNAME=root
DB_PASSWORD=your_password

# LDAP
LDAP_URL=ldap://your-ad-server:389
LDAP_BASE_DN=dc=company,dc=com
LDAP_DOMAIN=company.com
LDAP_USER_SEARCH_BASE=ou=users
LDAP_USER_SEARCH_FILTER=(sAMAccountName={0})

# JWT (generate a strong random secret)
JWT_SECRET=your-256-bit-secret-key-here-make-it-long-and-random
JWT_EXPIRATION=86400000

# Telegram (optional)
TELEGRAM_BOT_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=
TELEGRAM_CHAT_ID=
```

### 2\. Create the database

```sql
CREATE DATABASE grease\_data CHARACTER SET utf8mb4 COLLATE utf8mb4\_unicode\_ci;
```

Hibernate's `ddl-auto=update` will create all tables automatically on first startup.

### 3\. Build and run

```bash
# Using the Maven wrapper
./mvnw spring-boot:run

# Or build a JAR
./mvnw clean package
java -jar target/LabBackend-0.0.1-SNAPSHOT.jar
```

The server starts on port **8080** by default.

### 4\. Verify

```bash
curl http://localhost:8080/api/auth/check
# → {"authenticated":false}

# Health check (via Actuator)
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

---

## Docker Deployment

Both the backend and frontend include Docker support for containerized deployment.

### Backend Dockerfile

Multi-stage build using Maven to compile and Eclipse Temurin JRE Alpine for the runtime:

```bash
# Build and run the backend container
docker build -t lab-backend .
docker run -p 8080:8080 --env-file .env lab-backend
```

**Build stage:** Uses `maven:3.9-eclipse-temurin-25` to compile the project with `mvn clean package -DskipTests`.

**Runtime stage:** Uses `eclipse-temurin:25-jre-alpine` with a non-root user (`appuser`) for security. The `.env` file is optionally copied in but can also be overridden with Docker environment variables.

**Health check:** The container uses `wget` to poll `/actuator/health` every 30 seconds (60s start period for Spring Boot startup).

### Frontend Dockerfile

Multi-stage build using Node.js to build the Angular app and nginx to serve it:

```bash
# Build and run the frontend container
docker build -t lab-frontend .
docker run -p 80:80 lab-frontend
```

**Build stage:** Uses `node:22-alpine` with `npm ci` and `npm run build -- --configuration=production`.

**Runtime stage:** Uses `nginx:alpine` to serve the static Angular build from `dist/labfrontend/browser`.

### nginx.conf (Production Proxy)

The frontend's `nginx.conf` handles three concerns:

1. **API proxying:** `/api/` requests are proxied to `http://backend:8080/api/` (uses Docker service name `backend`)
2. **SPA routing:** All other routes fall through to `index.html` via `try_files $uri $uri/ /index.html`
3. **Static asset caching:** JS, CSS, images, and fonts are cached for 1 year with immutable headers

### Docker Compose (recommended)

To run both services together, use Docker Compose with a service named `backend` for the Spring Boot app (matching the nginx proxy target):

```yaml
services:
  backend:
    build: ./Lab-Spring-Boot-Backend
    ports:
      - "8080:8080"
    env_file: ./Lab-Spring-Boot-Backend/.env

  frontend:
    build: ./Lab-Angular-Frontend
    ports:
      - "80:80"
    depends_on:
      - backend
```

---

## Configuration Reference

All configuration lives in `application.properties` and is driven by environment variables (with sensible defaults). The `.env` file is loaded at startup by both `LabBackendApplication.main()` and `EnvConfig`.

|Property|Env Variable|Default|Description|
|-|-|-|-|
|`spring.datasource.url`|`DB_HOST`, `DB_PORT`, `DB_NAME`|`localhost:3306/grease_data`|JDBC connection URL|
|`spring.datasource.username`|`DB_USERNAME`|`root`|Database username|
|`spring.datasource.password`|`DB_PASSWORD`|*(empty)*|Database password|
|`spring.jpa.hibernate.ddl-auto`|—|`update`|Hibernate schema strategy|
|`jwt.secret`|`JWT_SECRET`|placeholder string|HMAC-SHA signing key|
|`jwt.expiration`|`JWT_EXPIRATION`|`86400000` (24h in ms)|Token lifetime|
|`ldap.url`|`LDAP_URL`|`ldap://localhost:389`|LDAP server URL|
|`ldap.base.dn`|`LDAP_BASE_DN`|`dc=company,dc=com`|LDAP base distinguished name|
|`ldap.domain`|`LDAP_DOMAIN`|*(empty)*|Domain appended to usernames|
|`ldap.user.search.base`|`LDAP_USER_SEARCH_BASE`|`ou=users`|LDAP search base for users|
|`ldap.user.search.filter`|`LDAP_USER_SEARCH_FILTER`|`(sAMAccountName={0})`|LDAP user search filter|
|`telegram.bot.enabled`|`TELEGRAM_BOT_ENABLED`|`false`|Enable/disable Telegram bot|
|`telegram.bot.token`|`TELEGRAM_BOT_TOKEN`|*(empty)*|Bot API token|
|`telegram.bot.username`|`TELEGRAM_BOT_USERNAME`|*(empty)*|Bot username|
|`telegram.chat.id`|`TELEGRAM_CHAT_ID`|*(empty)*|Target chat for notifications|
|`spring.application.name`|—|`LabBackend`|Application name|
|`server.servlet.session.timeout`|`SESSION_TIMEOUT`|`86400` (24h in seconds)|Server session timeout|
|`management.endpoints.web.exposure.include`|—|`health`|Exposed actuator endpoints|
|`management.endpoint.health.show-details`|—|`never`|Health endpoint detail level|

---

## Authentication \& Security

### Authentication Flow

```
Client                  Backend                 LDAP Server
  │                       │                          │
  │  POST /api/auth/login │                          │
  │  {username, password} │                          │
  │──────────────────────>│                          │
  │                       │  LDAP bind (user@domain) │
  │                       │─────────────────────────>│
  │                       │  bind result + user info │
  │                       │<─────────────────────────│
  │                       │                          │
  │                       │  Generate JWT            │
  │                       │  Set auth\_token cookie  │
  │  200 {token, user}    │                          │
  │<──────────────────────│                          │
  │                       │                          │
  │  GET /api/batches     │                          │
  │  Cookie: auth\_token=…│                          │
  │  (or) Authorization:  │                          │
  │       Bearer <token>  │                          │
  │──────────────────────>│                          │
  │                       │  JwtAuthFilter:          │
  │                       │  validate token,         │
  │                       │  set SecurityContext     │
  │  200 \[batch data]    │                          │
  │<──────────────────────│                          │
```

### JWT Details

* **Algorithm:** HMAC-SHA (key derived from `jwt.secret`)
* **Payload:** subject = username, issued-at, expiration
* **Lifetime:** configurable via `jwt.expiration` (default 24 hours)
* **Delivery:** returned in the login response body AND set as an `auth\_token` HTTP-only cookie

### JWT Extraction Priority

The `JwtAuthFilter` checks for a token in this order:

1. `Authorization: Bearer <token>` header
2. `auth\_token` cookie

If neither is present or the token is invalid, the request proceeds unauthenticated (Spring Security will deny it if the endpoint requires auth).

### LDAP Authentication

`LdapAuthService` supports multiple username formats:

* **UPN format:** `user@domain.com` — used as-is
* **Down-level format:** `DOMAIN\\user` — used as-is
* **Plain username:** `user` — automatically appended with `@{ldap.domain}`

On successful bind, the service retrieves `displayName`, `mail`, `cn`, `sAMAccountName`, and `memberOf` from the directory entry.

### Endpoint Authorization

|Pattern|Access|
|-|-|
|`/api/auth/\*\*`|Public (no auth required)|
|`/api/\*\*`|Authenticated (valid JWT required)|
|Everything else|Public (SPA static files, etc.)|

### CORS

Allowed origins: `http://localhost:4200`, `http://localhost:4201`
Allowed methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
Credentials: enabled (required for cookie-based auth)

---

## Database Schema

Hibernate auto-creates and updates tables from JPA entity annotations (`ddl-auto=update`). Below is the logical schema.

### `monthly\_batches` — Production Batches

|Column|Type|Key|Description|
|-|-|-|-|
|`batch`|VARCHAR|PK|Batch identifier (e.g., `NA6102`)|
|`code`|INT||Product code|
|`date\_start`|DATETIME||Production start|
|`date\_end`|DATETIME||Production end|
|`lbs`|INT||Weight in pounds|
|`released`|VARCHAR||`"Yes"` or `"No"`|
|`type`|VARCHAR||Batch type|

### `qc` — Quality Control Logs

|Column|Type|Key|Description|
|-|-|-|-|
|`batch`|VARCHAR|PK|Batch identifier|
|`code`|VARCHAR||Product code|
|`suffix`|VARCHAR||Batch suffix|
|`pen\_60x`|VARCHAR||Penetration at 60 strokes|
|`drop\_point`|VARCHAR||Drop point measurement|
|`date`|VARCHAR||Test date|
|`released\_by`|VARCHAR||Name of person who released|

### `retains` — Retain Samples

|Column|Type|Key|Description|
|-|-|-|-|
|`id`|BIGINT|PK, auto|Auto-generated ID|
|`batch`|VARCHAR||Batch identifier|
|`code`|BIGINT||Product code|
|`date`|DATE||Date retained|
|`box`|BIGINT||Physical box number|

### `testing\_data` — Laboratory Testing Data

|Column|Type|Key|Description|
|-|-|-|-|
|`id`|BIGINT|PK, auto|Auto-generated ID|
|`batch`|VARCHAR||Batch identifier|
|`code`|VARCHAR||Product code|
|`date`|DATE||Test date|
|`pen\_0x`|VARCHAR||Unworked penetration|
|`pen\_60x`|VARCHAR||Worked penetration (60 strokes)|
|`pen\_10k`|VARCHAR||Worked penetration (10,000 strokes)|
|`pen\_100k`|VARCHAR||Worked penetration (100,000 strokes)|
|`drop\_point`|VARCHAR||Drop point (°F or °C)|
|`weld`|VARCHAR||Weld point test|
|`rust`|VARCHAR||Rust prevention test|
|`copper\_corrosion`|VARCHAR||Copper strip corrosion test|
|`oxidation`|VARCHAR||Oxidation stability test|
|`oil\_bleed`|VARCHAR||Oil separation/bleed|
|`spray\_off`|VARCHAR||Spray-off resistance|
|`washout`|VARCHAR||Water washout test|
|`pressure\_bleed`|VARCHAR||Pressure bleed test|
|`roll\_stability\_dry`|VARCHAR||Roll stability (dry)|
|`roll\_stability\_wet`|VARCHAR||Roll stability (wet)|
|`wear`|VARCHAR||Wear test results|
|`ft\_ir`|VARCHAR||Fourier-transform infrared spectroscopy|
|`rheometer`|VARCHAR||Rheometer reading|
|`rheometer\_temp`|VARCHAR||Rheometer temperature|
|`minitest\_minus40`|VARCHAR||Mini-test at -40°C|
|`minitest\_minus30`|VARCHAR||Mini-test at -30°C|
|`minitest\_minus20`|VARCHAR||Mini-test at -20°C|
|`minitest\_0`|VARCHAR||Mini-test at 0°C|
|`minitest\_20`|VARCHAR||Mini-test at 20°C|

### `reminders` — Batch Reminders

|Column|Type|Key|Description|
|-|-|-|-|
|`id`|BIGINT|PK, auto|Auto-generated ID|
|`reminder\_id`|VARCHAR(50)|UNIQUE|Composite key: `{batch}-{interval}`|
|`batch`|VARCHAR(20)|IDX|Batch identifier|
|`interval\_type`|VARCHAR(10)||`48h`, `7d`, `3m`, or `1y`|
|`due`|DATETIME|IDX|When the reminder fires|
|`notified`|BOOLEAN|IDX|Whether notification was sent|
|`created\_at`|DATETIME||Creation timestamp|

### `names` — Product Code Directory

|Column|Type|Key|Description|
|-|-|-|-|
|`code`|INT|PK|Product code|
|`name`|VARCHAR||Product name|

---

## Batch Naming Convention

Batch identifiers encode the production month and year within the name itself. This convention is critical to how the statistics engine works.

**Format:** `\[PREFIX]\[MONTH\_LETTER]\[YEAR\_DIGIT]\[SEQUENCE]\[SUFFIX?]`

```
  N  A  6  1  0  2
  │  │  │  └──┴──┴── Sequence digits (batch number within the month)
  │  │  └─────────── Year digit: 6 → 2020 + 6 = 2026
  │  └────────────── Month letter: A=Jan, B=Feb, ... L=Dec
  └───────────────── Production plant prefix
```

**Month mapping:**

|Letter|Month|Letter|Month|
|-|-|-|-|
|A|January|G|July|
|B|February|H|August|
|C|March|I|September|
|D|April|J|October|
|E|May|K|November|
|F|June|L|December|

**Year mapping:** The third character is a single digit added to 2020. So `6` = 2026, `7` = 2027, etc.

**Examples:**

* `NA6102` → Product N, January 2026, batch 102
* `NB6100` → Product N, February 2026, batch 100
* `NL5003Y` → Product N, December 2025, batch 003, suffix Y

---

## API Reference

All endpoints return JSON. Protected endpoints require a valid JWT (via cookie or Authorization header).

### Authentication — `/api/auth`

#### `POST /api/auth/login`

Authenticates against LDAP and returns a JWT.

**Request:**

```json
{
  "username": "jsmith",
  "password": "secret"
}
```

**Success Response (200):**

```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "username": "jsmith",
    "displayName": "John Smith",
    "email": "jsmith@company.com"
  }
}
```

Also sets an `auth\_token` HTTP-only cookie.

**Failure Response (401):**

```json
{
  "error": "Invalid credentials",
  "message": "Username or password is incorrect"
}
```

#### `POST /api/auth/logout`

Clears the `auth\_token` cookie.

**Response (200):**

```json
{ "success": true, "message": "Logged out successfully" }
```

#### `GET /api/auth/me`

Returns the currently authenticated user.

**Authenticated Response (200):**

```json
{
  "authenticated": true,
  "user": { "username": "jsmith" }
}
```

**Unauthenticated Response (401):**

```json
{ "authenticated": false, "message": "Not authenticated" }
```

#### `GET /api/auth/check`

Lightweight authentication check.

**Response (200):**

```json
{ "authenticated": true }
```

---

### Monthly Batches — `/api/batches`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/batches`|All batches (newest first)|
|`GET`|`/api/batches/{batch}`|Single batch by ID|
|`GET`|`/api/batches/code/{code}`|Batches by product code|
|`GET`|`/api/batches/type/{type}`|Batches by type|
|`GET`|`/api/batches/released/{released}`|Batches by release status (`Yes`/`No`)|
|`GET`|`/api/batches/daterange?start=...\&end=...`|Batches in date range (ISO 8601 datetime)|
|`POST`|`/api/batches`|Create batch|
|`PUT`|`/api/batches/{batch}`|Update batch|
|`DELETE`|`/api/batches/{batch}`|Delete batch|
|`GET`|`/api/batches/stats/years`|List of years with data|
|`GET`|`/api/batches/stats/{year}`|Monthly stats for a year|
|`GET`|`/api/batches/stats`|Monthly stats for current year|

**Batch object:**

```json
{
  "batch": "NA6102",
  "code": 450,
  "dateStart": "2026-01-15T08:00:00",
  "dateEnd": "2026-01-15T16:00:00",
  "lbs": 5000,
  "released": "Yes",
  "type": "Standard"
}
```

**Stats object (returned by `/stats/{year}`):**

```json
{
  "year": 2026,
  "month": 1,
  "monthName": "January",
  "batchesReleased": 12,
  "totalPounds": 60000,
  "reworkBatches": 2,
  "reworkPounds": 10000
}
```

---

### QC Logs — `/api/qc`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/qc`|All QC logs (newest first)|
|`GET`|`/api/qc/{batch}`|QC log for specific batch|
|`GET`|`/api/qc/code/{code}`|QC logs by product code|
|`GET`|`/api/qc/releasedby/{releasedBy}`|QC logs by releaser|
|`GET`|`/api/qc/search?batch=...`|Case-insensitive batch search|
|`POST`|`/api/qc`|Create QC log|
|`PUT`|`/api/qc/{batch}`|Update QC log|
|`DELETE`|`/api/qc/{batch}`|Delete QC log|

**QC log object:**

```json
{
  "batch": "NA6102",
  "code": "450",
  "suffix": "",
  "pen60x": "285",
  "dropPoint": "510",
  "date": "2026-01-15",
  "releasedBy": "John Smith"
}
```

---

### Retains — `/api/retains`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/retains`|All retains (newest first)|
|`GET`|`/api/retains/{id}`|Single retain by ID|
|`GET`|`/api/retains/batch/{batch}`|Retains for a batch|
|`GET`|`/api/retains/code/{code}`|Retains by product code|
|`GET`|`/api/retains/box/{box}`|Retains by box number|
|`GET`|`/api/retains/search?batch=...`|Case-insensitive batch search|
|`GET`|`/api/retains/daterange?start=...\&end=...`|Retains in date range|
|`POST`|`/api/retains`|Create retain (ID auto-generated)|
|`PUT`|`/api/retains/{id}`|Update retain|
|`DELETE`|`/api/retains/{id}`|Delete retain|

**Retain object:**

```json
{
  "id": 1,
  "batch": "NA6102",
  "code": 450,
  "date": "2026-01-15",
  "box": 42
}
```

---

### Testing Data — `/api/testing`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/testing`|All testing data (newest first)|
|`GET`|`/api/testing/{id}`|Single record by ID|
|`GET`|`/api/testing/batch/{batch}`|Records for a batch|
|`GET`|`/api/testing/code/{code}`|Records by product code|
|`GET`|`/api/testing/search?batch=...`|Case-insensitive batch search|
|`GET`|`/api/testing/date/{date}`|Records by date (ISO date)|
|`GET`|`/api/testing/daterange?start=...\&end=...`|Records in date range|
|`POST`|`/api/testing`|Create record|
|`PUT`|`/api/testing/{id}`|Update record|
|`DELETE`|`/api/testing/{id}`|Delete record|

**Testing data object (abbreviated):**

```json
{
  "id": 1,
  "batch": "NA6102",
  "code": "450",
  "date": "2026-01-15",
  "pen0x": "280",
  "pen60x": "285",
  "pen10k": "295",
  "pen100k": "310",
  "dropPoint": "510",
  "weld": "Pass",
  "rust": "Pass",
  "copperCorrosion": "1a",
  "oxidation": "5",
  "oilBleed": "2.1",
  "sprayOff": "Pass",
  "washout": "1.5",
  "pressureBleed": "0.8",
  "rollStabilityDry": "290",
  "rollStabilityWet": "295",
  "wear": "0.45",
  "ftIr": "Match",
  "rheometer": "150",
  "rheometerTemp": "25",
  "minitestMinus40": "Pass",
  "minitestMinus30": "Pass",
  "minitestMinus20": "Pass",
  "minitest0": "Pass",
  "minitest20": "Pass"
}
```

---

### Product Names — `/api/products`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/products`|All products|
|`GET`|`/api/products/{code}`|Product by code|
|`GET`|`/api/products/search?name=...`|Case-insensitive name search|
|`POST`|`/api/products`|Create product|
|`PUT`|`/api/products/{code}`|Update product|
|`DELETE`|`/api/products/{code}`|Delete product|

**Product object:**

```json
{
  "code": 450,
  "name": "Premium Lithium Grease"
}
```

---

### Reminders — `/api/reminders`

|Method|Endpoint|Description|
|-|-|-|
|`GET`|`/api/reminders`|All reminders (ordered by due date)|
|`GET`|`/api/reminders/pending`|Only unnotified reminders|
|`GET`|`/api/reminders/{id}`|Single reminder by database ID|
|`GET`|`/api/reminders/batch/{batch}`|Reminders for a batch|
|`GET`|`/api/reminders/search?batch=...`|Case-insensitive batch search|
|`GET`|`/api/reminders/test-telegram`|Send a test message to the Telegram chat|
|`POST`|`/api/reminders`|Create batch reminders (all 4 intervals) + send Telegram notification|
|`POST`|`/api/reminders/single`|Create a single-interval reminder|
|`PUT`|`/api/reminders/{id}/notified`|Mark reminder as notified|
|`DELETE`|`/api/reminders/{id}`|Delete by database ID|
|`DELETE`|`/api/reminders/reminder/{reminderId}`|Delete by reminder ID string|
|`DELETE`|`/api/reminders/cleanup`|Delete all notified reminders|

**Create batch reminders:**

```json
POST /api/reminders
{
  "batch": "NA6102",
  "dayOffset": -5
}
```

Creates up to 4 reminders (`NA6102-48h`, `NA6102-7d`, `NA6102-3m`, `NA6102-1y`). Skips any that already exist. The optional `dayOffset` shifts all due dates by the given number of days.

**Create single reminder:**

```json
POST /api/reminders/single
{
  "batch": "NA6102",
  "intervalType": "7d",
  "dayOffset": 0
}
```

**Reminder object:**

```json
{
  "id": 1,
  "reminderId": "NA6102-7d",
  "batch": "NA6102",
  "intervalType": "7d",
  "due": "2026-01-22T19:00:00Z",
  "notified": false,
  "createdAt": "2026-01-15T14:30:00Z"
}
```

---

## Reminder System

The reminder system ensures that batches are re-checked at regulated intervals after production. Each batch gets up to four reminders at different time horizons.

### Intervals

|Code|Duration|Purpose|
|-|-|-|
|`48h`|48 hours|Immediate post-production check|
|`7d`|7 days|One-week stability check|
|`3m`|90 days|Quarterly shelf-life check|
|`1y`|365 days|Annual retention check|

### How Due Dates Are Calculated

1. **Base time** is computed as 7:00 PM (19:00 UTC) on the current day: `Instant.now().truncatedTo(DAYS).plus(19, HOURS)`
2. The interval duration is added to the base time
3. If a `dayOffset` is provided, that many days are added (or subtracted if negative)

**Example:** If today is January 15, 2026 and `dayOffset = -5`:

* `48h` → Jan 12, 2026 at 19:00 (base + 48h - 5 days)
* `7d` → Jan 17, 2026 at 19:00 (base + 7d - 5 days)
* `3m` → Apr 11, 2026 at 19:00 (base + 90d - 5 days)
* `1y` → Jan 10, 2027 at 19:00 (base + 365d - 5 days)

### Reminder ID Format

Each reminder has a unique `reminderId` string: `{BATCH}-{INTERVAL}`, for example `NA6102-7d`. This prevents duplicate reminders for the same batch and interval.

### Scheduled Tasks

|Schedule|Task|Description|
|-|-|-|
|Every 30 minutes|`checkDueReminders()`|Queries for reminders where `due < now()` and `notified = false`. Sends a Telegram notification for each, then marks them as notified.|
|Daily at 3:00 AM|`cleanupNotifiedReminders()`|Deletes all reminders where `notified = true` to keep the table clean.|

---

## Telegram Bot Integration

The Telegram bot serves two purposes: it accepts batch submissions via chat messages and delivers reminder notifications.

### Architecture

The bot uses **direct HTTP API calls** via Spring's `RestTemplate` rather than the Telegram bot library's long-polling framework. This approach:

* Uses `RestTemplate` with Apache HttpClient 5 (configured to trust all SSL certificates for environments with corporate proxies)
* Polls for new messages every **5 seconds** via `@Scheduled(fixedRate = 5000)` calling `getUpdates`
* Sends messages via `POST` to the Telegram `sendMessage` API endpoint
* Tracks the last processed update ID to avoid re-processing messages

### Setup

1. Create a bot via [@BotFather](https://t.me/BotFather) on Telegram
2. Note the bot token and username
3. Create a group chat and add the bot
4. Get the chat ID (send a message, then check `https://api.telegram.org/bot<TOKEN>/getUpdates`)
5. Set the environment variables:

```dotenv
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=123456:ABC-DEF...
TELEGRAM_BOT_USERNAME=MyGreaseBot
TELEGRAM_CHAT_ID=-1001234567890
```

### Testing the Connection

Use the test endpoint to verify Telegram is configured correctly:

```bash
curl http://localhost:8080/api/reminders/test-telegram
# → "Message sent successfully"
```

### Bot Commands (via Chat Messages)

The bot does not use Telegram's `/command` system. Instead, it pattern-matches raw messages.

**Pattern:** `[A-Z]{2}\d{4}[A-Z]?` optionally followed by `,OFFSET`

|Message|Effect|
|-|-|
|`NA6102`|Creates 4 reminders for batch NA6102 (no offset)|
|`NA6102,-5`|Creates 4 reminders with a -5 day offset|
|`NB6100Y`|Creates 4 reminders for batch NB6100Y|

**Bot responses** use Markdown formatting:

* `Batch *NA6102* added!`
* `Batch *NA6102* added with *-5 day* offset!`

### Notification Messages

When a reminder comes due, the bot sends:

```
Batch NA6102 - 7d check is due!
```

### Conditional Behavior

When `telegram.bot.token` is empty or not configured:

* `sendMessage()` silently returns without making HTTP calls
* `pollForMessages()` silently returns without polling
* The `TelegramBotService` bean still exists (it's injected by `ReminderController`) but all Telegram operations are no-ops

### ReminderController Integration

The `ReminderController` delegates reminder creation to `TelegramBotService.addBatchReminders()` and uses Java record types for request DTOs:

```java
public record BatchReminderRequest(String batch, Integer dayOffset) {}
public record SingleReminderRequest(String batch, String intervalType, Integer dayOffset) {}
```

When creating batch reminders via `POST /api/reminders`, the controller also sends a Telegram notification confirming the batch was added.

---

## Monthly Statistics Engine

The `/api/batches/stats/{year}` endpoint returns aggregated monthly production data by parsing batch names using the [batch naming convention](#batch-naming-convention).

### How It Works

The statistics are computed via a native SQL query in `MonthlyBatchRepository`:

1. **Extract month:** `ASCII(SUBSTRING(batch, 2, 1)) - 64` converts the second character to a month number (A=1, B=2, ..., L=12)
2. **Extract year:** `CAST(SUBSTRING(batch, 3, 1) AS UNSIGNED)` gets the year digit, then adds 2020
3. **Filter:** Only batches where the second character is between 'A' and 'L' (valid months)
4. **Aggregate:** Groups by year and month, then for each group:

   * `batchesReleased` — count where `released = 'Yes'`
   * `totalPounds` — sum of `lbs` where `released = 'Yes'`
   * `reworkBatches` — count where `released = 'No'`
   * `reworkPounds` — sum of `lbs` where `released = 'No'`

### Available Years

`GET /api/batches/stats/years` returns a descending list of all years that have batch data, extracted from batch names using the same parsing logic.

### Response Format

```json
\[
  {
    "year": 2026,
    "month": 1,
    "monthName": "January",
    "batchesReleased": 15,
    "totalPounds": 75000,
    "reworkBatches": 3,
    "reworkPounds": 15000
  },
  {
    "year": 2026,
    "month": 2,
    "monthName": "February",
    "batchesReleased": 12,
    "totalPounds": 60000,
    "reworkBatches": 1,
    "reworkPounds": 5000
  }
]
```

---

## SPA Route Forwarding

`RouteController` forwards the following client-side routes to `index.html` so the Angular frontend handles routing:

* `/`
* `/qc`
* `/results`
* `/update`
* `/retains`
* `/reminders`

This enables the backend to serve the Angular SPA as static files while still handling API routes separately.

