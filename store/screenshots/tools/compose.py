from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, sys

RAW = "../raw"; OUT = ".."  # run from this tools/ directory
W, H = 1080, 1920
BG_TOP, BG_BOT = (30, 31, 34), (49, 51, 56)
ACCENT = (212, 133, 64)
HN = "/System/Library/Fonts/HelveticaNeue.ttc"
title_font = ImageFont.truetype(HN, 64, index=1)
sub_font = ImageFont.truetype(HN, 36, index=0)

SHOTS = [
  ("01_board",       "Find the mystery word",   "Six tries. Green is right, gold is close."),
  ("02_won",         "Score big for fast solves","Fewer guesses earn more points."),
  ("03_hint",        "Stuck? Take a hint",       "Reveal a letter or rule some out."),
  ("04_six_letters", "Pick your challenge",      "Play with 4, 5 or 6 letter words."),
  ("05_history",     "Every game, remembered",   "Review your guesses and results."),
  ("06_definition",  "Learn what words mean",    "Tap any word for its definition."),
  ("07_bookmarks",   "Save words for later",     "Bookmark favorites to revisit."),
  ("08_settings",    "Track your stats",         "Scores, averages and preferences."),
]

def gradient():
    img = Image.new("RGB", (W, H))
    px = img.load()
    for y in range(H):
        t = y / (H - 1)
        c = tuple(int(BG_TOP[i] + (BG_BOT[i] - BG_TOP[i]) * t) for i in range(3))
        for x in range(W): px[x, y] = c
    return img

def rounded_mask(size, r):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0]-1, size[1]-1], r, fill=255)
    return m

def compose(name, title, sub):
    shot = Image.open(f"{RAW}/{name}.png").convert("RGB")
    canvas = gradient()
    d = ImageDraw.Draw(canvas)

    # caption block
    tw = d.textlength(title, font=title_font); d.text(((W-tw)/2, 96), title, font=title_font, fill=(242, 243, 245))
    sw = d.textlength(sub, font=sub_font);     d.text(((W-sw)/2, 184), sub, font=sub_font, fill=(163, 169, 178))
    d.rounded_rectangle([W/2-30, 256, W/2+30, 262], 3, fill=ACCENT)

    # device: screen scaled to 720 wide, bezel around it, shadow beneath
    sw_, sh_ = 720, 1600
    screen = shot.resize((sw_, sh_), Image.LANCZOS)
    bezel = 16; r_screen = 56; r_bezel = r_screen + bezel
    bx, by = (W - sw_)//2 - bezel, 300 - bezel
    bw, bh = sw_ + 2*bezel, sh_ + 2*bezel

    shadow = Image.new("RGBA", (W, H), (0,0,0,0))
    ImageDraw.Draw(shadow).rounded_rectangle([bx, by+28, bx+bw, by+bh+28], r_bezel, fill=(0,0,0,150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(34))
    canvas = Image.alpha_composite(canvas.convert("RGBA"), shadow)

    body = Image.new("RGBA", (bw, bh), (0,0,0,0))
    ImageDraw.Draw(body).rounded_rectangle([0,0,bw-1,bh-1], r_bezel, fill=(16,17,19,255), outline=(70,73,80,255), width=2)
    canvas.alpha_composite(body, (bx, by))
    canvas.paste(screen, (bx+bezel, by+bezel), rounded_mask((sw_, sh_), r_screen))

    out = canvas.convert("RGB")
    out.save(f"{OUT}/{name}.png", optimize=True)
    print("wrote", name, out.size)

for s in SHOTS: compose(*s)
