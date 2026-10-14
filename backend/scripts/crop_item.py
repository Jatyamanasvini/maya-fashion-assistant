#!/usr/bin/env python3
"""
Isolate a single garment from a photo and place it on a plain white background.

Strategy:
  * TOP / OUTERWEAR / BOTTOM / DRESS  -> U^2Net-Cloth-Seg (segments clothing by
    category / class, so no bounding box is required).
  * SHOES / BAG / ACCESSORY / JEWELRY / OTHER -> U^2Net salient-object segmentation
    applied to the item's bounding-box crop.
  * If a model is unavailable or a mask is unreliable -> plain padded crop.

The result is trimmed, composited on white, upscaled if small, and sharpened.

Usage:
    crop_item.py <input_image> <x1,y1,x2,y2|-> <CATEGORY> <output_png>
"""
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

_MEAN = (0.485, 0.456, 0.406)
_STD = (0.229, 0.224, 0.225)
_CLASS_BY_CATEGORY = {"TOP": 1, "OUTERWEAR": 1, "BOTTOM": 2, "DRESS": 3}


def find_model(filename: str) -> str | None:
    env = os.environ.get("MAYA_MODEL_DIR")
    candidates = [
        os.path.join(env, filename) if env else None,
        os.path.join(os.getcwd(), "models", filename),
        os.path.join(os.getcwd(), "..", ".tools", "models", filename),
        os.path.expanduser(os.path.join("~/.u2net", filename)),
        os.path.expanduser(os.path.join("~/.rembg/models", filename.replace(".onnx", ""), filename)),
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return path
    return None


def load_session(filename: str):
    path = find_model(filename)
    if not path:
        return None
    try:
        import onnxruntime as ort

        return ort.InferenceSession(path, providers=["CPUExecutionProvider"])
    except Exception as exc:  # noqa: BLE001
        print(f"failed to load {filename}: {exc}", file=sys.stderr)
        return None


def _tensor(img: Image.Image, size):
    im = img.convert("RGB").resize(size, Image.Resampling.LANCZOS)
    arr = np.array(im).astype(np.float64)
    arr = arr / max(np.max(arr), 1e-6)
    tmp = np.zeros_like(arr)
    for c in range(3):
        tmp[:, :, c] = (arr[:, :, c] - _MEAN[c]) / _STD[c]
    return np.expand_dims(tmp.transpose((2, 0, 1)), 0).astype(np.float32)


def cloth_class_alpha(session, img: Image.Image, cls: int) -> Image.Image:
    outs = session.run(None, {session.get_inputs()[0].name: _tensor(img, (768, 768))})
    pred = np.argmax(outs[0], axis=1, keepdims=True)
    labels = np.squeeze(pred).astype("uint8")
    mask = Image.fromarray(labels, mode="L").resize(img.size, Image.Resampling.NEAREST)
    m = np.array(mask)
    return Image.fromarray(((m == cls).astype("uint8") * 255), mode="L")


def salient_alpha(session, img: Image.Image) -> Image.Image:
    outs = session.run(None, {session.get_inputs()[0].name: _tensor(img, (320, 320))})
    pred = outs[0][:, 0, :, :]
    ma, mi = float(np.max(pred)), float(np.min(pred))
    pred = (pred - mi) / max(ma - mi, 1e-6)
    pred = np.squeeze(pred)
    mask = Image.fromarray((np.clip(pred, 0, 1) * 255).astype("uint8"), mode="L")
    return mask.resize(img.size, Image.Resampling.LANCZOS)


def composite_on_white(img: Image.Image, alpha: Image.Image) -> Image.Image:
    a = np.array(alpha).astype(np.float32) / 255.0
    a = np.clip((a - 0.12) / 0.72, 0.0, 1.0)
    if float((a > 0.5).mean()) < 0.015:
        return img  # nothing meaningful -> keep as-is
    arr = np.array(img).astype(np.float32)
    white = np.full_like(arr, 255.0)
    comp = (arr * a[:, :, None] + white * (1 - a[:, :, None])).astype("uint8")
    ys, xs = np.where(a > 0.35)
    if len(xs) > 0:
        return Image.fromarray(comp[ys.min():ys.max() + 1, xs.min():xs.max() + 1])
    return Image.fromarray(comp)


def sharpen(img: Image.Image) -> Image.Image:
    return img.filter(ImageFilter.UnsharpMask(radius=1.8, percent=140, threshold=3))


def parse_bbox(bbox_raw, w, h):
    """Parse 'x1,y1,x2,y2' into pixel coords, handling the three conventions
    VLMs use: 0..1 fractions, 0..1000 normalized (Gemini/Qwen), or absolute pixels."""
    try:
        x1, y1, x2, y2 = (float(v) for v in bbox_raw.split(","))
    except (ValueError, AttributeError):
        return None
    mx = max(x1, y1, x2, y2)
    if mx <= 1.5:
        px1, py1, px2, py2 = x1 * w, y1 * h, x2 * w, y2 * h
    elif mx <= 1000 and (mx > w or mx > h):
        px1, py1, px2, py2 = x1 * w / 1000.0, y1 * h / 1000.0, x2 * w / 1000.0, y2 * h / 1000.0
    else:
        px1, py1, px2, py2 = x1, y1, x2, y2
    if px2 < px1:
        px1, px2 = px2, px1
    if py2 < py1:
        py1, py2 = py2, py1
    return px1, py1, px2, py2


def main() -> int:
    if len(sys.argv) != 5:
        print("usage: crop_item.py <input> <x1,y1,x2,y2|-> <CATEGORY> <output>", file=sys.stderr)
        return 2

    inp, bbox_raw, category, outp = sys.argv[1], sys.argv[2], sys.argv[3].upper(), sys.argv[4]
    try:
        base = Image.open(inp).convert("RGB")
    except Exception as exc:  # noqa: BLE001
        print(f"could not read image: {exc}", file=sys.stderr)
        return 3

    w, h = base.size
    cls = _CLASS_BY_CATEGORY.get(category)
    result = None

    # 1. Category segmentation (tops / bottoms / dresses) — no bbox required
    if cls is not None:
        session = load_session("u2net_cloth_seg.onnx")
        if session is not None:
            alpha = cloth_class_alpha(session, base, cls)
            if float((np.array(alpha) > 127).mean()) > 0.01:
                result = composite_on_white(base, alpha)

    # 2. Salient segmentation on the bounding-box crop (shoes / bags / accessories)
    if result is None and bbox_raw not in ("-", "", "none"):
        try:
            box = parse_bbox(bbox_raw, w, h)
            if box is None:
                raise ValueError("bad bbox")
            px1, py1, px2, py2 = box
            pad_x = (px2 - px1) * 0.12 + 6
            pad_y = (py2 - py1) * 0.12 + 6
            rx1, ry1 = int(max(0, px1 - pad_x)), int(max(0, py1 - pad_y))
            rx2, ry2 = int(min(w, px2 + pad_x)), int(min(h, py2 + pad_y))
            if rx2 - rx1 >= 16 and ry2 - ry1 >= 16:
                crop = base.crop((rx1, ry1, rx2, ry2))
                session = load_session("u2net.onnx")
                if session is not None:
                    alpha = salient_alpha(session, crop)
                    a = np.array(alpha).astype(np.float32) / 255.0
                    cov = float(((a - 0.12) / 0.72 > 0.5).mean())
                    if 0.03 < cov < 0.97:
                        result = composite_on_white(crop, alpha)
                    else:
                        result = crop
                else:
                    result = crop
        except ValueError:
            pass

    # 3. Fallback: plain crop of the bbox, else the whole image
    if result is None:
        box = parse_bbox(bbox_raw, w, h) if bbox_raw not in ("-", "", "none") else None
        if box is None:
            result = base
        else:
            x1, y1, x2, y2 = box
            result = base.crop((int(max(0, min(x1, x2))), int(max(0, min(y1, y2))),
                                int(min(w, max(x1, x2))), int(min(h, max(y1, y2)))))

    if result.width < 16 or result.height < 16:
        result = base

    # Upscale small items, sharpen, center on a white square
    cw, ch = result.size
    if max(cw, ch) < 420:
        scale = 420 / max(cw, ch)
        result = result.resize((max(1, int(cw * scale)), max(1, int(ch * scale))), Image.Resampling.LANCZOS)
    result = sharpen(result)

    cw, ch = result.size
    side = int(max(cw, ch) * 1.18) + 2
    canvas = Image.new("RGB", (side, side), (255, 255, 255))
    canvas.paste(result, ((side - cw) // 2, (side - ch) // 2))
    canvas.save(outp, "PNG")
    return 0


if __name__ == "__main__":
    sys.exit(main())
