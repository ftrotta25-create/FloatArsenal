package net.floatmc.arsenal;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Las 4 armas legendarias. Todas se identifican por un tag en el PersistentDataContainer
 * y usan un CustomModelData propio, asi nunca se sobreescriben las texturas vanilla.
 *
 * Referencia Netherite: espada 8 dano / 1.6 vel / 2031 dur; hacha 10 dano / 1.0 vel / 2031 dur.
 */
public enum WeaponType {

    //            id          material                  cmd     nombre
    IGNARION("ignarion", Material.NETHERITE_SWORD, 910001, "&6&lIgnarion &8» &cColmillo del Magma",
            Kind.SWORD, Element.FIRE,
            //  dano(attr) vel   movim  empuje vida armad  cd
            9.0, 1.8, 0.05, 0.00, 0.0, 0.0, 12,
            "Llama Voraz",
            "Cada golpe prende fuego al enemigo. Los que ya estaban en llamas reciben daño extra.",
            "Nova Ígnea",
            "Libera una explosión de fuego a tu alrededor que daña, incendia y lanza lejos a los enemigos."),

    PYROKAR("pyrokar", Material.NETHERITE_AXE, 910002, "&6&lPyrokar &8» &cDevoradora de Brasas",
            Kind.AXE, Element.FIRE,
            12.0, 1.1, 0.0, 0.10, 2.0, 0.0, 15,
            "Sed de Magma",
            "Al matar a un enemigo recuperas vida y ganas Fuerza por unos segundos.",
            "Impacto Magmático",
            "Lanza una ola de lava hacia adelante que daña e incendia todo lo que atraviesa."),

    GLACIUS("glacius", Material.NETHERITE_SWORD, 910003, "&b&lGlacius &8» &fFilo del Invierno Eterno",
            Kind.SWORD, Element.ICE,
            8.5, 2.0, 0.07, 0.00, 0.0, 0.0, 14,
            "Toque Gélido",
            "Cada golpe congela y ralentiza al enemigo.",
            "Zarpazo Ventisca",
            "Te lanzas hacia adelante dañando a quien atraviesas y al aterrizar congelas a todos los cercanos."),

    CRYONAX("cryonax", Material.NETHERITE_AXE, 910004, "&b&lCryonax &8» &fRompe-Glaciares",
            Kind.AXE, Element.ICE,
            11.0, 1.2, 0.0, 0.10, 0.0, 3.0, 18,
            "Escarcha Pesada",
            "Tus golpes tienen probabilidad de inmovilizar al enemigo en un bloque de hielo.",
            "Tormenta Polar",
            "Invocas una ventisca a tu alrededor: ganas Resistencia y los enemigos cercanos reciben daño y lentitud.");

    public enum Kind { SWORD, AXE }

    public enum Element {
        FIRE("&c", "Fuego"),
        ICE("&b", "Hielo");

        private final String color;
        private final String label;

        Element(String color, String label) {
            this.color = color;
            this.label = label;
        }
    }

    private static final Map<String, WeaponType> BY_ID = new HashMap<>();

    static {
        for (WeaponType t : values()) {
            BY_ID.put(t.id, t);
        }
    }

    private final String id;
    private final Material material;
    private final int customModelData;
    private final String displayName;
    private final Kind kind;
    private final Element element;
    private final double attackDamage;      // valor del atributo (el dano total es +1 de la mano)
    private final double attackSpeed;       // velocidad final (se resta la base 4.0 al aplicar)
    private final double moveBonus;         // fraccion: 0.05 = +5%
    private final double knockbackResist;   // fraccion: 0.10 = +10%
    private final double extraHealth;       // puntos de vida (2.0 = 1 corazon)
    private final double extraArmor;
    private final int defaultCooldown;
    private final String passiveName;
    private final String passiveDesc;
    private final String activeName;
    private final String activeDesc;

    WeaponType(String id, Material material, int customModelData, String displayName,
               Kind kind, Element element,
               double attackDamage, double attackSpeed, double moveBonus,
               double knockbackResist, double extraHealth, double extraArmor, int defaultCooldown,
               String passiveName, String passiveDesc, String activeName, String activeDesc) {
        this.id = id;
        this.material = material;
        this.customModelData = customModelData;
        this.displayName = displayName;
        this.kind = kind;
        this.element = element;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.moveBonus = moveBonus;
        this.knockbackResist = knockbackResist;
        this.extraHealth = extraHealth;
        this.extraArmor = extraArmor;
        this.defaultCooldown = defaultCooldown;
        this.passiveName = passiveName;
        this.passiveDesc = passiveDesc;
        this.activeName = activeName;
        this.activeDesc = activeDesc;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public int defaultCooldown() {
        return defaultCooldown;
    }

    public String activeName() {
        return activeName;
    }

    public static WeaponType byId(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    /** Devuelve el tipo de arma del item, o null si no es un arma de FloatArsenal. */
    public static WeaponType fromItem(FloatArsenal plugin, ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.weaponKey(), PersistentDataType.STRING);
        return id == null ? null : BY_ID.get(id);
    }

    /** Crea el item listo para entregar. */
    public ItemStack create(FloatArsenal plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Txt.item(displayName));
        meta.setCustomModelData(customModelData);
        meta.getPersistentDataContainer().set(plugin.weaponKey(), PersistentDataType.STRING, id);
        // Al definir modificadores propios se pierden los de vanilla, por eso se agregan dano y velocidad.
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        modifier(meta, Attribute.GENERIC_ATTACK_DAMAGE, "damage", attackDamage, AttributeModifier.Operation.ADD_NUMBER);
        modifier(meta, Attribute.GENERIC_ATTACK_SPEED, "speed", attackSpeed - 4.0, AttributeModifier.Operation.ADD_NUMBER);
        if (moveBonus != 0.0) {
            modifier(meta, Attribute.GENERIC_MOVEMENT_SPEED, "move", moveBonus, AttributeModifier.Operation.ADD_SCALAR);
        }
        if (knockbackResist != 0.0) {
            modifier(meta, Attribute.GENERIC_KNOCKBACK_RESISTANCE, "kbres", knockbackResist, AttributeModifier.Operation.ADD_NUMBER);
        }
        if (extraHealth != 0.0) {
            modifier(meta, Attribute.GENERIC_MAX_HEALTH, "health", extraHealth, AttributeModifier.Operation.ADD_NUMBER);
        }
        if (extraArmor != 0.0) {
            modifier(meta, Attribute.GENERIC_ARMOR, "armor", extraArmor, AttributeModifier.Operation.ADD_NUMBER);
        }

        meta.lore(buildLore(plugin));
        item.setItemMeta(meta);
        return item;
    }

    private void modifier(ItemMeta meta, Attribute attribute, String key, double amount, AttributeModifier.Operation op) {
        // UUID estable por arma+atributo: asi el item nunca acumula modificadores duplicados.
        UUID uuid = UUID.nameUUIDFromBytes(("floatarsenal:" + id + ":" + key).getBytes(StandardCharsets.UTF_8));
        meta.addAttributeModifier(attribute,
                new AttributeModifier(uuid, "floatarsenal." + key, amount, op, EquipmentSlot.HAND));
    }

    private List<Component> buildLore(FloatArsenal plugin) {
        List<Component> lore = new ArrayList<>();
        lore.add(Txt.item("&8Arma Legendaria &8• " + element.color + element.label));
        lore.add(Txt.item(""));
        lore.add(Txt.item("&7Daño de ataque: &a" + fmt(attackDamage + 1.0)));
        lore.add(Txt.item("&7Velocidad de ataque: &a" + fmt(attackSpeed)));
        if (moveBonus != 0.0) {
            lore.add(Txt.item("&7Velocidad de movimiento: &a+" + Math.round(moveBonus * 100) + "%"));
        }
        if (knockbackResist != 0.0) {
            lore.add(Txt.item("&7Resistencia al empuje: &a+" + Math.round(knockbackResist * 100) + "%"));
        }
        if (extraHealth != 0.0) {
            lore.add(Txt.item("&7Vida máxima: &a+" + fmt(extraHealth)));
        }
        if (extraArmor != 0.0) {
            lore.add(Txt.item("&7Armadura: &a+" + fmt(extraArmor)));
        }
        lore.add(Txt.item("&7Durabilidad: &aMuy superior a Netherite"));
        lore.add(Txt.item(""));
        lore.add(Txt.item("&6» Pasiva: &e" + passiveName));
        for (String line : wrap(passiveDesc, 40)) {
            lore.add(Txt.item("&7" + line));
        }
        lore.add(Txt.item(""));
        lore.add(Txt.item("&6» Activa &8(Click derecho)&6: &e" + activeName));
        for (String line : wrap(activeDesc, 40)) {
            lore.add(Txt.item("&7" + line));
        }
        int cd = (int) Math.round(plugin.getConfig().getDouble("weapons." + id + ".cooldown", defaultCooldown));
        lore.add(Txt.item("&8Enfriamiento: " + cd + "s"));
        return lore;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(word);
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Receta de crafteo (cara a proposito):
     *   N H N     N = Bloque de Netherite   H = Estrella del Nether
     *   I S I     I = Magma (fuego) / Hielo azul (hielo)   S = Espada/Hacha de Netherite
     *   _ E _     E = Cabeza de Wither Esqueleto / Corazon del Mar (espadas) / Totem (hachas)
     */
    public ShapedRecipe buildRecipe(FloatArsenal plugin, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, id), result);
        recipe.shape("NHN", "ISI", " E ");
        recipe.setIngredient('N', Material.NETHERITE_BLOCK);
        recipe.setIngredient('H', Material.NETHER_STAR);
        recipe.setIngredient('I', element == Element.FIRE ? Material.MAGMA_BLOCK : Material.BLUE_ICE);
        recipe.setIngredient('S', kind == Kind.SWORD ? Material.NETHERITE_SWORD : Material.NETHERITE_AXE);
        Material bottom;
        if (kind == Kind.AXE) {
            bottom = Material.TOTEM_OF_UNDYING;
        } else {
            bottom = element == Element.FIRE ? Material.WITHER_SKELETON_SKULL : Material.HEART_OF_THE_SEA;
        }
        recipe.setIngredient('E', bottom);
        return recipe;
    }
}
