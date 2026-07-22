# AI Recruiter Portal

An AI-powered personal portfolio website that lets recruiters interactively explore your background, skills, projects, and **chat with an AI assistant** that knows everything about you.

## Quick Start (Development)

### Prerequisites
- **Java 21** (or 17+)
- **Maven 3.9+**
- **Node.js 20+** and npm
- **Angular CLI**: `npm install -g @angular/cli`

### 1. Start the Backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts at `http://localhost:8080` with an H2 in-memory database.
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:recruiterdb`)
- API: http://localhost:8080/api/profile

### 2. Start the Frontend

```bash
cd frontend
npm install
ng serve
```

The frontend starts at `http://localhost:4200`.

### 3. Open in Browser

Go to **http://localhost:4200** — you should see the full portal with seeded demo data.

## AI Chat Configuration

The app runs in **demo mode** by default (no API key needed) with keyword-matched responses. To enable real AI:

### Option A: OpenAI
```bash
export AI_PROVIDER=openai
export AI_API_KEY=sk-your-openai-key
export AI_MODEL=gpt-4o-mini
```

### Option B: Anthropic (Claude)
```bash
export AI_PROVIDER=anthropic
export AI_API_KEY=sk-ant-your-key
```

### Option C: Ollama (Local, Free)
```bash
# Install Ollama: https://ollama.com
ollama pull llama3.2
export AI_PROVIDER=ollama
export AI_MODEL=llama3.2
```

## Customize Your Data

Edit `backend/src/.../config/DataSeeder.java` — replace all placeholder data with your real:
- Name, title, summary, contact info
- Education history
- Work experience with highlights
- Projects with GitHub links

## Production Deployment

### Docker Compose (recommended)
```bash
cp .env.example .env
# Edit .env with your values
docker compose up -d
```

### Manual
```bash
# Backend
cd backend && mvn package -DskipTests
java -jar target/recruiter-portal-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# Frontend
cd frontend && ng build --configuration=production
# Serve dist/ with nginx
```

## Tech Stack

| Layer    | Technology                    |
|----------|-------------------------------|
| Frontend | Angular 17, TypeScript, SCSS  |
| Backend  | Java 21, Spring Boot 3.3      |
| Database | H2 (dev) / PostgreSQL (prod)  |
| AI       | OpenAI / Anthropic / Ollama   |
| Deploy   | Docker, Nginx, GitHub Actions |

## Project Structure

```
recruiter-portal/
├── backend/                    # Spring Boot API
│   ├── src/main/java/.../
│   │   ├── config/             # CORS, data seeder
│   │   ├── controller/         # REST endpoints
│   │   ├── model/              # JPA entities
│   │   ├── repository/         # Spring Data repos
│   │   ├── service/            # AI chat, GitHub
│   │   └── dto/                # Request/response DTOs
│   ├── src/main/resources/
│   │   └── application.yml     # Config
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                   # Angular SPA
│   ├── src/app/
│   │   ├── core/               # Services, models
│   │   ├── features/           # Page components
│   │   │   ├── home/
│   │   │   ├── about/
│   │   │   ├── education/
│   │   │   ├── work-history/
│   │   │   ├── projects/
│   │   │   └── ai-chat/        # ⭐ The star feature
│   │   └── shared/             # Navbar, footer
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml
├── .env.example
└── README.md
```
