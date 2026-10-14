# ============================================================
# MAYA — single-container production image
#   build:   docker build -t maya .
#   run:     docker run -p 8080:8080 -e AI_API_KEY=<gemini-key> maya
# ============================================================

# ---------- 1. Build the React frontend ----------
FROM node:20-slim AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm ci --silent
COPY frontend/ ./
RUN npm run build

# ---------- 2. Build the Spring Boot backend (frontend baked into /static) ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /fe/dist ./src/main/resources/static
RUN mvn -q -B -DskipTests package

# ---------- 3. Runtime: JRE + Python/OpenCV + segmentation models ----------
FROM eclipse-temurin:21-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-venv curl \
 && rm -rf /var/lib/apt/lists/*
RUN python3 -m venv /opt/venv \
 && /opt/venv/bin/pip install --no-cache-dir --upgrade pip \
 && /opt/venv/bin/pip install --no-cache-dir onnxruntime pillow numpy
ENV PATH="/opt/venv/bin:$PATH"

WORKDIR /app
COPY --from=backend /build/target/*.jar app.jar
COPY backend/scripts ./scripts

# U^2Net-p model for (optional) garment isolation — tiny (~4 MB) so it fits small hosts.
# Full-quality models live in .tools/models for local development.
RUN mkdir -p /app/models \
 && curl -L --fail -o /app/models/u2netp.onnx \
      https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx
ENV MAYA_MODEL_DIR=/app/models

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
