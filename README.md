# BhashaMitra Platform

**BhashaMitra** is a usage-first Indian language learning platform that helps learners understand how Indian languages are *actually spoken and written* — through words, sentences, audio, and contextual explanations.

Starting with **Marathi** and expanding to **Hindi**, **Gujarati**, and other languages.

## Tech Stack

- **Backend**: Java 21 + Spring Boot 4.0.1
- **Database**: MySQL 8.0 (Aurora Serverless v2 in production)
- **Frontend**: React + TypeScript + Vite
- **Infrastructure**: AWS (ECS Fargate, ALB, Route 53, S3 to store audio files)
- **CI/CD**: GitHub Actions with OIDC

## Local Development

### Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Maven 3.9+** for building the backend
- **Node.js 20+** and **npm** for frontend development
- **MySQL 8.0+** for local database

### Database Setup

1. **Install MySQL 8.0+** locally

2. **Create the database and user:**
   ```sql
   CREATE DATABASE bhashamitra CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   CREATE USER 'bhashamitra'@'localhost' IDENTIFIED BY 'bhashamitra';
   GRANT ALL PRIVILEGES ON bhashamitra.* TO 'bhashamitra'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Configure local profile:** Create `application-local.yml` with your local database connection settings

### Running Locally

#### Quick Start (Recommended)

For the fastest development workflow, use the provided rebuild-and-run script:

```bash
# Build with tests, then run (stops any running instance first)
./rebuild-and-run.sh
```

**What this script does:**
- ✅ Stops any running Spring Boot instance on port 8080
- ✅ Runs `mvn clean package` (includes all tests)
- ✅ Only starts the app if tests pass (prevents running with broken code)
- ✅ Starts Spring Boot with the local profile

**Why use this script:**
- Saves time: Single command vs typing multiple commands
- Safety: Tests must pass before app starts (catch errors early)
- Port management: Automatically frees up port 8080 if needed
- Consistency: Same workflow every time you test changes

#### Manual Approach

If you prefer manual control:

```bash
# Backend (with local profile)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend development (optional - if you want separate dev server)
cd frontend && npm install && npm run dev

# Access points
# - Full app: http://localhost:8080/
# - Health check: http://localhost:8080/actuator/health
# - API endpoints: http://localhost:8080/api/
```

### Database Schema

The application uses **Liquibase** for database schema management with master changelog at `db/changelog/db.changelog-master.xml`. Schema changes are version-controlled and automatically applied on startup in both local and production environments.

## Documentation

- **[Language Core](docs/language-core.md)** - Platform philosophy and language modeling approach
- **[Infrastructure](docs/infrastructure.md)** - AWS architecture and deployment details
- **[Disaster Recovery](docs/disaster-recovery.md)** - Complete DR procedures and backup strategies

## Status

🚧 **Active Development** - Editorial Workflows Complete

- **Infrastructure**: ✅ Production-ready AWS infrastructure deployed (ECS Fargate, Aurora MySQL, Cognito, S3)
- **Backend API**: ✅ Complete REST API for all content entities
- **Editorial UI v1**: ✅ Complete admin/editor workflows
  - Lemmas (list, create, edit, status workflow, pagination, filtering)
  - Meanings (CRUD via modal, linked to lemmas)
  - Surface Forms (CRUD via modal, linked to lemmas)
  - Pronunciations (CRUD via modal, linked to lemmas/sentences)
  - Usage Sentences (list, create, edit, status workflow)
  - Languages (admin management)
- **Authentication**: ✅ Cognito OAuth2 with role-based access (admin, editor, learner)
- **Initial focus**: Marathi language core
- **Next**: Public learner-facing UI

## Anuja's change