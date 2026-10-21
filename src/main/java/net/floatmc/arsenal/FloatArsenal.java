package net.floatmc.arsenal;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * FloatArsenal - armas legendarias de fuego y hielo para FloatMC (Paper 1.20.1).
 */
public final class FloatArsenal extends JavaPlugin {

    private NamespacedKey weaponKey;
    private Abilities abilities;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        weaponKey = new NamespacedKey(this, "weapon_id");
        abilities = new Abilities(this);

        Bukkit.getPluginManager().registerEvents(new WeaponListener(this, abilities), this);

        PluginCommand cmd = getCommand("floatarsenal");
        if (cmd != null) {
            ArsenalCommand handler = new ArsenalCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        } else {
            getLogger().severe("No se pudo registrar el comando /floatarsenal (revisa plugin.yml).");
        }

        registerRecipes();
        for (Player p : Bukkit.getOnlinePlayers()) {
            discoverRecipesFor(p);
        }
        getLogger().info("FloatArsenal activado: " + WeaponType.values().length + " armas legendarias cargadas.");
    }

    @Override
    public void onDisable() {
        unregisterRecipes();
    }

    public NamespacedKey weaponKey() {
        return weaponKey;
    }

    public void reloadAll() {
        reloadConfig();
        registerRecipes();
        for (Player p : Bukkit.getOnlinePlayers()) {
            discoverRecipesFor(p);
        }
    }

    public void discoverRecipesFor(Player p) {
        if (!recipeKeys.isEmpty() && getConfig().getBoolean("discover-recipes", true)) {
            p.discoverRecipes(recipeKeys);
        }
    }

    private void registerRecipes() {
        unregisterRecipes();
        if (!getConfig().getBoolean("recipes-enabled", true)) {
            return;
        }
        for (WeaponType t : WeaponType.values()) {
            ShapedRecipe recipe = t.buildRecipe(this, t.create(this));
            if (Bukkit.addRecipe(recipe)) {
                recipeKeys.add(recipe.getKey());
            } else {
                getLogger().warning("No se pudo registrar la receta de " + t.id());
            }
        }
    }

    private void unregisterRecipes() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }
        recipeKeys.clear();
    }
}
