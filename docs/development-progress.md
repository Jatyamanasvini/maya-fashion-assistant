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
| **Phase 6** | Vision AI integration | **PARTIAL** | `VisionAiClient` abstraction + OpenAI (`/chat/completions`, JSON schema) + Mock clients. Not live-verified (no key). No Gemini client yet. |
| **Phase 7** | Chat & Conversation persistence | **COMPLETED** | `ConversationService`, `ChatController` (`/api/chat/conversations`), message persistence, ownership validation. Tested. |
| **Phase 8** | Intent extraction | **COMPLETED** | Deterministic `IntentExtractor` (occasion, style, formality, negative constraints, refinement signals). Tested. |
| **Phase 9** | Wardrobe-based recommendation engine | **COMPLETED** | `StylingService` orchestrates retrieval → AI ranking → deterministic fallback → **ownership validation** → outfit persistence. NEVER invents items. Tested. |
| **Phase 10** | Weather & context | **COMPLETED** | `WeatherService` + `WeatherClient` (Open-Meteo + Mock). Optional location; no coordinates persisted. Not live-verified. |
| **Phase 11** | Hairstyle recommendation | **COMPLETED** | Deterministic picker + AI-provided hairstyle; surfaced with each outfit. |
| **Phase 12** | Image generation | **PARTIAL** | `ImageGenerationClient` + OpenAI (`/images/generations`) + Mock. Prompt-based (DALL-E 3 has no reference-image support). Not live-verified. |
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
