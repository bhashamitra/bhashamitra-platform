# BhashaMitra Platform

**BhashaMitra** is a usage-first Indian language learning platform that helps learners understand how Indian languages are *actually spoken and written* — through words, sentences, audio, and contextual explanations.

Starting with **Marathi** and expanding to **Hindi**, **Gujarati**, and other languages.

## Tech Stack

- **Backend**: Java 21 + Spring Boot 3
- **Database**: MySQL 8.0 (Aurora Serverless v2 in production)
- **Frontend**: React + TypeScript + Vite
- **Infrastructure**: AWS (ECS Fargate, ALB, Route 53)
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

```bash
# Backend (with local profile)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend development (optional)
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

🚧 **Active Development** - Shri Ganesha phase

- **Infrastructure**: ✅ Production-ready AWS infrastructure deployed
- **Application**: 🚧 Spring Boot development in progress
- **Initial focus**: Marathi language core
