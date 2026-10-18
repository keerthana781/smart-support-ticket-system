# Setup guide: from zip file to GitHub

This guide takes you from the zip file to a running app and a GitHub repository with a green CI badge.
Plan for about 2–3 hours the first time, plus time to read the code.

---

## Step 1: Install the tools

| Tool | Why | Check it works |
|------|-----|----------------|
| [Java 21 (Temurin)](https://adoptium.net/) | Runs the backend | `java -version` |
| [Maven](https://maven.apache.org/download.cgi) | Builds the backend | `mvn -v` |
| [Node.js 20 LTS](https://nodejs.org/) | Builds the frontend | `node -v` |
| [Git](https://git-scm.com/downloads) | Version control | `git --version` |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Runs PostgreSQL and the full stack | `docker -v` |
| [IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/) or VS Code | Editor | |

On Windows, if `mvn` is "not recognized", Maven's `bin` folder is not on your PATH. Search
"Edit the system environment variables" → Environment Variables → Path → add the Maven `bin` folder.
IntelliJ can also run Maven for you without installing it separately.

---

## Step 2: Get a free Gemini API key (optional but recommended)

1. Go to https://aistudio.google.com/apikey and sign in with a Google account.
2. Click **Create API key** and copy it.
3. **Keep it private.** Never paste it into code or commit it. It goes only in a `.env` file or an
   environment variable.

Without a key, the app still works using offline keyword rules.

---

## Step 3: Unzip and run the app

Extract the zip (on Windows: right-click → Extract All), then open a terminal in the extracted folder:

```bash
cd smart-support-ticket-system
cp .env.example .env
```

Open `.env` in an editor and paste your key after `GEMINI_API_KEY=`.

### Easiest: everything in Docker

```bash
docker compose up --build
```

The first build downloads a lot and takes 5–10 minutes. When the logs settle, open
**http://localhost:3000** and click a demo account button on the login page.

### For development: run the parts separately

This is how you'll work day to day, because code changes show up instantly.

```bash
# Terminal 1: database only
docker compose up -d db

# Terminal 2: backend
cd backend
# macOS/Linux:
export GEMINI_API_KEY=your-key-here
# Windows PowerShell:
#   $env:GEMINI_API_KEY="your-key-here"
mvn spring-boot:run

# Terminal 3: frontend
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**.

---

## Step 4: Try it like a recruiter would

1. Log in as **customer@demo.com / Customer@123** → raise a ticket such as
   "Payment deducted twice, need refund urgently".
2. Log out, log in as **agent@demo.com / Agent@123** → open the ticket.
   See the **AI triage** panel → click **Use AI first reply** → edit → **Send reply**.
3. Add an **internal note** → log back in as the customer and confirm you can't see it.
4. As the agent, **Mark resolved** → as the customer, **Confirm it's fixed**.
5. Log in as **admin@demo.com / Admin@123** → open **Admin** → check the AI agreement rate.

If the AI panel says "Keyword rules" instead of "Gemini", the key isn't being picked up (see Troubleshooting).

---

## Step 5: Run the tests

```bash
cd backend
mvn test
```

You should see `BUILD SUCCESS`. These are the same tests GitHub Actions will run.

---

## Step 6: Read the code before pushing

Read **LEARNING.md** and follow its suggested reading order. Make sure you can explain:
- how a request goes from React → Spring Security → controller → service → database
- what happens when Gemini is down
- why customers get 404 (not 403) for other people's tickets

A good habit is to change something small yourself, for example add a `WAITING_ON_CUSTOMER`
status or a new category, and see what breaks. That turns it into your project.

---

## Step 7: Personalise it

Replace these placeholders:
- `README.md`: `YOUR_GITHUB_USERNAME`, `YOUR NAME`, `YOUR_LINKEDIN`
- `LICENSE`: `YOUR NAME`

Then take 2–3 screenshots (ticket list, ticket detail with the AI panel, admin page), put them in a
`docs/` folder, and add them to the README where the comment says so:
```markdown
![Ticket detail](docs/ticket-detail.png)
```

---

## Step 8: Push to GitHub

### 8a. Create the repository
On GitHub: **New repository** → name it `smart-support-ticket-system` → **Public** →
do **not** add a README, .gitignore or licence (the project already has them) → **Create**.

### 8b. Set up git (first time only)
```bash
git config --global user.name "Your Name"
git config --global user.email "the-email-on-your-github-account@example.com"
```

### 8c. Double-check no secrets are included
```bash
cd smart-support-ticket-system
git init
git status
```
`.env` must **not** appear in the list. (It's in `.gitignore`.) If it does appear, stop and fix that first.

### 8d. Commit in logical steps
Committing in steps that match how you read and understood the code gives a meaningful history.
Recruiters do look at commit messages.

```bash
git add .gitignore LICENSE backend/pom.xml backend/src/main/resources backend/src/main/java/com/supportdesk/SupportDeskApplication.java backend/src/main/java/com/supportdesk/common
git commit -m "Set up Spring Boot project with global error handling"

git add backend/src/main/java/com/supportdesk/user backend/src/main/java/com/supportdesk/security backend/src/main/java/com/supportdesk/auth backend/src/main/java/com/supportdesk/config
git commit -m "Add JWT authentication and role-based access control"

git add backend/src/main/java/com/supportdesk/ticket backend/src/main/java/com/supportdesk/admin
git commit -m "Add ticket workflow, comments, internal notes and admin stats"

git add backend/src/main/java/com/supportdesk/ai
git commit -m "Add AI triage and draft replies with Gemini and rule-based fallback"

git add backend/src/test
git commit -m "Add unit and integration tests"

git add frontend
git commit -m "Add React frontend"

git add backend/Dockerfile backend/.dockerignore docker-compose.yml .env.example .github
git commit -m "Add Docker setup and GitHub Actions CI"

git add README.md LEARNING.md SETUP_GUIDE.md   # also add "docs" if you put screenshots there
git commit -m "Add documentation"

git status   # should say "nothing to commit"
```

If `git status` still shows files, add them with a sensible message.

### 8e. Push
Copy the commands GitHub shows under "…or push an existing repository", which look like:
```bash
git branch -M main
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/smart-support-ticket-system.git
git push -u origin main
```
GitHub will ask you to sign in. If it asks for a password, use a
[Personal Access Token](https://github.com/settings/tokens) instead (GitHub no longer accepts account passwords for git).

### 8f. Check the CI pipeline
On GitHub, open the **Actions** tab. The "CI" workflow should go green in a few minutes, and the badge
in the README will turn green too. If a job fails, click it to see the log.

Also commit `frontend/package-lock.json` (created by your first `npm install`), so builds use exact versions:
```bash
git add frontend/package-lock.json
git commit -m "Lock frontend dependency versions"
git push
```

---

## Step 9: Optional extras

- **Deploy it** so recruiters can click a live link. Render and Railway both have free or cheap tiers
  for a Spring Boot app plus PostgreSQL. Set the environment variables (`DB_URL`, `GEMINI_API_KEY`,
  `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`) in their dashboard, never in code.
- **Pin the repo** on your GitHub profile.
- **Write a LinkedIn post** with a screenshot and what you learned (especially the AI fallback design).

---

## Resume bullets you can adapt

Only use these once you can explain every part:

- Built a full-stack help-desk application (Spring Boot, React, PostgreSQL) with JWT authentication and role-based access for customers, agents and admins
- Integrated Google Gemini to auto-classify ticket category and priority and draft replies, with JSON output validation, prompt-injection safeguards and a rule-based fallback
- Kept a human in the loop: agents review and edit AI drafts; tracked an AI agreement rate on an admin dashboard
- Containerised with Docker Compose and set up a GitHub Actions CI pipeline running unit and integration tests (JUnit 5, MockMvc)

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `Connection refused` to `localhost:5432` | The database isn't running: `docker compose up -d db` |
| Port 5432 or 8080 already in use | Another PostgreSQL/app is running. Stop it, or change the port in `docker-compose.yml` |
| AI panel says "Keyword rules" | Key not set for the backend. Check `.env` (Docker) or the `export` in the same terminal as `mvn spring-boot:run`. Restart the backend after setting it |
| Backend log: "Gemini triage failed … 404" | The model name was retired. Check current names in Google AI Studio and set `GEMINI_MODEL` |
| Backend log: "Gemini triage failed … 429" | Free-tier rate limit hit. Wait a minute; the app uses keyword rules meanwhile |
| Frontend shows "Cannot reach the server" | Backend isn't running on port 8080 |
| Changed demo data isn't showing | Demo data is only created when the database is empty. Reset: `docker compose down -v` (deletes all data) |
| Compile error after `mvn` | Make sure `java -version` shows 21. Paste the first error message to an AI assistant or search it |
| `git push` rejected | You probably created the GitHub repo with a README. Run `git pull origin main --allow-unrelated-histories`, then push again |
