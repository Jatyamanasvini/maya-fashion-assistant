#!/usr/bin/env python3
"""
Crop a single garment from a photo and place it on a plain white background.

Usage:
    crop_item.py <input_image> <x1,y1,x2,y2> <output_png>

The bbox may be given as normalized fractions (0..1) or pixels; the script
auto-detects. GrabCut is seeded with the (padded) box to segment the item.
"""
import sys

import cv2
import numpy as np


def main() -> int:
    if len(sys.argv) != 4:
        print("usage: crop_item.py <input> <x1,y1,x2,y2> <output>", file=sys.stderr)
        return 2

    inp, bbox_raw, outp = sys.argv[1], sys.argv[2], sys.argv[3]
    img = cv2.imread(inp)
    if img is None:
        print(f"could not read image: {inp}", file=sys.stderr)
        return 3

    h, w = img.shape[:2]
    try:
        x1, y1, x2, y2 = (float(v) for v in bbox_raw.split(","))
    except ValueError:
        print(f"bad bbox: {bbox_raw}", file=sys.stderr)
        return 4

    # Auto-detect normalized (0..1) vs pixel coordinates
    if max(x1, y1, x2, y2) <= 1.5:
        px1, py1, px2, py2 = x1 * w, y1 * h, x2 * w, y2 * h
    else:
        px1, py1, px2, py2 = x1, y1, x2, y2

    if px2 < px1:
        px1, px2 = px2, px1
    if py2 < py1:
        py1, py2 = py2, py1

    # Pad the box so GrabCut has context around the garment
    pad_x = (px2 - px1) * 0.15 + 8
    pad_y = (py2 - py1) * 0.15 + 8
    rx1 = int(max(0, px1 - pad_x))
    ry1 = int(max(0, py1 - pad_y))
    rx2 = int(min(w, px2 + pad_x))
    ry2 = int(min(h, py2 + pad_y))
    if rx2 - rx1 < 12 or ry2 - ry1 < 12:
        rx1, ry1, rx2, ry2 = 0, 0, w, h

    mask = np.zeros(img.shape[:2], np.uint8)
    bgd = np.zeros((1, 65), np.float64)
    fgd = np.zeros((1, 65), np.float64)
    try:
        cv2.grabCut(img, mask, (rx1, ry1, rx2 - rx1, ry2 - ry1), bgd, fgd, 8, cv2.GC_INIT_WITH_RECT)
        fg = np.where((mask == cv2.GC_FGD) | (mask == cv2.GC_PR_FGD), 255, 0).astype("uint8")
    except Exception as exc:  # noqa: BLE001
        print(f"grabCut failed: {exc}", file=sys.stderr)
        fg = np.zeros(img.shape[:2], np.uint8)
        fg[ry1:ry2, rx1:rx2] = 255

    ys, xs = np.where(fg > 0)
    rect_area = max(1, (rx2 - rx1) * (ry2 - ry1))
    if len(xs) < 80 or len(xs) < 0.06 * rect_area:
        # Segmentation found too little (e.g. low-contrast item) -> use the padded box crop
        cx1, cy1, cx2, cy2 = rx1, ry1, rx2, ry2
        cut = img[cy1:cy2, cx1:cx2].copy()
    else:
        cx1, cy1, cx2, cy2 = int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1
        cut = img[cy1:cy2, cx1:cx2].copy()
        mcut = fg[cy1:cy2, cx1:cx2]
        white = np.full_like(cut, 255)
        m3 = (mcut > 0)[:, :, None]
        cut = (cut * m3 + white * (1 - m3)).astype("uint8")

    # Center the item on a square white canvas
    ch, cw = cut.shape[:2]
    side = int(max(ch, cw) * 1.22) + 2
    canvas = np.full((side, side, 3), 255, np.uint8)
    ox, oy = (side - cw) // 2, (side - ch) // 2
    canvas[oy:oy + ch, ox:ox + cw] = cut

    if not cv2.imwrite(outp, canvas):
        print("could not write output", file=sys.stderr)
        return 5
    return 0


if __name__ == "__main__":
    sys.exit(main())
