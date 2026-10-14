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

# ---------- 3. Runtime: JRE (+ OPTIONAL Python for garment cutouts) ----------
FROM eclipse-temurin:21-jre

# Garment cutouts need Python + ~340 MB RAM at runtime, which OOM-kills Render's free
# 512 MB tier. They are therefore OFF by default — the image ships JVM-only so uploads
# can never crash the app. Enable on a bigger instance with:
#     docker build --build-arg INSTALL_SEGMENTATION=true .
ARG INSTALL_SEGMENTATION=false
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && if [ "$INSTALL_SEGMENTATION" = "true" ]; then \
      apt-get update \
      && apt-get install -y --no-install-recommends python3 python3-venv \
      && rm -rf /var/lib/apt/lists/* \
      && python3 -m venv /opt/venv \
      && /opt/venv/bin/pip install --no-cache-dir --upgrade pip \
      && /opt/venv/bin/pip install --no-cache-dir onnxruntime pillow numpy \
      && mkdir -p /app/models \
      && curl -L --fail -o /app/models/u2netp.onnx \
           https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx ; \
    fi
ENV PATH="/opt/venv/bin:$PATH"
ENV MAYA_MODEL_DIR=/app/models

WORKDIR /app
COPY --from=backend /build/target/*.jar app.jar
COPY backend/scripts ./scripts

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
