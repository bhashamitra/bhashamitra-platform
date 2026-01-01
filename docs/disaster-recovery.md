# **BhashaMitra Disaster Recovery Guide**


## 🎯 Recovery Objectives

* **RTO (Recovery Time Objective)**: ~75 minutes
* **RPO (Recovery Point Objective)**:

  * **Database**: Last successful Aurora snapshot
  * **Cognito users**: Last Cognito backup (weekly/monthly)

---

## 🚨 Start Here

```bash
export AWS_PROFILE=prod
cd bhashamitra-platform/infra
terraform init -upgrade
```

---

## 🚨 Complete Disaster Recovery Sequence

---

## 🛑 Stop Conditions (Do NOT Proceed)

* AWS account access is impaired (`aws sts get-caller-identity` fails)
* Terraform backend/state is inaccessible or missing
* Route53 hosted zone is deleted or domain ownership is lost
* Cognito user pool is destroyed **and** no recent Cognito backup exists
* You are unsure whether you are operating in **production vs staging**

If any of the above are true, stop and reassess before proceeding.

---

## 🧭 Choose Your Recovery Path

* **Path A – Infra-only rebuild**
  App down, Cognito and DB intact
  → Steps **1, 3A, 4–10**

* **Path B – Full rebuild + Cognito restore**
  Infra destroyed, Cognito lost
  → Steps **1–10 + 7 + 12**

* **Path C – Full rebuild + Database restore**
  Application data must be recovered
  → Steps **1–10 + 11**

---

## 📋 Prerequisites

* AWS CLI access with sufficient permissions
* Terraform installed (with `null` provider support)
* GitHub repository access with workflow permissions
* **Recent Cognito user backup** (see backup procedure below)

---

## 🛡️ CRITICAL: Cognito User Protection

### Before any disaster

```bash
cd bhashamitra-platform
./scripts/backup-cognito-users.sh
```

### Current Protections

* ✅ `deletion_protection = "ACTIVE"`
* ✅ `lifecycle { prevent_destroy = true }`
* ✅ User export/import scripts available

---

## 🎯 Proven Disaster Recovery Process

**This process has been tested end-to-end and verified to work reliably.**

---

## Step 1: Assess the Damage

```bash
aws sts get-caller-identity
dig bhashamitra.com
curl -I https://bhashamitra.com

aws cognito-idp describe-user-pool \
  --user-pool-id $(terraform output -raw cognito_user_pool_id)
```

---

## Step 2: Backup Critical Data (If Accessible)

```bash
./scripts/backup-aurora.sh
./scripts/backup-cognito-users.sh
```

List available snapshots:

```bash
aws rds describe-db-cluster-snapshots \
  --db-cluster-identifier bhashamitra-aurora-cluster \
  --query 'DBClusterSnapshots[?starts_with(DBClusterSnapshotIdentifier, `disaster-recovery`)].{ID:DBClusterSnapshotIdentifier,Created:SnapshotCreateTime,Status:Status}' \
  --output table
```

---

## Step 3: Handle Cognito User Pool

### Option A: Cognito Survived

```bash
aws cognito-idp list-users \
  --user-pool-id $(terraform output -raw cognito_user_pool_id) \
  --limit 10
```

### Option B: Cognito Destroyed (Emergency Only)

1. Temporarily remove `prevent_destroy` from `cognito.tf`
2. Run `terraform destroy`
3. Restore `prevent_destroy`
4. Proceed with user restoration steps

---

## Step 4: Destroy What You Can (Cognito Should Be Protected)

```bash
cd bhashamitra-platform/infra
terraform init -upgrade
terraform destroy -auto-approve
```

* Expected duration: **15–20 minutes**
* Failure due to `prevent_destroy` is **expected and desired**

---

## Step 5: Rebuild Infrastructure (Bulletproof Process)

```bash
terraform init -upgrade
terraform plan
terraform apply -auto-approve
```

### Reliability Fixes Applied

* 120-second DNS propagation delay
* Explicit dependency chain for Cognito domain
* ACM certificate in `us-east-1`
* Apex A record created **before** Cognito domain

### Expected State After Rebuild

* ✅ ECS cluster running (nginx placeholder)
* ✅ Aurora cluster available (empty or restored)
* ✅ ALB reachable (502 expected)
* ✅ DNS resolving correctly
* ✅ Cognito custom domain operational
* ⚠️ App not yet deployed

---

## Step 6: Create Application Database User

```bash
./scripts/create-app-user.sh
```

* Uses RDS Data API
* Creates `bhashamitra` DB user
* Verifies permissions

---

## Step 7: Restore Cognito Users (If Needed)

```bash
./scripts/restore-cognito-users.sh \
  ./cognito-backups/users-backup-YYYYMMDD-HHMMSS.json \
  $(terraform output -raw cognito_user_pool_id)
```

* Users restored with temporary password
* Group memberships restored
* Password reset required on first login

---

## Step 8: Verify Infrastructure Health

```bash
curl -I https://bhashamitra.com/
curl -I http://bhashamitra.com/
curl -I https://www.bhashamitra.com/
curl -I https://auth.bhashamitra.com/
```

Expected:

* 502 on main site (pre-deployment)
* HTTPS redirect working
* Cognito Hosted UI returns 200

---

## Step 9: Deploy Application

```bash
gh workflow run deploy.yml
gh run list --limit 1
gh run view --log
```

Expected duration: **5–10 minutes**

---

## Step 10: Verify Complete Recovery

```bash
curl -f https://bhashamitra.com/actuator/health/liveness
curl -f https://bhashamitra.com/actuator/health/readiness
curl -f https://bhashamitra.com/actuator/health/db
curl -I https://bhashamitra.com/api/me
```

Authentication expectations:

* `/api/*` redirects to Cognito if unauthenticated
* Successful login returns user data

---

## Step 11: Restore Database (If Needed)

```bash
./scripts/restore-aurora.sh disaster-recovery-YYYYMMDD-HHMMSS bhashamitra-aurora-cluster-restored
```

Notes:

* New cluster is created alongside existing
* Application config must be updated to new endpoint
* Original cluster remains untouched

---

## Step 12: Send Password Reset Emails

```bash
aws cognito-idp list-users \
  --user-pool-id $(terraform output -raw cognito_user_pool_id) \
  --query 'Users[].Username' --output text | \
  xargs -I {} aws cognito-idp admin-reset-user-password \
    --user-pool-id $(terraform output -raw cognito_user_pool_id) \
    --username {}
```

---

## Step 13: Post-Recovery Tasks

```bash
echo "COGNITO_CLIENT_ID=$(terraform output -raw cognito_client_id)"
echo "COGNITO_USER_POOL_ID=$(terraform output -raw cognito_user_pool_id)"
```

* Update monitoring and alerting
* Notify stakeholders
* Document incident and lessons learned
* Review and update DR procedures

---

## ✅ Recovery Complete Checklist

* [ ] `https://bhashamitra.com/` returns 200
* [ ] Liveness & readiness probes return UP
* [ ] `/api/me` redirects to Cognito when logged out
* [ ] Cognito Hosted UI loads successfully
* [ ] At least one user can log in
* [ ] Database integrity validated (if restored)

---

## Expected Timeline

* Steps 1–3: ~5 minutes
* Step 4: 15–20 minutes
* Step 5: 15–20 minutes
* Step 6: <1 minute
* Step 7: 1–2 minutes
* Step 8: ~2 minutes
* Step 9: 5–10 minutes
* Step 10: ~5 minutes
* Optional DB restore & cleanup: 10–30 minutes

**Total RTO: ~45–75 minutes**

---

## 🔧 What Survives vs What Doesn’t

### Survives

* GitHub repository
* Route53 hosted zone
* Cognito user pool (protected)
* Aurora snapshots
* Secrets Manager
* ECR images

### Lost Without Backups

* User passwords
* Active sessions
* Application data
* Local configuration values

---

## 🧪 Testing & Validation

* Monthly: Steps 1–8 in staging
* Quarterly: Full DR including user restore
* Annually: Full disaster simulation

---

## Emergency Contacts

* AWS Support: [details]
* Domain Registrar: [details]
* Team Lead: [details]
* Database Admin: [details]

---

**🚨 This runbook is production-grade. Test it regularly.**
