# Development Progress Log: MAYA – AI Personal Fashion Assistant

## Overview
This document tracks the phased development, verifications, test status, known limitations, and configuration requirements for **MAYA – AI Personal Fashion Assistant**.

---

## Phase Tracking

| Phase | Description | Status | Verification / Notes |
|---|---|---|---|
| **Phase 0** | Repository inspection & environment validation | **COMPLETED** | Java 21 LTS, Maven 3.9.6, Node v20.18.0, npm 10.8.2 (pinned in `.tools/`). |
| **Phase 1** | Spring Boot 3.x project foundation | **COMPLETED** | Spring Boot 3.3.4, Java 21, monolith layout, compiled. |
| **Phase 2** | MySQL entities & Spring Data JPA repositories | **COMPLETED** | 7 entities (User, WardrobeSource, WardrobeItem, Conversation, ChatMessage, Outfit, OutfitItem) + 7 repositories with ownership filtering. |
| **Phase 3** | Authentication, JWT, Security | **COMPLETED** | BCrypt, JJWT 0.12.5, CustomUserDetailsService, `/api/auth/register`, `/api/auth/login`, `/api/users/me`. Integration + unit tested. |
| **Phase 4** | React application foundation + Auth UI | **COMPLETED** | React 18, React Router v6, Axios interceptor, AuthContext, protected routes. Verified production build. |
| **Phase 5** | Wardrobe CRUD & secure image storage | **COMPLETED** | `ImageStorageService` + `LocalImageStorageService` (path-traversal, MIME, magic-byte validation), `WardrobeController`, protected image endpoints. Ownership enforced. Tested. |
| **Phase 6** | Vision AI integration | **PARTIAL** | `VisionAiClient` abstraction + OpenAI + Mock clients, plus a local **Ollama** provider. **Live-verified** via local `qwen2.5vl:3b` (see log). Cloud OpenAI not live-verified (no credits). |
| **Phase 7** | Chat & Conversation persistence | **COMPLETED** | `ConversationService`, `ChatController` (`/api/chat/conversations`), message persistence, ownership validation. Tested. |
| **Phase 8** | Intent extraction | **COMPLETED** | Deterministic `IntentExtractor` (occasion, style, formality, negative constraints, refinement signals). Tested. |
| **Phase 9** | Wardrobe-based recommendation engine | **COMPLETED** | `StylingService` orchestrates retrieval → AI ranking → deterministic fallback → **ownership validation** → outfit persistence. NEVER invents items. Tested; AI ranking **live-verified** via local Ollama. |
| **Phase 10** | Weather & context | **COMPLETED** | `WeatherService` + `WeatherClient` (Open-Meteo + Mock). Optional location; no coordinates persisted. **Live-verified** against the free Open-Meteo API (no key). |
| **Phase 11** | Hairstyle recommendation | **COMPLETED** | Deterministic picker + AI-provided hairstyle; surfaced with each outfit. |
| **Phase 12** | Image generation | **COMPLETED** | `ImageGenerationClient` + **Pollinations** (free, keyless) + OpenAI (`/images/generations`, handles `url` & `b64_json`) + Mock. Prompt-based (no reference images). **Live-verified** via Pollinations (real JPEG generated). |
| **Phase 13** | Conversational refinement | **COMPLETED** | "I don't want sneakers", "make it more feminine", "give me another option" handled; constraints applied to the current outfit. Tested. |
| **Phase 14** | Security hardening & error handling | **PARTIAL** | `@RestControllerAdvice`; image paths no longer exposed; client `imagePath` ignored; ownership checks throughout. Rate limiting not yet added. |
| **Phase 15** | Testing, Documentation, Polish | **PARTIAL** | 17 tests passing. README + architecture docs present. |

---

## Log Entries

### [Phase 0] Toolchain and Environment Setup
- Verified Java 21 LTS, Maven 3.9.6, Node v20.18.0, npm 10.8.2 (pinned in `.tools/`).

### [Phases 1–6] Foundation (Antigravity)
- Spring Boot monolith, JPA entities/repositories, JWT auth, React SPA, wardrobe CRUD, secure local image storage, and Vision AI abstraction (OpenAI + Mock).

### [Phases 7–13] Conversational Styling Pipeline (OpenCode continuation)
- Added: `ConversationService`, `ChatController`, `OutfitController`, `OutfitService`, `StylingService`, `IntentExtractor`, `WeatherService`, `WeatherClient` (Open-Meteo + Mock), `ChatAiClient` (OpenAI + Mock), `ImageGenerationClient` (OpenAI + Mock).
- Enforced the core rule: **MAYA only recommends items the authenticated user owns.** AI-provided IDs are re-validated against MySQL; invalid/foreign IDs force a deterministic fallback.
- Verified: `mvn test` → 17/17 green; `npm run build` → success.

### [Phase 14] Security hardening
- Removed internal `imagePath` from API responses (replaced with `hasImage` flag on wardrobe items; removed from outfit items).
- Ignored client-supplied `imagePath` on wardrobe create/update (image paths are vision-ingestion-only).
- Added `ChatSecurityIntegrationTest` verifying cross-user conversation/outfit isolation (404).

### [Live verification attempt] External providers
- **Key validity:** confirmed (`GET /v1/models` → HTTP 200).
- **Model availability:** `gpt-4o-mini`, `gpt-4o` present; **`dall-e-3` is NOT available** on the tested account — only the newer `gpt-image-*` family (`gpt-image-1`, `gpt-image-1-mini`, `gpt-image-2`, ...).
- **Inference:** **BLOCKED** — all completion/image calls returned `insufficient_quota` ("You have no credits remaining"). Therefore vision, chat ranking, and image generation remain **IMPLEMENTED but NOT LIVE-VERIFIED**.
- **Fix applied as a result of the probe:** `OpenAiImageGenerationClient` now handles both response shapes — inline `b64_json` (newer `gpt-image-*` models) and hosted `url` (DALL-E). Default model in `.env.example` changed to `gpt-image-1`.
- **Weather — LIVE-VERIFIED (free, keyless):** a live `GET https://api.open-meteo.com/v1/forecast` returned the exact shape the client parses (`current.temperature_2m`, `current.weather_code`, `current.wind_speed_10m`, `current.precipitation`). No credentials or cost involved.

### [Phases 6 & 9] Local free AI via Ollama — LIVE-VERIFIED
Goal: run real vision + outfit-ranking AI with **no API key and no cost**.
- Installed Ollama locally without `sudo` (Homebrew was owned by another user; used the official `Ollama-darwin.zip` in the gitignored `.tools/`), started the server on `localhost:11434`, and pulled the multimodal model `qwen2.5vl:3b` (~3.2 GB).
- Added an `OLLAMA` provider to `AiConfig` (reusing the existing OpenAI-compatible clients — Ollama speaks that protocol) and an `application-ollama.yml` profile.
- Raised the frontend request timeout to 180s (CPU inference is slower than cloud).
- Added `LiveOllamaIntegrationTest` (opt-in via `MAYA_LIVE_OLLAMA=true`).
- **Result:** `LiveOllamaIntegrationTest` → **2/2 passed**. MAYA's real `OpenAiVisionClient` extracted an item from a synthetic image, and its real `OpenAiChatClient` returned a schema-correct outfit that **respected an "avoid sneakers" constraint** (chose loafers, skipped sneakers).
- Image generation remains unavailable locally (Ollama does not generate images) and not live-verified in the cloud (no credits).

### [Phase 12] Free image generation via Pollinations — LIVE-VERIFIED
- Ollama cannot generate images, so added a dedicated `POLLINATIONS` image-generation provider: a free, **keyless** text-to-image endpoint (`https://image.pollinations.ai/prompt/...`).
- Implemented `PollinationsImageGenerationClient` (no new dependencies; uses Spring's `RestClient`).
- Improved the visualization prompt to describe the **actual selected garments** (color + description), not just roles.
- Added `LiveImageGenerationTest` (opt-in via `MAYA_LIVE_IMAGEGEN=true`).
- **Result:** generated a real 47,973-byte JPEG in ~22 s. The `ollama` profile now defaults image generation to Pollinations, so the whole pipeline (local AI + free image gen) runs at **$0**.

---

## How to run

### Backend
```bash
export PATH="$PWD/.tools/bin:$PATH"
cd backend
mvn spring-boot:run     # requires MySQL configured (see .env.example / application.yml)
mvn test                # H2-backed tests, no MySQL required
```

### Frontend
```bash
export PATH="$PWD/.tools/bin:$PATH"
cd frontend
npm install
npm run dev             # dev server on :3000, proxies /api -> :8080
npm run build           # production build into dist/
```

## Known limitations
- AI (Vision, chat ranking, image generation) and Weather integrations are **implemented but not live-verified** (no provider credentials available).
- Image generation is prompt-based only; OpenAI DALL-E 3 does not accept reference images, so it cannot reproduce the user's exact garments.
- No rate limiting on auth endpoints yet (dev-acceptable).
- `.env.example` mentions a `GEMINI` vision provider that is not yet implemented.
