package net.floatmc.arsenal;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.concurrent.ThreadLocalRandom;

public final class WeaponListener implements Listener {

    private final FloatArsenal plugin;
    private final Abilities abilities;

    public WeaponListener(FloatArsenal plugin, Abilities abilities) {
        this.plugin = plugin;
        this.abilities = abilities;
    }

    /** Habilidad activa: click derecho con el arma en la mano principal. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        WeaponType type = WeaponType.fromItem(plugin, e.getItem());
        if (type == null) {
            return;
        }
        Player p = e.getPlayer();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            if (block != null && !p.isSneaking()) {
                // No molestar al abrir cofres/puertas ni al pelar troncos con el hacha.
                boolean interactable = block.getType().isInteractable();
                boolean strippable = type.kind() == WeaponType.Kind.AXE && Tag.LOGS.isTagged(block.getType());
                if (interactable || strippable) {
                    return;
                }
            }
        }
        if (e.useItemInHand() == Event.Result.DENY || !p.hasPermission("floatarsenal.use")) {
            return;
        }
        abilities.activate(p, type);
    }

    /** Pasivas al golpear cuerpo a cuerpo. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (abilities.isAbilityDamage() || e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (!(e.getDamager() instanceof Player p)) {
            return;
        }
        Entity target = e.getEntity();
        if (!(target instanceof LivingEntity victim) || target instanceof ArmorStand) {
            return;
        }
        WeaponType type = WeaponType.fromItem(plugin, p.getInventory().getItemInMainHand());
        if (type == null) {
            return;
        }
        double minCharge = plugin.getConfig().getDouble("min-attack-charge", 0.85);
        if (p.getAttackCooldown() < minCharge) {
            return;
        }
        abilities.onHit(p, victim, type, e);
    }

    /** Pasiva de Pyrokar al matar. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        Entity k = e.getEntity().getKiller();
        if (!(k instanceof Player killer)) {
            return;
        }
        WeaponType type = WeaponType.fromItem(plugin, killer.getInventory().getItemInMainHand());
        if (type != null) {
            abilities.onKill(killer, type);
        }
    }

    /** Durabilidad efectiva mayor: parte de los usos no gasta durabilidad. */
    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        if (WeaponType.fromItem(plugin, e.getItem()) == null) {
            return;
        }
        double chance = plugin.getConfig().getDouble("durability-save-chance", 0.5);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            e.setCancelled(true);
        }
    }

    /**
     * Las armas legendarias no se pueden usar en la mesa de crafteo: evita convertir una en otra
     * (son de Netherite) y que la reparacion por crafteo les borre los atributos custom.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        for (ItemStack stack : e.getInventory().getMatrix()) {
            if (WeaponType.fromItem(plugin, stack) != null) {
                e.getInventory().setResult(null);
                return;
            }
        }

        if (e.getRecipe() instanceof ShapedRecipe recipe
                && "floatarsenal".equals(recipe.getKey().getNamespace())) {
            WeaponType type = WeaponType.byId(recipe.getKey().getKey());
            if (type != null && (!(e.getView().getPlayer() instanceof Player player)
                    || !player.hasPermission(type.recipePermission()))) {
                e.getInventory().setResult(null);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.discoverRecipesFor(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        abilities.clearCooldowns(e.getPlayer().getUniqueId());
    }
}
