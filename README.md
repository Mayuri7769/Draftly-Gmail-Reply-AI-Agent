# Draftly Gmail Reply AI Agent

Draftly is an AI-powered Gmail assistant that fetches unread emails, generates draft replies in multiple tones, allows user review/edit, and sends approved replies while tracking status history.

## Submission Deliverables

- GitHub Repository Link: https://github.com/Mayuri7769/Draftly-Gmail-Reply-AI-Agent
- Dockerized Solution: included via `docker-compose.yml`, `Backend/Dockerfile`, `frontend/Dockerfile`

## Demo Video
https://drive.google.com/file/d/1tribi0jPwKph0IVlcmYkx4bWR8YTCtov/view?usp=drive_link

## Implemented Features

- Google OAuth2 login for Gmail access
- Fetch unread Gmail messages and extract metadata
  - sender, subject, snippet/body, thread id, received timestamp
- AI draft generation endpoint with tone support
  - formal, friendly, concise
  - external AI call with safe fallback draft generation
- Draft review workflow
  - edit draft text
  - approve and send
  - reject/discard draft
- Secure send flow
  - send replies via Gmail API
  - preserve thread id and reply headers
- Monitoring and resilience
  - status tracking in database (`PENDING`, `DRAFT_GENERATED`, `EDITED`, `APPROVED`, `SENT`, `REJECTED`, `FAILED`)
  - retry logic on send
  - clearer backend/frontend error messaging
- Session management
  - logout endpoint
  - Google token revocation request on logout

## Tech Stack

- Backend: Spring Boot, Spring Security OAuth2 Client, Spring Data JPA, MySQL
- Gmail Integration: Google Gmail API
- AI Integration: Gemini REST API
- Frontend: React + Axios

## Architecture Overview

Frontend (React) communicates with the Spring Boot backend through REST APIs.

Backend responsibilities:
- Gmail OAuth2 authentication
- Gmail API integration
- AI draft generation
- Draft workflow management
- MySQL persistence

MySQL stores:
- Email metadata
- Draft content
- Workflow status history

External integrations:
- Google Gmail API
- Gemini AI API

## Project Structure

- `Backend/` - Spring Boot backend
- `frontend/` - React frontend

## API Endpoints

Base URL: `http://localhost:8080/api`

- `GET /emails/unread`  
  Fetch unread emails from Gmail and store mapped drafts.

- `POST /ai/generate?id={draftId}&tone={formal|friendly|concise}`  
  Generate AI draft body for the selected email.

- `POST /drafts/approve-and-send?id={draftId}&editedBody={text}`  
  Approve and send draft (uses edited body if provided).

- `PUT /drafts/{id}`  
  Edit and save draft body.

- `POST /drafts/reject?id={draftId}`  
  Mark draft as rejected.

- `GET /history?status={STATUS}`  
  View draft history.  
  `status` optional (`ALL`, `SENT`, `FAILED`, `REJECTED`, etc.).

- `POST /auth/logout`  
  Logout and request Google token revocation.

## Local Setup

## 1) Backend setup

Update `Backend/src/main/resources/application.properties`:

- `server.port=8080`
- MySQL credentials
- `ai.api.key=<YOUR_GEMINI_API_KEY>`
- Google OAuth client id/secret and redirect URI

Ensure in Google Cloud:

- Gmail API is enabled
- OAuth consent screen is configured
- Authorized redirect URI includes:  
  `http://localhost:8080/login/oauth2/code/google`

Run backend:

```bash
cd Backend
./mvnw spring-boot:run
```

(On Windows PowerShell, use `.\mvnw.cmd spring-boot:run`)

## 2) Frontend setup

```bash
cd frontend
npm install
npm start
```

Open: `http://localhost:3000`

## Docker Setup (Detailed)

Prerequisites:

- Docker Desktop installed and running
- Gmail API enabled for your Google Cloud project
- OAuth consent and credentials configured
- Redirect URI in Google Cloud set to:
  `http://localhost:8080/login/oauth2/code/google`

### 1) Create environment file

At project root:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Update `.env` with real values:

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_DATABASE`
- `AI_API_KEY`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### 2) Build and run all services

```bash
docker compose up --build
```

This starts:

- MySQL on `localhost:3306`
- Spring Boot backend on `localhost:8080`
- React frontend (Nginx-served build) on `localhost:3000`

### 3) Access the app

- Open `http://localhost:3000`
- Login with Google
- Click `Sync Inbox` and continue normal flow

### 4) Stop services

```bash
docker compose down
```

To also remove DB volume:

```bash
docker compose down -v
```

## Demo Flow

1. Login with Google
2. Click `Sync Inbox`
3. Select an email
4. Generate `Formal` / `Friendly` / `Concise` draft
5. Edit if needed
6. Click `Approve & Send` or `Discard`
7. Verify updates in `Draft History`

## Demo Video Script (Under 5 Minutes)

1. Introduce problem + project goal (20-30 sec)
2. Show architecture quickly (`frontend`, `backend`, MySQL, Gmail API, Gemini) (30 sec)
3. Run app (local or docker) and show login (30 sec)
4. Sync inbox and select an email (30 sec)
5. Generate `Formal`, `Friendly`, `Concise` drafts and show difference (60 sec)
6. Edit one draft, approve & send (45 sec)
7. Show reject flow and history status filter (45 sec)
8. Show logout and close with key features (30 sec)

## Known Limitations

- User style inference is currently basic (not advanced NLP profiling).
- Encryption service exists, but persistent encrypted token storage can be strengthened further.
- UI can be further improved (toasts, richer status chips, pagination/search).

## Submission Checklist Mapping

- OAuth2 Gmail integration: done
- Email ingestion + metadata extraction: done
- AI-driven draft generation with tones: done
- Review/edit/approve/reject workflow: done
- Secure send with thread continuity: done
- Logging/monitoring/failure status + retries: done

## How To Add GitHub Repository Link

1. Create repo on GitHub
2. From project root run:

```bash
git init
git add .
git commit -m "Initial commit: Draftly Gmail Reply AI Agent"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```

3. Copy your GitHub repo URL and paste it under "Submission Deliverables" in this README.

