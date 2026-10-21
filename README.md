# FloatArsenal — Armas legendarias para FloatMC

Plugin para **Paper 1.20.1** (Java 17). Agrega 4 armas de Netherite mejoradas (2 de fuego, 2 de hielo)
con habilidad **pasiva + activa** cada una, textura propia por `CustomModelData` y receta de crafteo muy cara.

## Armas

| ID | Base | CMD | Daño | Vel. ataque | Extras | Pasiva | Activa (click der.) |
|---|---|---|---|---|---|---|---|
| `ignarion` | Espada | 910001 | 10 | 1.8 | +5% velocidad | **Llama Voraz**: prende al golpear, +25% daño a enemigos en llamas | **Nova Ígnea** (12s): explosión de fuego radio 5, 8 dmg, incendia y empuja |
| `pyrokar` | Hacha | 910002 | 13 | 1.1 | +10% resist. empuje, +2 vida máx. | **Sed de Magma**: al matar, cura 3 ♥ y Fuerza I 8s | **Impacto Magmático** (15s): ola de lava de 10 bloques, 10 dmg, incendia |
| `glacius` | Espada | 910003 | 9.5 | 2.0 | +7% velocidad | **Toque Gélido**: cada golpe congela y da Lentitud II | **Zarpazo Ventisca** (14s): dash que daña (6) y onda de hielo al aterrizar |
| `cryonax` | Hacha | 910004 | 12 | 1.2 | +10% resist. empuje, +3 armadura | **Escarcha Pesada**: 20% de inmovilizar 2s | **Tormenta Polar** (18s): 5s de ventisca, Resistencia I, 3 dmg/s y lentitud en radio 6 |

Referencia Netherite: espada 8 / 1.6, hacha 10 / 1.0. Todas tienen además **durabilidad efectiva x2** (4062 usos vs 2031).
Las hachas **no** talan árboles (eso lo hace tu encantamiento aparte; son `NETHERITE_AXE`, así que son compatibles).

## Receta (crafteo en mesa)

```
N H N      N = Bloque de Netherite      H = Estrella del Nether
I S I      I = Magma (fuego) / Hielo azul (hielo)
  E        S = Espada/Hacha de Netherite
           E = Cabeza de Wither Esqueleto (espada fuego) / Corazón del Mar (espada hielo) / Tótem (hachas)
```
Las armas legendarias **no pueden usarse en la mesa de crafteo** (evita convertir una en otra o repararlas ahí y perder atributos).

## Comandos y permisos

- `/floatarsenal give <jugador> <ignarion|pyrokar|glacius|cryonax|all> [1-10]` — alias `/farsenal`, `/arsenal`
- `/floatarsenal list` · `/floatarsenal reload`
- `floatarsenal.admin` (op por defecto): comandos. `floatarsenal.use` (todos por defecto): usar habilidades activas.

## Compilar e instalar

```bash
mvn clean package          # requiere JDK 17 + Maven y acceso a repo.papermc.io
# jar resultante: target/FloatArsenal-1.0.0.jar  -> copiar a /plugins
```
Sin Maven local: sube la carpeta a GitHub; `.github/workflows/build.yml` compila el jar automáticamente (pestaña Actions → Artifacts).

## Resource pack

`resourcepack/` (o el zip entregado). En `server.properties`:
```
resource-pack=https://TU-URL/FloatArsenal-ResourcePack.zip
resource-pack-sha1=<sha1 del zip>
require-resource-pack=false
```
El pack **no reemplaza texturas vanilla**: solo agrega `predicate: custom_model_data` a
`models/item/netherite_sword.json` y `netherite_axe.json`. Si ya tienes un pack propio que modifica esos dos
archivos, **fusiona** los `overrides` en lugar de reemplazar el archivo.
Texturas regenerables con `python3 tools/gen_textures.py <carpeta_salida>` (requiere Pillow).

## Configuración

`config.yml`: cooldowns, daños, radios, duraciones, probabilidad de ahorrar durabilidad, recetas on/off.
Los valores de habilidades se leen en vivo (`/floatarsenal reload`). El lore de items ya entregados no cambia.
Las habilidades hacen daño con `entity.damage(x, jugador)`, por lo que WorldGuard y similares pueden cancelarlo.
