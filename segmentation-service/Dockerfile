# ============================================================
# MAYA segmentation microservice — garment cutouts on white.
#   build (from repo root):  docker build -f segmentation-service/Dockerfile -t maya-segmenter .
#   run:                     docker run -p 8080:8080 maya-segmenter
# ============================================================
FROM python:3.11-slim

WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY segmentation-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Reuse the exact same segmentation script as the backend
COPY backend/scripts/crop_item.py ./crop_item.py
COPY segmentation-service/app.py ./app.py

# Tiny U^2Net-p model (~4 MB) so the whole service stays well under 512 MB
RUN mkdir -p /app/models \
 && curl -L --fail -o /app/models/u2netp.onnx \
      https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx
ENV MAYA_SEG_MODEL=lite
ENV MAYA_MODEL_DIR=/app/models

EXPOSE 8080
CMD ["python", "app.py"]
