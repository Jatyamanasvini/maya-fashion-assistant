"""
MAYA segmentation microservice.

Wraps the crop_item.py script as a tiny HTTP API so the main app (which has no Python)
can request garment cutouts. Runs standalone so the ~340 MB Python/onnxruntime footprint
does not compete with the JVM for RAM.

Endpoints:
  GET  /health          -> {"status":"ok", ...}
  POST /segment         -> multipart: image=<file>, bbox="x1,y1,x2,y2"|-, category=TOP -> PNG
"""
import os
import subprocess
import sys
import tempfile

from flask import Flask, jsonify, request, send_file

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "crop_item.py")

app = Flask(__name__)


@app.get("/health")
def health():
    return jsonify(status="ok", model=os.environ.get("MAYA_SEG_MODEL", "full"))


@app.post("/segment")
def segment():
    upload = request.files.get("image")
    if upload is None:
        return jsonify(error="missing 'image' file"), 400

    bbox = request.form.get("bbox") or "-"
    category = (request.form.get("category") or "OTHER").upper()

    with tempfile.TemporaryDirectory() as work:
        inp = os.path.join(work, "in.jpg")
        out = os.path.join(work, "out.png")
        upload.save(inp)
        try:
            subprocess.run(
                [sys.executable, SCRIPT, inp, bbox, category, out],
                check=True, timeout=120,
                stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
            )
        except subprocess.TimeoutExpired:
            return jsonify(error="segmentation timed out"), 504
        except subprocess.CalledProcessError as exc:
            detail = (exc.stderr or b"").decode("utf-8", "ignore")[-300:]
            return jsonify(error="segmentation failed", detail=detail), 500

        if not os.path.exists(out):
            return jsonify(error="no output produced"), 500
        return send_file(out, mimetype="image/png", download_name="cutout.png")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
