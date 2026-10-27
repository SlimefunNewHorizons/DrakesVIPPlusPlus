package com.drakescraft.vip.manager;

import com.drakescraft.vip.model.VipTier;

import com.drakescraft.vip.model.VipToggleType;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Aplica los buffs pasivos por banda (corazones extra + efectos de pocion
 * infinitos y ambientales) leidos de tiers.yml. Idempotente: el modificador de
 * vida se identifica por NamespacedKey, asi que re-aplicar no acumula.
 */
public final class VipBuffManager {

    private final Plugin plugin;
    private final Logger logger;
    private final NamespacedKey healthKey;
    private VipToggleManager toggleManager;

    public VipBuffManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.healthKey = new NamespacedKey(plugin, "vip_extra_hearts");
    }

    public void setToggleManager(VipToggleManager toggleManager) {
        this.toggleManager = toggleManager;
    }

    /** Aplica los buffs del tier (o los quita si tier es null o estan desactivados). */
    public void apply(Player player, VipTier tier) {
        clear(player);
        if (tier == null) {
            return;
        }
        if (toggleManager != null && !toggleManager.isEnabled(player, VipToggleType.PASSIVE_BUFFS)) {
            return;
        }
        ConfigurationSection band = plugin.getConfig()
                .getConfigurationSection("bands." + tier.getBand().name());
        if (band == null) {
            return;
        }

        double extraHearts = band.getDouble("extra-hearts", 0.0D);
        if (extraHearts > 0.0D) {
            applyExtraHearts(player, extraHearts * 2.0D); // 1 corazon = 2 puntos de vida
        }

        for (String raw : band.getStringList("effects")) {
            PotionEffect effect = parseEffect(raw);
            if (effect != null) {
                player.addPotionEffect(effect);
            }
        }

        // Pasivas tematicas exclusivas por dios (encima de la banda).
        for (String raw : godPassives(tier)) {
            PotionEffect effect = parseEffect(raw);
            if (effect != null) {
                player.addPotionEffect(effect);
            }
        }
    }

    /** Efecto pasivo con sabor mitologico propio de cada dios. */
    private java.util.List<String> godPassives(VipTier tier) {
        return switch (tier) {
            case HERCULES -> java.util.List.of("STRENGTH:0");
            case HESTIA -> java.util.List.of("FIRE_RESISTANCE:0");
            case HERMES -> java.util.List.of("SPEED:1");
            case HEFESTO -> java.util.List.of("FIRE_RESISTANCE:0", "HASTE:0");
            case ARTEMISA -> java.util.List.of("NIGHT_VISION:0", "LUCK:0");
            case AFRODITA -> java.util.List.of("REGENERATION:0", "LUCK:0");
            case ZEUS -> java.util.List.of("RESISTANCE:0", "SPEED:0");
            case THOR -> java.util.List.of("STRENGTH:0", "FIRE_RESISTANCE:0");
            case ANUBIS -> java.util.List.of("NIGHT_VISION:0", "RESISTANCE:0");
            case POSEIDON -> java.util.List.of("WATER_BREATHING:0", "DOLPHINS_GRACE:0");
            case TITAN_JAPETO -> java.util.List.of("HASTE:1");
            case TITAN_OCEANO -> java.util.List.of("WATER_BREATHING:0", "CONDUIT_POWER:0", "DOLPHINS_GRACE:0");
            case TITAN_HIPERION -> java.util.List.of("FIRE_RESISTANCE:0", "NIGHT_VISION:0");
            case TITAN_CRONOS -> java.util.List.of("HASTE:0");
            case TITAN_CAOS -> java.util.List.of("FIRE_RESISTANCE:0", "NIGHT_VISION:0", "WATER_BREATHING:0");
        };
    }

    /** Quita todos los buffs VIP del jugador (corazones y pociones infinitas). */
    public void clear(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.getModifiers().stream()
                    .filter(m -> healthKey.equals(m.getKey()))
                    .forEach(attr::removeModifier);
        }
        for (PotionEffect pe : player.getActivePotionEffects()) {
            if (pe.isInfinite()) {
                player.removePotionEffect(pe.getType());
            }
        }
    }

    private void applyExtraHearts(Player player, double amount) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        // Evita duplicar si ya existe (clear() lo remueve antes, pero por seguridad).
        boolean exists = attr.getModifiers().stream().anyMatch(m -> healthKey.equals(m.getKey()));
        if (!exists) {
            attr.addModifier(new AttributeModifier(healthKey, amount, AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    /** Formato "TIPO:amplificador", p. ej. "SPEED:0" (Speed I). */
    private PotionEffect parseEffect(String raw) {
        try {
            String[] parts = raw.split(":");
            String name = parts[0].trim().toUpperCase(Locale.ROOT);
            int amplifier = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            PotionEffectType type = resolveType(name);
            if (type == null) {
                logger.warning("Efecto VIP desconocido en tiers.yml: " + raw);
                return null;
            }
            return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, true, false, false);
        } catch (RuntimeException ex) {
            logger.warning("Efecto VIP mal formado en tiers.yml: '" + raw + "' (" + ex.getMessage() + ")");
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private PotionEffectType resolveType(String name) {
        PotionEffectType type = org.bukkit.Registry.EFFECT.get(NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
        if (type == null) {
            type = PotionEffectType.getByName(name);
        }
        return type;
    }

    /** Utilidad para listar los tipos aplicados (debug). */
    public List<String> describe(VipTier tier) {
        List<String> out = new ArrayList<>();
        if (tier == null) {
            return out;
        }
        ConfigurationSection band = plugin.getConfig().getConfigurationSection("bands." + tier.getBand().name());
        if (band != null) {
            out.add("+" + band.getDouble("extra-hearts", 0) + " corazones");
            out.addAll(band.getStringList("effects"));
        }
        return out;
    }
}
