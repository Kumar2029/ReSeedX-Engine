# 🔐 ReSeedX Engine

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-HS256-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![AES-256](https://img.shields.io/badge/AES--256--GCM-Encrypted-red?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

**A 3-node fault-tolerant encrypted cloud backup system with blockchain audit trail.**
*Every file. Triple encrypted. Blockchain verified. Always recoverable.*

---

## 🚀 Overview

**ReSeedX Engine** is a distributed cloud backup system that solves the most fundamental flaw in traditional storage — the **single point of failure**. When a conventional storage server crashes, your data is gone permanently. ReSeedX eliminates this risk by storing every uploaded file simultaneously across **three independent encrypted nodes**, each applying a completely different encryption strategy.

If Node A crashes, the system silently fails over to Node B. If Node B also fails, it falls back to Node C. As long as even **one node is alive**, your data is fully recoverable — automatically, instantly, and without any user intervention.

Every operation — upload, delete, recovery — is recorded in an **immutable blockchain audit trail** using SHA-256 hash-chaining with proof-of-work. If anyone tampers with a single record, the entire chain breaks and the system detects it instantly.

At the core of the system is the **SeedBlock Algorithm**: a per-user 256-bit cryptographic key derived by XORing the user's Client ID with a cryptographically random value. This seed powers all XOR layering operations, ensuring that no two users' encrypted data is structurally similar — even for identical source files.

### Who Is This For?

- Students and developers learning **distributed systems** and **applied cryptography**
- Anyone building a **fault-tolerant file storage** proof of concept
- Projects requiring **multi-node redundancy** with layered encryption
- Security researchers studying **blockchain audit mechanisms**
- BCA/MCA/BTech final year project demonstrations

---

## ✨ Features

### 🛡️ Security & Encryption
- **SeedBlock Algorithm** — Per-user 256-bit XOR seed derived from `ClientID ⊕ Random(256-bit)`
- **AES-256-GCM** on Node A — Military-grade authenticated encryption with random 12-byte IV per file and 128-bit authentication tag
- **Double XOR** on Node B using seed + reversed ClientID bytes
- **Triple XOR** on Node C using seed + clientId + reversed seed — three independent XOR passes
- **BCrypt** password hashing — salted, deliberately slow, non-reversible
- **JWT HS256** signed tokens — stateless authentication, tamper detection via signature
- **File name sanitization** — strips path traversal, null bytes, and directory attacks

### ⛓️ Blockchain Audit Trail
- **SHA-256 hash-chained** immutable records — each block references the previous block's hash
- **Proof-of-work** mining — difficulty = 2 leading zeros, prevents trivial forgery
- **Chain validation** — one-click integrity check detects any tampering
- **Terminal logging** — full blockchain details (hashes, nonces, timestamps) printed to server console for technical demonstration
- **Clean UI** — user-facing view shows only action, timestamp, and verification status

### 🔄 Fault Tolerance
- **3-node simultaneous storage** — every file backed up to all three nodes at upload time
- **Automatic failover chain** — Node A → Node B → Node C, no user action required
- **Per-node deletion** — remove a file from a specific node without affecting others
- **Node crash simulation** — crash and restore individual nodes via Admin controls
- **Real-time topology** — live SVG diagram shows node states with animated data-flow lines

### 🗜️ Data Processing
- **GZIP compression** before encryption — reduces storage footprint for compressible files
- **Smart bypass** — skips compression if savings < 5%
- **SHA-256 deduplication** — identical files detected at upload; no redundant storage
- **SHA-256 integrity verification** — every recovery mathematically proves the data is intact
- **Multi-threaded XOR** — files over 1 MB split into 1 MB chunks processed in parallel across a shared 4-thread pool

### 📊 Security Analysis Dashboard
- **Encryption Strength** — Shannon entropy analysis showing randomness before vs after encryption
- **File Integrity** — Merkle tree verification splitting files into segments with independent hashing
- **Speed Test** — Real-time benchmarking of XOR, Double XOR, AES-256-GCM, ChaCha20, SHA-256
- **Zero-Knowledge Proofs** — Challenge-response verification proving node holds correct data

### 👥 Team Collaboration
- **Organizations** — Create teams and invite members by username
- **File sharing** — Share encrypted files within teams; members can download shared files
- **Role-based access** — Owner, Admin, Member roles within each organization
- **Cross-user download** — Team members can download files shared with their organization

### 📈 Additional Features
- **Admin panel** — User management, system stats, create admin accounts
- **PDF report generation** — Downloadable system analytics report with file statistics
- **Rate limiting** — 60 requests/minute per IP with automatic eviction of stale entries
- **File versioning** — Version history tracked per file with change summaries
- **Paginated file list** — Efficient browsing with page/size parameters
- **Swagger UI** — Full interactive API documentation at `/swagger-ui.html`
- **Built-in load testing** — Automated stress test across 1KB–10MB file sizes


---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 LTS | Core language |
| Spring Boot | 3.2.0 | Web framework, embedded Tomcat |
| Spring Security | Bundled | Filter chain, authentication |
| Spring Data JPA | Bundled | ORM layer |
| Hibernate | 6.3.1 | SQL generation, DDL auto |
| jjwt | 0.11.5 | JWT token generation & validation |
| iTextPDF | 5.5.13.3 | PDF report generation |
| Bucket4j | 8.7.0 | API rate limiting |
| springdoc-openapi | 2.3.0 | Swagger UI & OpenAPI docs |
| Maven | 3.8+ | Dependency management & build |

### Frontend
| Technology | Purpose |
|---|---|
| HTML5 | Structure of login and dashboard pages |
| Tailwind CSS (CDN) | Utility-first styling |
| Vanilla JavaScript | API calls, dynamic rendering, topology diagram |
| Google Material Symbols | Navigation and UI icons |
| Inline SVG + JS | Real-time animated node topology diagram |

### Database
| Technology | Purpose |
|---|---|
| MySQL 8.0+ | Primary relational database |
| JPA / Hibernate | ORM — entities mapped to tables automatically |

### Encryption & Security
| Algorithm | Usage |
|---|---|
| AES-256-GCM | Primary node encryption with authentication tag |
| XOR (1x/2x/3x) | Node B and C layered encryption |
| SHA-256 | File fingerprinting, deduplication, integrity, blockchain |
| BCrypt | Password hashing |
| HS256 | JWT token signing |

---

## 🏗️ Architecture / How It Works

### System Layers

```
┌───────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                       │
│          index.html (Login)   dashboard.html (SPA)        │
│          Tailwind CSS · Material Icons · SVG Topology      │
└────────────────────────┬──────────────────────────────────┘
                         │ HTTP + JWT Bearer Token
┌────────────────────────▼──────────────────────────────────┐
│                 APPLICATION LAYER                          │
│  JwtFilter → SecurityConfig → Controllers → Services      │
│  AuthController · FileController · NodeController         │
│  AdminController · AnalyticsController · TeamController   │
│  SBAService (Core) · BlockchainAuditService               │
│  NodeStatusService · CryptoAnalysisService · MetricsService│
└────────────────────────┬──────────────────────────────────┘
                         │ JPA / Hibernate
┌────────────────────────▼──────────────────────────────────┐
│                    DATA LAYER (MySQL)                      │
│  clients · cloud_files · backup_files                     │
│  secondary_backups · tertiary_backups · audit_blocks      │
│  file_node_mapping · audit_logs · file_versions           │
│  organizations · team_members · shared_files              │
└────────┬──────────────────┬──────────────────┬────────────┘
         │                  │                  │
   NODE A (PRIMARY)   NODE B (SECONDARY)  NODE C (TERTIARY)
   AES-256-GCM        Double XOR          Triple XOR
   + Multi-thread XOR  + ClientId XOR     + Reversed Seed XOR
```

### Upload Data Flow

```
User uploads file
       │
       ▼
JwtFilter validates token → extracts ClientID
       │
       ▼
SBAService.uploadFile()
  1. Compute SHA-256(rawBytes) → check deduplication
  2. GZIP compress (skip if <5% savings)
  3. Fetch client's SeedBlock from DB
  4. Node A: xorMultiThreaded() → aesEncrypt() → backup_files
  5. Node B: doubleXor() → secondary_backups
  6. Node C: tripleXor() → tertiary_backups
  7. Save metadata + 3 FileNodeMapping records
  8. BlockchainAuditService.addBlock() → mine with PoW
  9. Create FileVersion record (v1)
       │
       ▼
Return: fileId, SHA-256 hash, XOR time, AES time, compression ratio
Terminal: Full blockchain block printed with hashes
```

### Recovery Failover Flow

```
recoverPrimary(fileId)
       │
       ├── Node A ACTIVE? ──YES──► AES decrypt → Un-XOR → Decompress → SHA-256 verify ──► Return file
       │
       └── Node A FAILED?
               │
               ├── Node B ACTIVE? ──YES──► Double Un-XOR → Decompress → SHA-256 verify ──► Return file
               │
               └── Node B FAILED?
                       │
                       └── Node C ACTIVE? ──YES──► Triple Un-XOR → Decompress → Verify ──► Return file
                               │
                               └── ALL FAILED ──► RuntimeException: Recovery unavailable
```

### SeedBlock Algorithm

```
Registration:
  clientId  =  "550e8400-e29b-41d4-a716-446655440000"  (UUID)
  random    =  SecureRandom.nextBytes(32)               (256 bits)
  SeedBlock =  clientId_bytes[i] XOR random[i]          (per-byte XOR)

Node A encryption:
  xored     = xorMultiThreaded(gzip(file), SeedBlock)
  encrypted = AES-256-GCM(xored, SHA-256(SeedBlock))

Node B encryption:
  step1     = xorWithSeed(gzip(file), SeedBlock)
  step2     = xorWithSeed(step1, reversed(clientId_bytes))

Node C encryption:
  step1     = xorWithSeed(gzip(file), SeedBlock)
  step2     = xorWithSeed(step1, clientId_bytes)
  step3     = xorWithSeed(step2, reversed(SeedBlock))
```

### Multi-threaded XOR (Files > 1 MB)

```
File bytes split into 1 MB chunks
      │
      ├── Chunk 0 → Thread 1 → XOR with seed
      ├── Chunk 1 → Thread 2 → XOR with seed
      ├── Chunk 2 → Thread 3 → XOR with seed
      └── Chunk 3 → Thread 4 → XOR with seed
              │
              └── Reassemble chunks in order → encrypted file
```
Shared static thread pool (4 threads). Small files skip threading entirely for efficiency.

### Blockchain Audit (Proof-of-Work)

```
addBlock(clientId, action, data):
  1. Fetch previous block's hash (or GENESIS_000...)
  2. nonce = 0
  3. LOOP:
       hash = SHA-256(index + prevHash + data + nonce + timestamp)
       if hash starts with "00" → DONE (proof-of-work satisfied)
       nonce++
  4. Save block to DB
  5. Print full block details to terminal
```


---

## 📂 Folder Structure

```
sba_enhanced/
├── pom.xml                              # Maven dependencies & build config
├── run.bat                              # Windows startup script
├── SETUP_GUIDE.txt                      # Manual setup instructions
└── src/
    └── main/
        ├── java/com/sba/
        │   ├── SbaApplication.java      # Entry point — @SpringBootApplication
        │   ├── DataMigrationRunner.java # Startup hook — fixes NULL Boolean columns
        │   │
        │   ├── config/
        │   │   ├── JwtFilter.java       # JWT auth — reads Bearer token or ?token= param
        │   │   ├── SecurityConfig.java  # Spring Security — stateless, JWT filter chain
        │   │   ├── RateLimitFilter.java # 60 req/min rate limiter with IP eviction
        │   │   ├── WebConfig.java       # CORS configuration (configurable origins)
        │   │   └── SwaggerConfig.java   # OpenAPI/Swagger setup
        │   │
        │   ├── controller/
        │   │   ├── AuthController.java      # /api/auth/** — register, login, me
        │   │   ├── FileController.java      # /api/files/** — upload, download, recover, delete
        │   │   ├── NodeController.java      # /api/nodes/** — crash, restore, status
        │   │   ├── AdminController.java     # /api/admin/** — user management, stats
        │   │   ├── AnalyticsController.java # /api/analytics/** — blockchain, metrics, entropy
        │   │   ├── TeamController.java      # /api/team/** — organizations, sharing
        │   │   ├── LoadTestController.java  # /api/loadtest/** — stress testing
        │   │   └── ReportController.java    # /api/report/** — PDF generation
        │   │
        │   ├── service/
        │   │   ├── SBAService.java              # ⭐ CORE — upload, recover, failover, verify
        │   │   ├── BlockchainAuditService.java  # Hash-chained blocks with PoW + terminal logging
        │   │   ├── NodeStatusService.java       # In-memory node state (Active/Failed)
        │   │   ├── MetricsService.java          # Real-time performance tracking
        │   │   ├── CryptoAnalysisService.java   # Entropy, Merkle, ZKP, benchmarks
        │   │   ├── AuditService.java            # Operation logging to audit_logs
        │   │   └── LoadTestService.java         # Automated load testing (1KB–10MB)
        │   │
        │   ├── model/                   # 13 JPA entities
        │   │   ├── Client.java          # Users — clientId, username, BCrypt hash, SeedBlock
        │   │   ├── CloudFile.java       # File metadata — hash, size, compression info
        │   │   ├── BackupFile.java      # Node A — AES-256-GCM encrypted BLOB
        │   │   ├── SecondaryBackup.java # Node B — Double XOR encrypted BLOB
        │   │   ├── TertiaryBackup.java  # Node C — Triple XOR encrypted BLOB
        │   │   ├── FileNodeMapping.java # Node distribution tracking + deletion flags
        │   │   ├── AuditBlock.java      # Blockchain blocks — hash, prevHash, nonce
        │   │   ├── AuditLog.java        # Operation history — action, IP, timestamp
        │   │   ├── FileVersion.java     # Version history per file
        │   │   ├── Organization.java    # Team organizations
        │   │   ├── TeamMember.java      # Organization membership + roles
        │   │   ├── SharedFile.java      # Files shared with organizations
        │   │   └── LoadTestResult.java  # Stress test metrics
        │   │
        │   ├── repository/              # 11 Spring Data JPA repositories
        │   │
        │   └── util/
        │       ├── SBAUtils.java        # ⭐ All crypto — XOR, AES, SHA-256, GZIP, sanitize
        │       └── JwtUtil.java         # Token generation, validation, claim extraction
        │
        └── resources/
            ├── application.properties   # DB, JWT, CORS, multipart config
            └── static/
                ├── index.html           # Login/Register page
                ├── dashboard.html       # Full SPA dashboard (all sections)
                ├── admin.html           # Admin panel
                └── logo.png             # Application logo
```

---

## ⚙️ Installation & Setup

### Prerequisites

- **Java 17+** — [Download JDK 17](https://adoptium.net/)
- **Maven 3.8+** — [Download Maven](https://maven.apache.org/download.cgi)
- **MySQL 8.0+** — [Download MySQL](https://dev.mysql.com/downloads/)

### 1. Clone the Repository

```bash
git clone https://github.com/Kumar2029/ReSeedX-Engine.git
```

### 2. Configure Database

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.password=YOUR_MYSQL_PASSWORD    # ← Change this
```

> The database and all tables are created automatically on first startup — no SQL scripts needed.

### 3. Increase MySQL Packet Size (for large files)

```sql
SET GLOBAL max_allowed_packet = 268435456;
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Open your browser at: **http://localhost:8080**

### 5. First-Time Setup

1. Register a new account (password requires uppercase + number, min 8 chars)
2. Upload a file — watch it encrypt and distribute across 3 nodes
3. To get Admin access, run:
   ```sql
   UPDATE sba_db.clients SET role='ADMIN' WHERE username='YOUR_USERNAME';
   ```
4. Log out and back in — Admin panel and node controls appear

### Reset Database (if needed)

```sql
DROP DATABASE sba_db;
CREATE DATABASE sba_db;
```
Restart the server — Hibernate recreates all tables automatically.


---

## 🔑 Environment Variables

All configuration is in `src/main/resources/application.properties`:

| Property | Description | Default |
|---|---|---|
| `spring.datasource.password` | MySQL password | *(must set)* |
| `spring.datasource.url` | JDBC connection URL | `localhost:3306/sba_db` |
| `jwt.secret` | JWT signing key (min 32 chars) | *(built-in default)* |
| `jwt.expiry-ms` | Token expiration in milliseconds | `86400000` (24h) |
| `cors.allowed-origins` | Allowed CORS origins (comma-separated) | `http://localhost:8080` |
| `server.port` | HTTP server port | `8080` |
| `spring.servlet.multipart.max-file-size` | Max upload size | `500MB` |

> For production, externalize secrets using environment variables:
> ```bash
> export DB_PASSWORD=your_secure_password
> ```
> Then reference in properties: `spring.datasource.password=${DB_PASSWORD}`

---

## 🧪 Usage

### Uploading a File

1. Login to your account
2. Click **Upload** in the sidebar
3. Drag & drop or click to browse — the system will:
   - Check for duplicates (SHA-256)
   - Compress with GZIP (if beneficial)
   - Encrypt with AES-256-GCM + XOR → Node A
   - Apply Double XOR → Node B
   - Apply Triple XOR → Node C
   - Mine a blockchain audit block
4. The file appears in **My Drive** with all node actions available

### Recovering a File

In **My Drive**, each file has recovery buttons:
- **🔄** → Recover from Node A (AES decrypt → Un-XOR → Decompress)
- **⟲** → Recover from Node B (Double Un-XOR → Decompress)
- **☁** → Recover from Node C (Triple Un-XOR → Decompress)

If the requested node is crashed, the system automatically fails over to the next available node.

### Simulating Node Failure (Admin only)

1. Login as Admin
2. Navigate to **Storage Nodes**
3. Click **Crash** on any node card
4. The topology diagram updates in real-time (red lines, FAILED status)
5. Try recovering a file — it silently fails over to an active node
6. Click **Restore** to bring the node back

### Verifying Integrity

Click the **✓** button on any file — the system recovers the file, recomputes SHA-256, and compares with the stored hash. Mismatch = corruption detected.

### Team File Sharing

1. Go to **Team** → Create a new team
2. Add members by username
3. Share files with the team — members can download shared files

### API Quick Reference

```
# Auth
POST   /api/auth/register        {username, password}
POST   /api/auth/login           {username, password}
GET    /api/auth/me

# Files
POST   /api/files/upload         multipart/form-data
GET    /api/files/list?page=0&size=50
GET    /api/files/download/{id}
GET    /api/files/recover/{id}   Node A (auto-failover)
GET    /api/files/recover2/{id}  Node B (auto-failover)
GET    /api/files/recover3/{id}  Node C (auto-failover)
GET    /api/files/verify/{id}    SHA-256 integrity check
DELETE /api/files/{id}           Delete from all nodes
DELETE /api/files/{id}/node/{n}  Delete from specific node

# Nodes (Admin)
GET    /api/nodes/status
POST   /api/nodes/crash?node=A
POST   /api/nodes/restore?node=A

# Analytics
GET    /api/analytics/blockchain/status
GET    /api/analytics/blockchain/validate
GET    /api/analytics/entropy/{fileId}
GET    /api/analytics/merkle/{fileId}
GET    /api/analytics/benchmark?sizeKB=256
GET    /api/analytics/metrics

# Team
POST   /api/team/org             {name}
GET    /api/team/orgs
POST   /api/team/org/{id}/members  {username}
POST   /api/team/org/{id}/share/{fileId}

# Admin
GET    /api/admin/users
GET    /api/admin/stats
POST   /api/admin/create-admin   {username, password}

# Utilities
POST   /api/loadtest/run
GET    /api/report/generate      PDF download (?token= for browser)
```

Full interactive docs: **http://localhost:8080/swagger-ui.html**


---

## 🚧 Challenges & Learnings

### Challenge 1 — Thread Pool Leak Under Load
Initially, every file upload created a new `ExecutorService` with 4 threads for multi-threaded XOR. Under concurrent uploads, this leaked hundreds of threads that were never garbage collected. The fix was replacing per-call pools with a single shared static `ExecutorService` that all uploads reuse.

**Learning:** Never create thread pools inside request-scoped methods. Use application-scoped shared pools for predictable resource usage.

### Challenge 2 — MySQL BLOB Size Limits
Storing 5 copies of each file (original + 3 encrypted nodes + version history) as MySQL LONGBLOBs hit the `max_allowed_packet` limit (default 64MB). A 20MB file would require ~100MB of packet space, causing silent upload failures with no clear error message.

**Learning:** MySQL's `max_allowed_packet` must be tuned for BLOB-heavy workloads. The JDBC URL parameter alone isn't sufficient — the server-side `SET GLOBAL` is also required.

### Challenge 3 — Blockchain Race Conditions
Under concurrent uploads, two threads could simultaneously read the same "latest block" from the database, compute their proof-of-work against the same previous hash, and both attempt to save — creating a forked chain. Solved with `synchronized` on the `addBlock()` method.

**Learning:** Blockchain append operations are inherently sequential. Any concurrent access to the chain tip must be serialized to maintain integrity.

### Challenge 4 — XOR Symmetry for Recovery
Double and Triple XOR operations must be perfectly self-inverting. During development, a single byte offset in the reversed seed array caused the entire recovery to produce garbage data. The bug was invisible during encryption (output looked random either way) and only surfaced during decryption verification.

**Learning:** Always verify encryption round-trips with SHA-256 hash comparison immediately after implementing any new cipher mode. Never assume correctness from "looks encrypted."

### Challenge 5 — JWT Filter vs Static File Serving
The JWT authentication filter initially blocked access to static HTML/CSS/JS files, causing the login page itself to return 401. The fix required careful path exclusion patterns (`*.html`, `*.css`, `*.js`, `*.png`) while still enforcing auth on all `/api/**` endpoints.

**Learning:** Security filters must explicitly whitelist static resources. Spring Security's `permitAll()` alone isn't sufficient when a custom filter runs before the security chain.

### Challenge 6 — Frontend Pagination Breaking Change
Adding server-side pagination changed the `/api/files/list` response from a plain array to `{files: [...], page, size, totalFiles}`. This silently broke every frontend function that called `.length` or `.map()` on the response, with no JavaScript errors — just empty UI sections.

**Learning:** API response shape changes are breaking changes. Always check all consumers before modifying response structure, or use defensive parsing (`resp.files || resp`).

### Key Technical Learnings
- AES-256-GCM requires a unique 12-byte IV per encryption — reusing IVs completely breaks the security guarantee
- Shannon entropy is the gold standard for measuring encryption quality (ideal = 8.0 bits/byte)
- GZIP compression must happen **before** encryption — encrypted data has near-random byte distribution and is incompressible
- Blockchain proof-of-work difficulty must be balanced — too high and uploads become slow, too low and it's trivially forgeable
- `synchronized` in Java is per-instance — for singleton Spring beans this effectively provides global locking

---

## 🔮 Future Improvements

| Enhancement | Description |
|---|---|
| **Cloud Node Storage** | Replace MySQL BLOBs with AWS S3 / Azure Blob for true geographic separation |
| **WebSocket Real-Time** | Replace polling with WebSocket push for instant node state updates |
| **End-to-End Encryption** | Client-side encryption in browser — server never sees plaintext |
| **Docker Compose** | One-command deployment with MySQL + Spring Boot containers |
| **Redis Rate Limiting** | Replace in-memory rate limiter for horizontal scaling |
| **Mobile PWA** | Progressive Web App for mobile file management |
| **Shamir's Secret Sharing** | Split seed block into N shares, require K to recover (key backup) |
| **File Streaming** | Stream large files instead of loading entirely into memory |
| **Scheduled Backups** | Automatic folder sync with incremental versioning |
| **Encryption Upgrade** | Apply AES-256-GCM to all three nodes in production mode |

---

## 📋 Database Schema

| Table | Purpose |
|---|---|
| `clients` | User accounts — clientId (UUID PK), username, BCrypt password, role, SeedBlock BLOB |
| `cloud_files` | File metadata — fileId, fileName, SHA-256 hash, size, compression flag, timestamps |
| `backup_files` | Node A data — AES-256-GCM + Multi-thread XOR encrypted BLOB |
| `secondary_backups` | Node B data — Double XOR encrypted BLOB |
| `tertiary_backups` | Node C data — Triple XOR encrypted BLOB |
| `file_node_mapping` | Distribution tracking — nodeId, nodeLabel, filePath, deleted flag |
| `audit_blocks` | Blockchain — blockIndex, hash, previousHash, nonce, action, timestamp |
| `audit_logs` | Operation history — action, status, IP, duration for every API call |
| `file_versions` | Version history — versionNumber, hash, fileData, changeSummary |
| `organizations` | Teams — orgId, orgName, ownerId |
| `team_members` | Membership — orgId, clientId, role (OWNER/ADMIN/MEMBER) |
| `shared_files` | Shared files — fileId, orgId, sharedBy, timestamp |
| `load_test_results` | Stress test metrics — file sizes, XOR/AES timing, throughput |

> All tables are created automatically by Hibernate on first startup. No SQL scripts required.

---

## 🤝 Contributing

Contributions are welcome! Here's how to get started:

1. **Fork** the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes following existing code style
4. Test thoroughly — especially the encryption round-trip and failover logic
5. Commit with a clear message: `git commit -m "Add: description of what you added"`
6. Push to your fork: `git push origin feature/your-feature-name`
7. Open a **Pull Request** with a clear description of the change

### Code Style Guidelines
- Follow existing Spring Boot conventions (`@Service`, `@RestController`, `@Autowired`)
- All cryptographic operations belong in `SBAUtils.java`
- All business logic belongs in `SBAService.java`
- Every API endpoint should log to `AuditService`
- Frontend uses `api()` helper for authenticated requests

---

## 📜 License

```
MIT License

Copyright (c) 2026 Vasanth Kumar S

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 👨‍💻 Author

**Vasanth Kumar S**

---

Built with ☕ Java · 🔐 AES-256-GCM · ⛓️ Blockchain · ⚡ Spring Boot

*No matter which node fails — your data is always recoverable.*
