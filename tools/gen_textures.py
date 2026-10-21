from PIL import Image
import os, sys

def hx(h):
    h = h.lstrip('#'); return tuple(int(h[i:i+2], 16) for i in (0, 2, 4)) + (255,)

def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), hx(c))

def filled(img):
    px = img.load()
    return {(x, y) for x in range(16) for y in range(16) if px[x, y][3] > 0}

def outline(img, col):
    f = filled(img)
    for (x, y) in f:
        for dx, dy in ((1,0),(-1,0),(0,1),(0,-1)):
            n = (x+dx, y+dy)
            if 0 <= n[0] < 16 and 0 <= n[1] < 16 and n not in f:
                put(img, n[0], n[1], col)

PAL = {
 'fire': dict(out='#1c0707', dark='#6b1208', mid='#d1380f', light='#ff8a1f', hi='#ffe27a',
              guard='#2a1d24', guard2='#4b3340', gold='#e8a825', gem='#fff08a',
              h1='#8a5532', h2='#552f1c', pom='#e8a825', ember='#ffb347', ember2='#ff5a14'),
 'ice':  dict(out='#08202f', dark='#1f6a9c', mid='#5ec8f2', light='#b9efff', hi='#ffffff',
              guard='#8fb1c4', guard2='#5f8196', gold='#e4f6ff', gem='#7af2ff',
              h1='#46618a', h2='#2a3a5c', pom='#e4f6ff', ember='#d9f7ff', ember2='#8fe3ff'),
}

def sword(p):
    img = new()
    for y in range(16):
        for x in range(16):
            u, v = x+y, x-y
            # blade
            if abs(u-15) <= 1 and -3 <= v <= (13 if u == 15 else 12):
                c = {14: p['light'], 15: p['mid'], 16: p['dark']}[u]
                if u == 15 and v == 13: c = p['hi']
                if u == 15 and v in (9, 5, 1): c = p['hi']   # fuller sparkle
                put(img, x, y, c)
            # guard
            elif v in (-4, -5) and 11 <= u <= 19:
                put(img, x, y, p['gold'] if 13 <= u <= 17 else p['guard2'])
            # handle
            elif u in (15, 16) and -12 <= v <= -6:
                put(img, x, y, p['h1'] if u == 15 else p['h2'])
            # pommel
            elif u in (14, 15, 16) and v in (-14, -13):
                put(img, x, y, p['pom'])
    put(img, 5, 10, p['gem'])   # gem at guard centre
    put(img, 4, 9, p['guard2']); put(img, 6, 11, p['guard2'])
    return img

def axe(p):
    img = new()
    # handle
    for y in range(16):
        for x in range(16):
            u, v = x+y, x-y
            if u in (15, 16) and -13 <= v <= 7:
                put(img, x, y, p['h1'] if u == 15 else p['h2'])
    # head rows: y -> (x0, x1)
    rows = {1:(9,13), 2:(7,14), 3:(6,14), 4:(5,13), 5:(5,12), 6:(6,11), 7:(7,10)}
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1+1):
            if x == x0: c = p['hi']
            elif x == x0+1: c = p['light']
            elif x >= x1-1: c = p['dark']
            else: c = p['mid']
            put(img, x, y, c)
    return img

def deco_ignarion(img, p):
    # flame tongues on the lower edge + core glow
    for (x, y, c) in [(9,8,'ember'),(10,7,'ember2'),(7,6,'ember'),(12,5,'ember2'),(5,8,'ember')]:
        put(img, x, y, p[c])
    put(img, 13, 2, p['hi']); put(img, 12, 3, p['hi'])
    put(img, 11, 4, p['light'])

def deco_pyrokar(img, p):
    # jagged lava edge, glowing crack, magma gem
    for (x, y) in [(4,3),(4,5),(5,2),(6,7)]:
        put(img, x, y, p['ember2'])
    for (x, y) in [(8,3),(9,4),(10,5)]:
        put(img, x, y, p['hi'])        # glowing crack
    put(img, 6, 4, p['gem'])
    for (x, y) in [(3,4),(5,6)]:
        put(img, x, y, p['ember'])
    # gold ring on handle
    put(img, 8, 7, p['gold']); put(img, 7, 8, p['gold']) if False else None

def deco_glacius(img, p):
    # crystal spikes along the upper edge + sparkle
    for (x, y) in [(9,3),(11,1),(7,5)]:
        put(img, x, y, p['hi'])
    for (x, y) in [(10,3),(8,5),(12,1)]:
        put(img, x, y, p['light'])
    put(img, 14, 3 if False else 2, p['hi'])

def deco_cryonax(img, p):
    # icicle spikes on the edge and a frozen core
    for (x, y) in [(4,4),(5,3),(4,2),(5,6),(6,8)]:
        put(img, x, y, p['light'])
    for (x, y) in [(3,4),(4,1)]:
        put(img, x, y, p['hi'])
    for (x, y) in [(8,3),(8,4),(9,3)]:
        put(img, x, y, p['hi'])
    put(img, 7, 4, p['gem'])

def build(outdir):
    os.makedirs(outdir, exist_ok=True)
    out = {}
    s = sword(PAL['fire']); deco_ignarion(s, PAL['fire']); outline(s, PAL['fire']['out']); out['ignarion'] = s
    a = axe(PAL['fire']); deco_pyrokar(a, PAL['fire']); outline(a, PAL['fire']['out']); out['pyrokar'] = a
    s = sword(PAL['ice']); deco_glacius(s, PAL['ice']); outline(s, PAL['ice']['out']); out['glacius'] = s
    a = axe(PAL['ice']); deco_cryonax(a, PAL['ice']); outline(a, PAL['ice']['out']); out['cryonax'] = a
    for k, im in out.items():
        im.save(os.path.join(outdir, k + '.png'))
    return out

if __name__ == '__main__':
    o = build(sys.argv[1])
    sheet = Image.new('RGBA', (16*4*12+50, 16*12+20), (60, 60, 70, 255))
    for i, k in enumerate(['ignarion','pyrokar','glacius','cryonax']):
        sheet.paste(o[k].resize((192, 192), Image.NEAREST), (10 + i*205, 10), o[k].resize((192,192), Image.NEAREST))
    sheet.save('/home/claude/work/preview.png')
