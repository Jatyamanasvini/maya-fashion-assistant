# MAYA — AI Personal Fashion Assistant

**"Your wardrobe becomes something you can talk to."**

MAYA is a conversational personal stylist connected to your **own** digital wardrobe. Upload photos of your clothes, and MAYA identifies each item with multimodal AI, then styles complete outfits *only* from what you actually own — never inventing garments.

It is a **Java full-stack web application** (React + Spring Boot + MySQL), not a mobile app.

---

## Features

- **Wardrobe ingestion** — upload mirror selfies, outfit photos, or clothing snapshots; MAYA extracts individual items (top, bottom, dress, shoes, bag, accessories) with structured metadata.
- **Conversational styling** — ask naturally: *"Pick something cute for brunch with my girlies."*
- **Context awareness** — occasion, style, formality, and (optional) local weather are considered.
- **Refinement** — *"I don't want sneakers."* / *"Make it more feminine."* / *"Give me another option."*
- **Hairstyle recommendations** — a complementary hairstyle with every look.
- **Outfit visualization** — a generated image of the complete look (when an image-gen provider is configured).
- **Strict ownership** — the database is the source of truth; MAYA can never recommend an item you don't own.

## Core business rule

> **MAYA MUST NEVER RECOMMEND AN ITEM THE USER DOES NOT OWN.**

The AI may reason about fashion and propose wardrobe item IDs, but Spring Boot re-validates every ID against the authenticated user's wardrobe before committing an outfit. Invalid or foreign IDs are rejected.

## Architecture

```
React (Vite)  →  REST/JSON/multipart  →  Spring Boot (Java 21)
                                            ├── MySQL (source of truth)
                                            ├── AI APIs (vision, ranking, image gen)
                                            ├── Weather API (Open-Meteo)
                                            └── Protected local image storage
```

Spring Boot is the sole orchestrator. React never calls AI providers directly, and AI credentials are never exposed to the browser.

Key backend flow for a chat message:

```
user message → persist → intent extraction (deterministic)
  → retrieve the user's ACTUAL wardrobe
  → AI ranks candidates (or deterministic fallback)
  → validate every selected ID against DB + ownership + role
  → persist outfit → hairstyle → (optional) image generation → reply
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.4, Spring Data JPA, Spring Security, JJWT 0.12.5 |
| Database | MySQL (runtime), H2 (tests) |
| Frontend | React 18, Vite 5, React Router 6, Axios |
| AI | Provider-abstracted: **Ollama** (local, free, offline) or OpenAI (vision, ranking, image generation) |
| Weather | Open-Meteo (free, no key required) |
| Docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`) |

## Getting started

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- MySQL (optional for local dev; tests use in-memory H2)

> A pinned toolchain lives in `.tools/` (Maven 3.9.6, Node 20.18.0). Add it to your PATH:
> `export PATH="$PWD/.tools/bin:$PATH"`

### 1. Configure environment
```bash
cp .env.example .env     # then edit values
```
Export the variables (or rely on the defaults in `backend/src/main/resources/application.yml`).

### 2. Run the backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Run the frontend
```bash
cd frontend
npm install
npm run dev        # http://localhost:3000 (proxies /api → http://localhost:8080)
```

### 4. Run the tests
```bash
cd backend
mvn test           # 17 tests, H2-backed, no external services required
```

## Run with free local AI (Ollama)

MAYA can run real vision + outfit-ranking AI **locally, offline, and for free** using
[Ollama](https://ollama.com), which exposes an OpenAI-compatible API. No API key, no cost.

```bash
# 1. Install Ollama and start it (default: http://localhost:11434)
# 2. Pull a vision-capable model
ollama pull qwen2.5vl:3b

# 3. Run the backend with the bundled "ollama" profile
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=ollama
```

What it does: `AiConfig` recognises `AI_PROVIDER=OLLAMA` and routes the existing
OpenAI-compatible clients at your local server. One multimodal model
(`qwen2.5vl:3b`) handles both photo item-extraction and outfit ranking.

**Image generation under this profile:** Ollama cannot generate images, so the
`ollama` profile defaults image generation to **Pollinations.ai** — a free, keyless
text-to-image service. Set `IMAGE_GENERATION_PROVIDER=MOCK` to disable visualizations.

**Performance:** CPU inference is slower than the cloud — the frontend timeout is
already raised to 180 s.

Verify the integrations end-to-end (Ollama must be running with the model pulled):
```bash
MAYA_LIVE_OLLAMA=true   mvn -Dtest=LiveOllamaIntegrationTest test       # vision + ranking
MAYA_LIVE_IMAGEGEN=true mvn -Dtest=LiveImageGenerationTest test         # image generation
```

## Environment variables

See `.env.example` for the full list. The essentials:

| Variable | Purpose |
|---|---|
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | MySQL connection |
| `JWT_SECRET` / `JWT_EXPIRATION_MS` | JWT signing |
| `AI_PROVIDER` / `AI_API_KEY` / `AI_MODEL` / `AI_VISION_MODEL` | Vision + ranking AI |
| `IMAGE_GENERATION_PROVIDER` / `IMAGE_GENERATION_MODEL` / `IMAGE_GENERATION_API_KEY` | Outfit visualization |
| `WEATHER_PROVIDER` / `WEATHER_DEFAULT_LAT` / `WEATHER_DEFAULT_LON` | Weather context |
| `IMAGE_STORAGE_PATH` | Local image storage directory |

Without an `AI_API_KEY`, MAYA runs in **MOCK mode** — vision extraction returns sample items and outfit ranking falls back to a deterministic selector, so the full flow still works offline.

## API overview

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register, returns JWT |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/users/me` | Current user profile |
| POST | `/api/wardrobe/photos` | Upload photo → vision extraction → create items |
| GET/POST | `/api/wardrobe/items` | List / manually add wardrobe items |
| GET/PUT/DELETE | `/api/wardrobe/items/{id}` | Read / update / delete an owned item |
| GET | `/api/wardrobe/images/{id}` | Protected item image |
| GET/POST | `/api/chat/conversations` | List / create conversations |
| GET | `/api/chat/conversations/{id}` | Conversation + messages |
| POST | `/api/chat/conversations/{id}/messages` | Send a message, receive a styled recommendation |
| GET | `/api/outfits` | List the user's outfits |
| GET | `/api/outfits/{id}` | Outfit details |
| GET | `/api/outfits/{id}/image` | Protected generated visualization |

## Security

- JWT stateless auth; BCrypt password hashing.
- Per-user ownership checks on **every** user-owned resource (wardrobe items, images, conversations, outfits).
- Images are private; served only through authenticated, ownership-checked endpoints (no public URLs).
- Upload validation: MIME type, extension, 15 MB size limit, magic-byte image check, path-traversal protection.
- AI-provided wardrobe IDs are never trusted without backend validation.
- Passwords, JWTs, API keys, and image contents are never logged.

## Testing

`mvn test` (17 tests) covers:

- **Auth** — registration, login, duplicate email, invalid credentials.
- **Authorization** — User A cannot access/modify User B's wardrobe, conversation, or outfit.
- **Wardrobe** — upload + extraction + persistence, CRUD, ownership.
- **Recommendation** — foreign AI IDs rejected; only owned items returned; exclusion constraints honored.
- **Intent** — occasion, style, negative constraints, refinement signals.

## Known limitations

- **Verification status:**
  - ✅ **Live-verified (all free):** local AI via **Ollama** (`qwen2.5vl:3b`) for vision + outfit ranking, **weather** via Open-Meteo, and **image generation** via **Pollinations.ai** (keyless).
  - 🟡 **Implemented, not live-verified:** cloud **OpenAI** (the test account had no credit balance).
- Image generation is prompt-based; neither the Pollinations nor OpenAI provider accepts reference images, so a visualization is a stylistic interpretation rather than a pixel-accurate reproduction of the user's garments.
- No rate limiting on auth endpoints (acceptable for a learning/demo project).

## Documentation

- [`docs/architecture.md`](docs/architecture.md) — system design.
- [`docs/development-progress.md`](docs/development-progress.md) — phased progress and verification log.
