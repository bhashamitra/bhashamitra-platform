# BhashaMitra Platform

**BhashaMitra** is a usage-first Indian language learning platform.

The platform aims to help learners understand how Indian languages are
*actually spoken and written* — through words, sentences, audio, and
contextual explanations — starting with **Marathi** and expanding to
**Hindi**, **Gujarati**, and other languages.

---

## What BhashaMitra Focuses On

Instead of treating language as a static dictionary,
BhashaMitra treats language as **living usage**.

The platform emphasizes:
- Real-world usage over abstract definitions
- Spoken and written language distinctions
- High-frequency vocabulary
- Contextual learning through sentences
- Clear explanations for learners

---

## MVP Direction (Phase 1)

The first phase focuses on building a strong **language core**, which includes:

- Word entries (lemmas)
- Common inflections and variants
- Real usage sentences
- Pronunciation audio
- Basic search and navigation

Words are the starting point — not the final form of the product.

---

## Future Directions (Not in MVP)

The platform is intentionally designed to grow into:
- Phrase and sentence libraries
- Guided lessons
- Grammar explanations tied to usage
- Learner progress and practice
- Multiple Indian languages using a shared model

These will be added incrementally.

---

## Infrastructure

The BhashaMitra platform is deployed on AWS using a modern, scalable architecture designed for production workloads.

### Architecture Overview

- **Backend**: Java Spring Boot (containerized)
- **Database**: Aurora Serverless v2 MySQL
- **Frontend**: React TypeScript (served by Spring Boot)
- **Deployment**: ECS Fargate with Application Load Balancer
- **Domain**: https://bhashamitra.com/

### AWS Infrastructure Components

#### **Existing Resources (Reused)**
- **VPC**: `mvl-vpc` (10.0.0.0/16) in us-west-1
- **Public Subnet**: us-west-1a for ALB and ECS tasks
- **Internet Gateway**: Existing routing configuration
- **Route 53 Hosted Zone**: `bhashamitra.com` with SSL certificates

#### **New Infrastructure (Created)**

**Database Layer:**
- **Aurora Serverless v2 MySQL**: Auto-scaling (0.5-2.0 ACUs)
- **Multi-AZ Setup**: Private subnets in us-west-1a and us-west-1c
- **Security**: Encrypted storage, private subnet isolation
- **Credentials**: AWS Secrets Manager with separate app user

**Container Platform:**
- **ECS Fargate Cluster**: Serverless container orchestration
- **Task Definition**: 0.25 vCPU, 512MB memory configuration
- **Auto Scaling**: Based on CPU and memory utilization
- **Health Checks**: Dual-layer monitoring
  - Container Health: Spring Boot Actuator liveness endpoint (`/actuator/health/liveness`)
  - Load Balancer Health: Spring Boot Actuator readiness endpoint (`/actuator/health/readiness`)

**Load Balancing & SSL:**
- **Application Load Balancer**: Multi-AZ across public subnets
- **SSL Certificate**: Free AWS ACM certificate with auto-renewal
- **HTTPS-Only**: Automatic HTTP to HTTPS redirects (301)
- **Domain Redirects**: www.bhashamitra.com → bhashamitra.com (301)

**Security & Networking:**
- **Security Groups**: Layered defense (ALB → ECS → Aurora)
- **Private Subnets**: Database isolation without internet access
- **IAM Roles**: Least privilege for ECS tasks and GitHub Actions
- **Secrets Management**: Database credentials via AWS Secrets Manager

**CI/CD & Deployment:**
- **GitHub Actions OIDC**: Secure authentication without access keys
- **ECR Integration**: Container image storage with immutable tags
- **Automated Deployments**: Push to ECR → Update ECS service
- **Task Definition Management**: Committed task definition file (`ecs/taskdef.json`) for deterministic deployments

**Monitoring & Logging:**
- **CloudWatch Logs**: ECS task and application logging
- **Container Insights**: ECS cluster monitoring
- **Health Monitoring**: ALB and ECS health checks

### Infrastructure as Code

All infrastructure is managed using **Terraform** with:
- **Remote State**: S3 backend with DynamoDB locking
- **Modular Design**: Separate files for each component
- **Environment Separation**: Production-ready configuration
- **Cost Optimization**: Serverless and auto-scaling components

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                                  Internet                                        │
└─────────────────────────────┬───────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                          Route 53 DNS                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │  bhashamitra.com    │  │ www.bhashamitra.com │  │   SSL Certificate       │  │
│  │        (A)          │  │        (A)          │  │     (ACM)               │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                    Application Load Balancer                                    │
│                           (Multi-AZ)                                            │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │   HTTP Listener     │  │   HTTPS Listener    │  │    SSL Termination      │  │
│  │   (Port 80)         │  │   (Port 443)        │  │                         │  │
│  │   301 → HTTPS       │  │   Forward to ECS    │  │   www → non-www         │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                         VPC (mvl-vpc)                                           │
│                        10.0.0.0/16                                              │
│                                                                                 │
│  ┌─────────────────────────────────────┐  ┌─────────────────────────────────────┐│
│  │        Public Subnets               │  │        Private Subnets              ││
│  │                                     │  │                                     ││
│  │  ┌─────────────────────────────────┐│  │  ┌─────────────────────────────────┐││
│  │  │         us-west-1a              ││  │  │         us-west-1a              │││
│  │  │      10.0.8.0/24                ││  │  │      10.0.1.0/24                │││
│  │  │                                 ││  │  │                                 │││
│  │  │  ┌─────────────────────────────┐││  │  │                                 │││
│  │  │  │      ECS Fargate            │││  │  │                                 │││
│  │  │  │                             │││  │  │                                 │││
│  │  │  │ ┌─────────────────────────┐ │││  │  │                                 │││
│  │  │  │ │    Spring Boot App      │ │││  │  │                                 │││
│  │  │  │ │      Port 8080          │ │││  │  │                                 │││
│  │  │  │ │                         │ │││  │  │                                 │││
│  │  │  │ │   Container Health:     │ │││  │  │                                 │││
│  │  │  │ │   /actuator/health/     │ │││  │  │                                 │││
│  │  │  │ │   liveness              │ │││  │  │                                 │││
│  │  │  │ └─────────────────────────┘ │││  │  │                                 │││
│  │  │  └─────────────────────────────┘││  │  │                                 │││
│  │  └─────────────────────────────────┘│  │  └─────────────────────────────────┘││
│  │                                     │  │                                     ││
│  │  ┌─────────────────────────────────┐│  │  ┌─────────────────────────────────┐││
│  │  │         us-west-1c              ││  │  │         us-west-1c              │││
│  │  │      10.0.3.0/24                ││  │  │      10.0.2.0/24                │││
│  │  │                                 ││  │  │                                 │││
│  │  │     (ALB Secondary AZ)          ││  │  │  ┌─────────────────────────────┐│││
│  │  │                                 ││  │  │  │    Aurora Serverless        ││││
│  │  │                                 ││  │  │  │      MySQL 8.0              ││││
│  │  │                                 ││  │  │  │                             ││││
│  │  │                                 ││  │  │  │   Auto-scaling:             ││││
│  │  │                                 ││  │  │  │   0.5 - 2.0 ACUs            ││││
│  │  │                                 ││  │  │  │                             ││││
│  │  │                                 ││  │  │  │   Database: bhashamitra     ││││
│  │  │                                 ││  │  │  │   Users: bmadmin,           ││││
│  │  │                                 ││  │  │  │          bhashamitra        ││││
│  │  │                                 ││  │  │  └─────────────────────────────┘│││
│  │  └─────────────────────────────────┘│  │  └─────────────────────────────────┘││
│  └─────────────────────────────────────┘  └─────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────────────┐
│                        AWS Services                                             │
│                                                                                 │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │   Secrets Manager   │  │    CloudWatch       │  │         ECR             │  │
│  │                     │  │                     │  │                         │  │
│  │  DB Credentials:    │  │  ┌─────────────────┐│  │  Container Images:      │  │
│  │  • bmadmin (AWS)    │  │  │ ECS Logs        ││  │  • bhashamitra-platform │  │
│  │  • bhashamitra      │  │  │ /ecs/bhashamitra││  │                         │  │
│  │    (Terraform)      │  │  └─────────────────┘│  │                         │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘

Security Groups:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ ALB Security    │    │ ECS Security    │    │Aurora Security  │
│ Group           │    │ Group           │    │ Group           │
│                 │    │                 │    │                 │
│ Inbound:        │    │ Inbound:        │    │ Inbound:        │
│ • 80 (Internet) │───▶│ • 8080(from ALB)│───▶│ • 3306 (from    │
│ • 8080(Internet) │    │                 │    │   ECS only)     │
│                 │    │ Outbound:       │    │                 │
│ Outbound:       │    │ • All (Internet)│    │ Outbound:       │
│ • 8080 (to ECS) │    │                 │    │ • None          │
└─────────────────┘    └─────────────────┘    └─────────────────┘

GitHub Actions CI/CD:
┌─────────────────────────────────────────────────────────────────────────────────┐
│  GitHub Repository: bhashamitra/bhashamitra-platform                           │
│                                                                                 │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │   OIDC Provider     │  │   IAM Role          │  │   Deployment Pipeline   │  │
│  │                     │  │                     │  │                         │  │
│  │  • No Access Keys   │  │  Permissions:       │  │  1. Build Docker Image  │  │
│  │  • Secure Auth      │  │  • ECR Push/Pull    │  │  2. Push to ECR         │  │
│  │  • Repository       │  │  • ECS Update       │  │  3. Update ECS Service  │  │
│  │    Scoped           │  │  • Secrets Access   │  │  4. Health Check        │  │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### Security Features

- **Network Isolation**: Private subnets for database
- **Encryption**: SSL/TLS in transit, encrypted storage at rest
- **Access Control**: IAM roles with minimal permissions
- **Secrets Management**: No hardcoded credentials
- **Security Groups**: Port-specific access controls

### Cost Optimization

- **Aurora Serverless**: Pay only for actual database usage
- **ECS Fargate**: No EC2 instances to manage or pay for when idle
- **Auto Scaling**: Resources scale based on demand
- **Free SSL**: AWS Certificate Manager certificates
- **Efficient Networking**: Reused existing VPC infrastructure

---

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

---

## Architecture (high-level)

- Backend: Java (Spring Boot)
- Database: MySQL
- Frontend: Web
- Audio: Object storage (e.g., S3)

Details will evolve.

---

## Philosophy

- Build a solid language core first
- Optimize for learners, not linguists
- Quality over quantity
- Grow deliberately

---

## Status

🚧 Project initialized — Shri Ganesha phase.

Initial focus: Marathi language core.

**Infrastructure**: ✅ Production-ready AWS infrastructure deployed
**Application**: 🚧 Spring Boot development in progress
