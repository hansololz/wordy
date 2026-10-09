"""Play Store feature graphic (1024x500). Run from this tools/ directory."""
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1024, 500
HN = "/System/Library/Fonts/HelveticaNeue.ttc"
ICON = "../../../app/src/main/ic_launcher-playstore.png"
OUT = "../../feature_graphic.png"

BG_L, BG_R = (30, 31, 34), (49, 51, 56)
GREEN, GOLD, ABSENT = (59, 165, 92), (201, 130, 9), (69, 72, 80)
TEXT, MUTED = (242, 243, 245), (163, 169, 178)

img = Image.new("RGB", (W, H))
px = img.load()
for x in range(W):
    t = x / (W - 1)
    c = tuple(int(BG_L[i] + (BG_R[i] - BG_L[i]) * t) for i in range(3))
    for y in range(H): px[x, y] = c

# soft orange glow behind the icon
glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse([40, 40, 420, 460], fill=(242, 90, 12, 70))
glow = glow.filter(ImageFilter.GaussianBlur(70))
img = Image.alpha_composite(img.convert("RGBA"), glow)

# icon with rounded corners and shadow
icon = Image.open(ICON).convert("RGBA").resize((250, 250), Image.LANCZOS)
mask = Image.new("L", icon.size, 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, 249, 249], 56, fill=255)
shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(shadow).rounded_rectangle([108, 140, 358, 390], 56, fill=(0, 0, 0, 160))
shadow = shadow.filter(ImageFilter.GaussianBlur(22))
img = Image.alpha_composite(img, shadow)
img.paste(icon, (108, 125), mask)

d = ImageDraw.Draw(img)
title = ImageFont.truetype(HN, 96, index=1)
tag = ImageFont.truetype(HN, 34, index=0)
d.text((430, 118), "Wordy", font=title, fill=TEXT)
d.text((434, 232), "Guess the mystery word in six tries.", font=tag, fill=MUTED)

# tile row spelling the name, with the game's three tile colours
tile_font = ImageFont.truetype(HN, 44, index=1)
x, y, size, gap = 434, 300, 72, 10
for letter, colour in zip("WORDY", [GREEN, GOLD, GREEN, ABSENT, GREEN]):
    d.rounded_rectangle([x, y, x + size, y + size], 14, fill=colour)
    tw = d.textlength(letter, font=tile_font)
    d.text((x + (size - tw) / 2, y + 10), letter, font=tile_font, fill=TEXT)
    x += size + gap

img.convert("RGB").save(OUT, optimize=True)
print("wrote", OUT)
