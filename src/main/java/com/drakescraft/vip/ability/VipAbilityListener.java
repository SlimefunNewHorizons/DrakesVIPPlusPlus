package com.drakescraft.vip.ability;

import com.drakescraft.vip.manager.VipManager;
import com.drakescraft.vip.manager.VipToggleManager;
import com.drakescraft.vip.model.VipTier;
import com.drakescraft.vip.model.VipToggleType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Habilidad activa divina EXCLUSIVA por dios (los 15 tiers tienen la suya, con sus
 * particulas y sonidos). Disparo: agacharse (sneak) + intercambiar mano (tecla F).
 * Con cooldown, desactivable por mundo/modalidad (PvP/clasico) via
 * {@code abilities.disabled-worlds}. No copia a DrakesRankup: sabor mitologico propio.
 */
public final class VipAbilityListener implements Listener {

    private final Plugin plugin;
    private final VipManager vipManager;
    private final VipToggleManager toggleManager;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public VipAbilityListener(Plugin plugin, VipManager vipManager, VipToggleManager toggleManager) {
        this.plugin = plugin;
        this.vipManager = vipManager;
        this.toggleManager = toggleManager;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        VipTier tier = vipManager.getTier(player);
        if (tier == null) {
            return;
        }
        if (isDisabledWorld(player)) {
            return;
        }
        if (!toggleManager.isEnabled(player, VipToggleType.ABILITY)) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("abilities.cooldown-seconds", 30) * 1000L;
        if (tier.getHierarchy() >= VipTier.TITAN_CRONOS.getHierarchy()) {
            cooldownMs /= 2; // Cronos y superiores: mitad de enfriamiento
        }
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(player.getUniqueId());
        if (until != null && now < until) {
            long left = (until - now) / 1000 + 1;
            player.sendActionBar(Component.text("⏳ Habilidad en enfriamiento: " + left + "s", NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.6f);
            return;
        }

        event.setCancelled(true);
        cooldowns.put(player.getUniqueId(), now + cooldownMs);
        cast(player, tier);
    }

    private void cast(Player player, VipTier tier) {
        chargeFx(player, tier); // fogonazo de carga comun
        switch (tier) {
            case HERCULES -> groundSlam(player);
            case HESTIA -> hearth(player);
            case HERMES -> blinkDash(player);
            case HEFESTO -> forgeMeteor(player);
            case ARTEMISA -> huntersVolley(player);
            case AFRODITA -> charm(player);
            case ZEUS -> lightning(player, 4.0, 9.0);
            case THOR -> thunderDash(player);
            case ANUBIS -> lifesteal(player);
            case POSEIDON -> shockwave(player, 2.2, 5.0);
            case TITAN_JAPETO -> titanForge(player);
            case TITAN_OCEANO -> maelstrom(player);
            case TITAN_HIPERION -> solarFlare(player);
            case TITAN_CRONOS -> timeStop(player);
            case TITAN_CAOS -> cataclysm(player);
        }
    }

    // ---------------------------------------------------------------- Olímpicos base

    private void groundSlam(Player player) {
        player.setVelocity(new Vector(0, 0.9, 0));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location l = player.getLocation();
            l.getWorld().spawnParticle(Particle.EXPLOSION, l, 3);
            l.getWorld().spawnParticle(Particle.BLOCK, l, 60, 1.5, 0.1, 1.5, org.bukkit.Material.DIRT.createBlockData());
            sound(player, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);
            for (LivingEntity e : nearby(l, 5.0, player)) {
                Vector push = away(l, e).multiply(1.6).setY(0.6);
                e.setVelocity(push);
                e.damage(5.0, player);
            }
        }, 12L);
    }

    private void hearth(Player player) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.HEART, l.clone().add(0, 1, 0), 20, 1.5, 1, 1.5, 0);
        l.getWorld().spawnParticle(Particle.FLAME, l, 40, 1.5, 0.5, 1.5, 0.02);
        sound(player, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
        heal(player, 6.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
        for (LivingEntity e : nearby(l, 5.0, player)) {
            if (e instanceof Player ally) {
                heal(ally, 4.0);
                ally.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0));
            }
        }
    }

    private void blinkDash(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.4).setY(0.35);
        player.setVelocity(dir);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 2));
        trail(player.getLocation(), Particle.CLOUD, 30);
        sound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.8f);
        sound(player, Sound.ITEM_TRIDENT_RIPTIDE_3, 0.8f, 1.6f);
    }

    private void forgeMeteor(Player player) {
        Location eye = player.getEyeLocation();
        var fb = player.launchProjectile(org.bukkit.entity.SmallFireball.class, eye.getDirection().multiply(1.4));
        fb.setIsIncendiary(true);
        eye.getWorld().spawnParticle(Particle.LAVA, eye, 15, 0.3, 0.3, 0.3, 0);
        sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.7f);
        sound(player, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.2f);
    }

    private void huntersVolley(Player player) {
        Location eye = player.getEyeLocation();
        Vector base = eye.getDirection();
        for (int i = -2; i <= 2; i++) {
            Vector v = base.clone().rotateAroundY(Math.toRadians(i * 8)).multiply(2.2);
            var arrow = player.launchProjectile(org.bukkit.entity.Arrow.class, v);
            arrow.setCritical(true);
            arrow.setDamage(arrow.getDamage() + 1.5);
        }
        eye.getWorld().spawnParticle(Particle.CRIT, eye, 20, 0.5, 0.5, 0.5, 0.1);
        sound(player, Sound.ENTITY_ARROW_SHOOT, 1f, 1.2f);
        sound(player, Sound.ENTITY_ARROW_SHOOT, 1f, 1.5f);
    }

    private void charm(Player player) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.HEART, l.clone().add(0, 1.5, 0), 30, 2, 1, 2, 0);
        sound(player, Sound.ENTITY_ALLAY_ITEM_GIVEN, 1f, 1.4f);
        for (LivingEntity e : nearby(l, 6.0, player)) {
            if (e instanceof org.bukkit.entity.Monster m) {
                m.setTarget(null);
                m.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2));
                m.getWorld().spawnParticle(Particle.HEART, m.getLocation().add(0, 1.5, 0), 5);
            }
        }
    }

    // ---------------------------------------------------------------- Olímpicos altos

    private void lightning(Player player, double radius, double dmg) {
        Location target = player.getTargetBlockExact(30) != null
                ? player.getTargetBlockExact(30).getLocation().add(0.5, 1, 0.5)
                : player.getLocation();
        target.getWorld().strikeLightningEffect(target);
        target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, target, 40, 1, 1, 1, 0.1);
        sound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1.3f);
        for (LivingEntity e : nearby(target, radius, player)) {
            e.damage(dmg, player);
        }
    }

    private void thunderDash(Player player) {
        Vector dir = player.getLocation().getDirection().normalize().multiply(2.0).setY(0.7);
        player.setVelocity(dir);
        trail(player.getLocation(), Particle.ELECTRIC_SPARK, 40);
        sound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.7f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Location l = player.getLocation();
            l.getWorld().strikeLightningEffect(l);
            for (LivingEntity e : nearby(l, 4.0, player)) {
                e.damage(7.0, player);
            }
        }, 10L);
    }

    private void lifesteal(Player player) {
        double healed = 0;
        Location l = player.getLocation();
        for (LivingEntity e : nearby(l, 5.5, player)) {
            e.damage(5.0, player);
            e.getWorld().spawnParticle(Particle.SCULK_SOUL, e.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
            healed += 2.0;
        }
        if (healed > 0) {
            heal(player, healed);
        }
        player.getWorld().spawnParticle(Particle.SOUL, l.add(0, 1, 0), 20, 0.5, 0.8, 0.5, 0.02);
        sound(player, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 0.8f);
        sound(player, Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.6f);
    }

    private void shockwave(Player player, double power, double radius) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.EXPLOSION, l, 3);
        l.getWorld().spawnParticle(Particle.SPLASH, l, 60, radius / 2, 0.3, radius / 2, 0.1);
        l.getWorld().spawnParticle(Particle.BUBBLE_POP, l, 40, radius / 2, 0.3, radius / 2, 0.1);
        sound(player, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1f, 0.8f);
        sound(player, Sound.ITEM_TRIDENT_THUNDER, 1f, 1.2f);
        for (LivingEntity e : nearby(l, radius, player)) {
            e.setVelocity(away(l, e).multiply(power).setY(0.55));
            e.damage(4.0, player);
        }
    }

    // ---------------------------------------------------------------- Titanes

    private void titanForge(Player player) {
        heal(player, 4.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 1));
        var item = player.getInventory().getItemInMainHand();
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg && dmg.hasDamage()) {
            dmg.setDamage(Math.max(0, dmg.getDamage() - 200));
            item.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmg);
        }
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.LAVA, l, 30, 1, 0.5, 1, 0);
        l.getWorld().spawnParticle(Particle.CRIT, l.add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
        sound(player, Sound.BLOCK_ANVIL_USE, 1f, 0.8f);
        sound(player, Sound.ITEM_TOTEM_USE, 0.5f, 1.4f);
    }

    private void maelstrom(Player player) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.FALLING_WATER, l.clone().add(0, 3, 0), 80, 2, 2, 2, 0);
        sound(player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1.2f);
        for (LivingEntity e : nearby(l, 7.0, player)) {
            e.setVelocity(away(e.getLocation(), l).multiply(0.9).setY(0.2)); // atrae hacia el centro
            e.damage(3.0, player);
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
        }
    }

    private void solarFlare(Player player) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.FLASH, l.clone().add(0, 1, 0), 2);
        l.getWorld().spawnParticle(Particle.FLAME, l, 120, 3, 1, 3, 0.05);
        sound(player, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
        sound(player, Sound.ITEM_FIRECHARGE_USE, 1f, 0.7f);
        for (LivingEntity e : nearby(l, 6.0, player)) {
            e.setFireTicks(120);
            e.damage(6.0, player);
            e.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
        }
    }

    private void timeStop(Player player) {
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(0, 1, 0), 60, 2, 1, 2, 0.02);
        l.getWorld().spawnParticle(Particle.REVERSE_PORTAL, l, 80, 2, 1, 2, 0.1);
        sound(player, Sound.BLOCK_BELL_RESONATE, 1f, 0.6f);
        sound(player, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.8f);
        for (LivingEntity e : nearby(l, 8.0, player)) {
            e.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 140, 6));
            e.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 140, 2));
            if (e instanceof org.bukkit.entity.Mob mob) {
                mob.setAware(false);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> mob.setAware(true), 140L);
            }
        }
    }

    private void cataclysm(Player player) {
        shockwave(player, 3.0, 7.0);
        lightning(player, 6.0, 12.0);
        Location l = player.getLocation();
        l.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, l, 2);
        // Paper 1.21.11 declares DRAGON_BREATH with Float particle data. The
        // data-less overload throws during the cast and aborts Cataclysm.
        l.getWorld().spawnParticle(Particle.DRAGON_BREATH, l.clone().add(0, 1, 0), 100, 3, 2, 3, 0.05, 0.0F);
        sound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 0.7f);
        sound(player, Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
    }

    // ---------------------------------------------------------------- helpers

    private void chargeFx(Player player, VipTier tier) {
        Color c = bandColor(tier);
        Particle.DustOptions dust = new Particle.DustOptions(c, 1.6f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 0.9, 0.5, dust);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.2f);
    }

    private Color bandColor(VipTier tier) {
        return switch (tier.getBand()) {
            case OLYMPIAN_BASE -> Color.fromRGB(120, 200, 255);
            case OLYMPIAN_HIGH -> Color.fromRGB(255, 215, 0);
            case TITAN -> Color.fromRGB(170, 80, 255);
        };
    }

    private void heal(LivingEntity e, double amount) {
        double max = e.getAttribute(Attribute.MAX_HEALTH).getValue();
        e.setHealth(Math.min(max, e.getHealth() + amount));
    }

    private void trail(Location l, Particle p, int count) {
        l.getWorld().spawnParticle(p, l.add(0, 0.5, 0), count, 0.3, 0.3, 0.3, 0.03);
    }

    private void sound(Player player, Sound s, float vol, float pitch) {
        player.getWorld().playSound(player.getLocation(), s, vol, pitch);
    }

    private Vector away(Location center, LivingEntity e) {
        return e.getLocation().toVector().subtract(center.toVector()).normalize();
    }

    private Vector away(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }

    private List<LivingEntity> nearby(Location loc, double r, Player self) {
        return loc.getWorld().getNearbyEntities(loc, r, r, r).stream()
                .filter(en -> en instanceof LivingEntity && !en.equals(self))
                .map(en -> (LivingEntity) en)
                .toList();
    }

    private boolean isDisabledWorld(Player player) {
        Set<String> disabled = new HashSet<>(plugin.getConfig().getStringList("abilities.disabled-worlds"));
        String world = player.getWorld().getName().toLowerCase();
        for (String d : disabled) {
            if (world.startsWith(d.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
