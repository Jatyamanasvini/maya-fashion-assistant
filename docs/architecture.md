# MAYA – AI Personal Fashion Assistant Architecture

## 1. System Overview
Maya is a learning-focused full-stack web application designed to help users interactively style their outfits using their actual, uploaded wardrobe items.

```
+---------------------------------------------------------------+
|                      React Web Client                         |
|   (Auth, Wardrobe Gallery, Photo Upload, Chat, Outfits)       |
+------------------------------+--------------------------------+
                               | REST (JSON / Multipart)
                               v
+---------------------------------------------------------------+
|                   Spring Boot Monolith                        |
|                                                               |
|  [Security / JWT]  [Wardrobe Controller]  [Chat Controller]   |
|         |                   |                     |           |
|         v                   v                     v           |
|  [Auth Service]    [Wardrobe Service]    [Chat Service]       |
|                             |                     |           |
|                     +-------+-------+             v           |
|                     |               |      [Recommendation]   |
|                     v               v             |           |
|             [Vision Service]  [Storage]    [Hairstyle Service]|
|                     |               |             |           |
|                     v               v             v           |
|             [VisionAiClient] [ImageStorage] [ImageGenClient]  |
+---------------------+---------------+-------------+-----------+
                      |               |             |
           +----------+               v             +--------+
           |                    [File Storage]               |
           v                                                 v
    [MySQL Database]                                  [External AI]
    - Users                                           - Vision AI
    - Wardrobe Sources                                - Chat / Intent
    - Wardrobe Items                                  - Image Gen
    - Conversations & Messages                        [Weather API]
    - Outfits & Outfit Items                          - Open-Meteo
```

## 2. Key Architecture Principles
1. **Source of Truth Rule**: MAYA MUST NOT INVENT WARDROBE ITEMS. Wardrobe recommendations are strictly grounded in items owned by the authenticated user in MySQL.
2. **Deterministic & Semantic Separation**:
   - Java filters candidates by ownership, category combinations (e.g. TOP + BOTTOM or DRESS), and user-excluded items.
   - Multimodal AI performs semantic reasoning, ranking, occasion suitability, and hairstyle matching.
   - Java re-validates that every returned item ID actually exists in the user's database before committing the outfit.
3. **Protected Image Storage**: Uploaded user photos and generated outfit visualizations are stored in private local file storage with path-traversal protection and served only via authenticated, ownership-checked endpoints (`/api/wardrobe/images/{id}` or `/api/outfits/{id}/image`).
4. **Resilient AI & Weather Abstractions**:
   - If AI or Weather services are unavailable, the application gracefully reports errors or falls back to sensible defaults rather than crashing.
5. **Clean DTO & Entity Separation**:
   - No JPA entities are exposed directly to API controllers.
