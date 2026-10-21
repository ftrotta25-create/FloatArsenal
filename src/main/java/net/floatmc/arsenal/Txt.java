package net.floatmc.arsenal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Utilidades de texto: convierte codigos legacy con '&' a Components de Adventure. */
public final class Txt {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Txt() {
    }

    /** Para nombre y lore de items: sin cursiva (Minecraft la pone por defecto). */
    public static Component item(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    /** Para mensajes de chat / action bar. */
    public static Component msg(String legacy) {
        return LEGACY.deserialize(legacy);
    }
}
