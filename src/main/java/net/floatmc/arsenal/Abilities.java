package net.floatmc.arsenal;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Habilidades activas (click derecho) y pasivas (al golpear / matar) de cada arma. */
public final class Abilities {

    private static final Particle.DustOptions ICE_DUST = new Particle.DustOptions(Color.fromRGB(150, 230, 255), 1.3f);

    private final FloatArsenal plugin;
    private final Map<UUID, Map<WeaponType, Long>> cooldowns = new HashMap<>();

    /** true mientras una habilidad inflige dano: evita que las pasivas se disparen en cadena. */
    private boolean abilityDamage = false;

    public Abilities(FloatArsenal plugin) {
        this.plugin = plugin;
    }

    public boolean isAbilityDamage() {
        return abilityDamage;
    }

    public void clearCooldowns(UUID player) {
        cooldowns.remove(player);
    }

    // ------------------------------------------------------------------ config

    private double dbl(WeaponType t, String key, double def) {
        return plugin.getConfig().getDouble("weapons." + t.id() + "." + key, def);
    }

    /** Lee segundos de la config y los devuelve en ticks. */
    private int ticks(WeaponType t, String key, double defSeconds) {
        return (int) Math.round(dbl(t, key, defSeconds) * 20.0);
    }

    // --------------------------------------------------------------- activacion

    public void activate(Player p, WeaponType t) {
        long now = System.currentTimeMillis();
        Map<WeaponType, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new EnumMap<>(WeaponType.class));
        Long until = map.get(t);
        if (until != null && until > now) {
            double left = (until - now) / 1000.0;
            p.sendActionBar(Txt.msg(String.format(Locale.ROOT, "&c%s &7en enfriamiento: &e%.1fs", t.activeName(), left)));
            return;
        }
        long cooldownMs = (long) (dbl(t, "cooldown", t.defaultCooldown()) * 1000.0);
        map.put(t, now + cooldownMs);

        switch (t) {
            case IGNARION -> ignarionNova(p);
            case PYROKAR -> pyrokarWave(p);
            case GLACIUS -> glaciusDash(p);
            case CRYONAX -> cryonaxStorm(p);
        }
        p.sendActionBar(Txt.msg("&6» &e" + t.activeName() + " &6«"));
    }

    // ------------------------------------------------------------------ pasivas

    public void onHit(Player p, LivingEntity victim, WeaponType t, EntityDamageByEntityEvent event) {
        World w = victim.getWorld();
        Location spot = victim.getLocation().add(0, victim.getHeight() / 2.0, 0);
        switch (t) {
            case IGNARION -> {
                if (victim.getFireTicks() > 0) {
                    event.setDamage(event.getDamage() * (1.0 + dbl(t, "burning-bonus", 0.25)));
                }
                victim.setFireTicks(Math.max(victim.getFireTicks(), ticks(t, "passive-fire-seconds", 5)));
                w.spawnParticle(Particle.FLAME, spot, 12, 0.3, 0.4, 0.3, 0.03);
            }
            case GLACIUS -> {
                frost(victim, ticks(t, "passive-slow-seconds", 3), 1);
                w.spawnParticle(Particle.SNOWFLAKE, spot, 14, 0.3, 0.4, 0.3, 0.03);
                w.playSound(spot, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.2f);
            }
            case CRYONAX -> {
                if (Math.random() < dbl(t, "root-chance", 0.20)) {
                    int dur = ticks(t, "root-seconds", 2);
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, dur, 6));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, dur, 128));
                    victim.setFreezeTicks(victim.getMaxFreezeTicks());
                    w.spawnParticle(Particle.REDSTONE, spot, 25, 0.4, 0.6, 0.4, 0.0, ICE_DUST);
                    w.spawnParticle(Particle.SNOWFLAKE, spot, 20, 0.4, 0.6, 0.4, 0.05);
                    w.playSound(spot, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.7f);
                }
            }
            default -> {
                // PYROKAR: su pasiva se dispara al matar (onKill)
            }
        }
    }

    public void onKill(Player killer, WeaponType t) {
        if (t != WeaponType.PYROKAR || killer.isDead()) {
            return;
        }
        double heal = dbl(t, "kill-heal", 6.0);
        AttributeInstance maxAttr = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxAttr == null ? 20.0 : maxAttr.getValue();
        killer.setHealth(Math.min(max, killer.getHealth() + heal));
        killer.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ticks(t, "kill-strength-seconds", 8), 0));
        Location l = killer.getLocation().add(0, 1, 0);
        killer.getWorld().spawnParticle(Particle.FLAME, l, 25, 0.4, 0.6, 0.4, 0.05);
        killer.getWorld().spawnParticle(Particle.HEART, l, 3, 0.4, 0.5, 0.4, 0.0);
        killer.getWorld().playSound(l, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7f, 1.6f);
    }

    // ---------------------------------------------------------------- IGNARION

    private void ignarionNova(Player p) {
        WeaponType t = WeaponType.IGNARION;
        double radius = dbl(t, "radius", 5.0);
        double damage = dbl(t, "damage", 8.0);
        int fire = ticks(t, "fire-seconds", 6);

        World w = p.getWorld();
        Location center = p.getLocation().add(0, 1, 0);
        w.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.4f, 0.6f);
        w.playSound(center, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.7f);
        w.spawnParticle(Particle.EXPLOSION_LARGE, center, 2, 0.3, 0.3, 0.3, 0.0);
        animateRing(w, center, radius, 6, Particle.FLAME, Particle.LAVA);

        for (LivingEntity e : targets(p, center, radius)) {
            hurt(p, e, damage);
            e.setFireTicks(fire);
            Vector push = e.getLocation().toVector().subtract(p.getLocation().toVector());
            push.setY(0);
            if (push.lengthSquared() > 0.0001) {
                push.normalize().multiply(0.9);
            }
            push.setY(0.35);
            e.setVelocity(push);
        }
    }

    // ----------------------------------------------------------------- PYROKAR

    private void pyrokarWave(Player p) {
        WeaponType t = WeaponType.PYROKAR;
        double range = dbl(t, "range", 10.0);
        double damage = dbl(t, "damage", 10.0);
        int fire = ticks(t, "fire-seconds", 5);

        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.0001) {
            dir = new Vector(0, 0, 1);
        }
        final Vector forward = dir.normalize();
        final Vector side = new Vector(-forward.getZ(), 0, forward.getX());
        final World w = p.getWorld();
        final Location origin = p.getLocation().add(0, 0.2, 0);
        final Set<UUID> hit = new HashSet<>();

        w.playSound(origin, Sound.ENTITY_BLAZE_SHOOT, 1.3f, 0.5f);
        w.playSound(origin, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);

        new BukkitRunnable() {
            double dist = 0.0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead()) {
                    cancel();
                    return;
                }
                dist += 1.2;
                Location c = origin.clone().add(forward.clone().multiply(dist));
                // La ola se detiene contra paredes.
                if (!c.clone().add(0, 1, 0).getBlock().isPassable()) {
                    w.spawnParticle(Particle.EXPLOSION_NORMAL, c.clone().add(0, 1, 0), 6, 0.3, 0.3, 0.3, 0.02);
                    cancel();
                    return;
                }
                for (double k = -2.0; k <= 2.0; k += 0.5) {
                    Location l = c.clone().add(side.clone().multiply(k));
                    w.spawnParticle(Particle.FLAME, l, 3, 0.1, 0.35, 0.1, 0.02);
                    if (Math.abs(k) < 1.0) {
                        w.spawnParticle(Particle.LAVA, l, 1);
                    }
                }
                w.playSound(c, Sound.ENTITY_GENERIC_BURN, 0.5f, 1.2f);

                Location body = c.clone().add(0, 1, 0);
                for (Entity e : w.getNearbyEntities(body, 2.2, 1.6, 2.2)) {
                    if (!valid(p, e) || !hit.add(e.getUniqueId())) {
                        continue;
                    }
                    LivingEntity le = (LivingEntity) e;
                    hurt(p, le, damage);
                    le.setFireTicks(fire);
                    le.setVelocity(forward.clone().multiply(0.6).setY(0.5));
                }
                if (dist >= range) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ----------------------------------------------------------------- GLACIUS

    private void glaciusDash(Player p) {
        WeaponType t = WeaponType.GLACIUS;
        double damage = dbl(t, "damage", 6.0);
        double radius = dbl(t, "radius", 4.0);
        int slow = ticks(t, "slow-seconds", 4);

        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 0.0001) {
            dir = new Vector(0, 0, 1);
        }
        dir.normalize().multiply(dbl(t, "dash-power", 1.6)).setY(0.25);
        p.setVelocity(dir);
        p.setFallDistance(0f);

        final World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.3f, 0.6f);
        w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
        final Set<UUID> hit = new HashSet<>();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                tick++;
                if (!p.isOnline() || p.isDead()) {
                    cancel();
                    return;
                }
                Location l = p.getLocation().add(0, 1, 0);
                w.spawnParticle(Particle.SNOWFLAKE, l, 12, 0.4, 0.5, 0.4, 0.02);
                w.spawnParticle(Particle.CLOUD, l, 4, 0.3, 0.3, 0.3, 0.01);
                for (LivingEntity e : targets(p, l, 2.2)) {
                    if (hit.add(e.getUniqueId())) {
                        hurt(p, e, damage);
                        frost(e, slow, 1);
                    }
                }
                if (tick >= 8) {
                    // Aterrizaje: onda de hielo.
                    p.setFallDistance(0f);
                    w.playSound(l, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.7f);
                    w.spawnParticle(Particle.SNOWFLAKE, l, 60, radius / 2.0, 0.5, radius / 2.0, 0.05);
                    animateRing(w, l, radius, 5, Particle.SNOWFLAKE, Particle.END_ROD);
                    for (LivingEntity e : targets(p, l, radius)) {
                        frost(e, slow, 2);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ----------------------------------------------------------------- CRYONAX

    private void cryonaxStorm(Player p) {
        WeaponType t = WeaponType.CRYONAX;
        double radius = dbl(t, "radius", 6.0);
        double pulseDamage = dbl(t, "pulse-damage", 3.0);
        int duration = ticks(t, "duration-seconds", 5);
        int slow = ticks(t, "slow-seconds", 3);

        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, 0));
        final World w = p.getWorld();
        w.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.8f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!p.isOnline() || p.isDead() || tick >= duration) {
                    cancel();
                    return;
                }
                Location c = p.getLocation().add(0, 1, 0);
                // Torbellino de nieve alrededor del jugador.
                for (int i = 0; i < 6; i++) {
                    double a = tick * 0.35 + i * (Math.PI / 3.0);
                    double r = radius * (0.35 + 0.65 * ((tick + i * 7) % 20) / 20.0);
                    Location l = c.clone().add(Math.cos(a) * r, -0.6 + ((tick + i * 5) % 14) * 0.12, Math.sin(a) * r);
                    w.spawnParticle(Particle.SNOWFLAKE, l, 2, 0.1, 0.1, 0.1, 0.0);
                }
                if (tick % 20 == 0) {
                    w.playSound(c, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.6f);
                    animateRing(w, c, radius, 4, Particle.SNOWFLAKE, Particle.CLOUD);
                    for (LivingEntity e : targets(p, c, radius)) {
                        hurt(p, e, pulseDamage);
                        frost(e, slow, 1);
                    }
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ----------------------------------------------------------------- helpers

    /** Ralentiza y congela visualmente a la entidad. */
    private void frost(LivingEntity e, int slowTicks, int amplifier) {
        e.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, slowTicks, amplifier));
        e.setFreezeTicks(e.getMaxFreezeTicks());
    }

    /** Dano de habilidad: pasa por el evento normal, asi WorldGuard & co. pueden cancelarlo. */
    private void hurt(Player source, LivingEntity target, double amount) {
        abilityDamage = true;
        try {
            target.damage(amount, source);
        } finally {
            abilityDamage = false;
        }
    }

    private boolean valid(Player owner, Entity e) {
        if (!(e instanceof LivingEntity le) || e.equals(owner) || e instanceof ArmorStand || le.isDead() || !le.isValid()) {
            return false;
        }
        if (e instanceof Player other
                && (other.getGameMode() == GameMode.CREATIVE || other.getGameMode() == GameMode.SPECTATOR)) {
            return false;
        }
        if (e instanceof Tameable pet && pet.isTamed() && pet.getOwner() != null
                && pet.getOwner().getUniqueId().equals(owner.getUniqueId())) {
            return false;
        }
        return true;
    }

    private List<LivingEntity> targets(Player owner, Location center, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        World w = center.getWorld();
        if (w == null) {
            return list;
        }
        double r2 = radius * radius;
        for (Entity e : w.getNearbyEntities(center, radius, radius, radius)) {
            if (!valid(owner, e)) {
                continue;
            }
            Location mid = e.getLocation().add(0, e.getHeight() / 2.0, 0);
            if (mid.distanceSquared(center) <= r2) {
                list.add((LivingEntity) e);
            }
        }
        return list;
    }

    /** Anillo de particulas que se expande desde el centro. */
    private void animateRing(World w, Location c, double radius, int steps, Particle main, Particle accent) {
        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                step++;
                double r = radius * step / steps;
                int points = Math.max(12, (int) (r * 10));
                for (int i = 0; i < points; i++) {
                    double a = 2.0 * Math.PI * i / points;
                    Location l = c.clone().add(Math.cos(a) * r, -0.7, Math.sin(a) * r);
                    w.spawnParticle(main, l, 1, 0.05, 0.1, 0.05, 0.01);
                    if (i % 3 == 0) {
                        w.spawnParticle(accent, l, 1, 0.05, 0.1, 0.05, 0.0);
                    }
                }
                if (step >= steps) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
