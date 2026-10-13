package com.drakescraft.vip.manager;

import com.drakescraft.vip.model.VipTier;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Estado y calculo de los boosters. Un booster se define por dias + ventanas
 * horarias + multiplicador global + a que afecta (MONEY/XP/SKILL). El VIP suma un
 * extra por banda encima del global (p. ej. global x2 + Titan +1.0 = x3).
 * Solo estado/calculo; el {@link com.drakescraft.vip.task.BoosterScheduler} decide
 * las transiciones y anuncios.
 */
public final class BoosterManager {

    public enum BoostType { MONEY, XP, SKILL }

    private final Logger logger;
    private final VipManager vipManager;

    private final List<BoosterDef> definitions = new ArrayList<>();
    private final Map<VipTier.VipBand, Double> vipExtra = new EnumMap<>(VipTier.VipBand.class);
    /** Bono permanente de dinero de Jobs por banda VIP, independiente de los boosters. */
    private final Map<VipTier.VipBand, Double> jobsBonus = new EnumMap<>(VipTier.VipBand.class);

    public BoosterManager(Logger logger, VipManager vipManager) {
        this.logger = logger;
        this.vipManager = vipManager;
    }

    /** (Re)carga las definiciones desde boosters.yml. */
    public void load(FileConfiguration cfg) {
        definitions.clear();
        vipExtra.clear();
        jobsBonus.clear();

        ConfigurationSection boosters = cfg.getConfigurationSection("boosters");
        if (boosters != null) {
            for (String key : boosters.getKeys(false)) {
                ConfigurationSection s = boosters.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                try {
                    definitions.add(parse(key, s));
                } catch (RuntimeException ex) {
                    logger.warning("Booster '" + key + "' mal configurado: " + ex.getMessage());
                }
            }
        }

        ConfigurationSection extra = cfg.getConfigurationSection("vip-extra");
        vipExtra.put(VipTier.VipBand.OLYMPIAN_BASE, extra != null ? extra.getDouble("olympian-base", 0.25) : 0.25);
        vipExtra.put(VipTier.VipBand.OLYMPIAN_HIGH, extra != null ? extra.getDouble("olympian-high", 0.5) : 0.5);
        vipExtra.put(VipTier.VipBand.TITAN, extra != null ? extra.getDouble("titan", 1.0) : 1.0);

        ConfigurationSection jobs = cfg.getConfigurationSection("jobs-bonus");
        jobsBonus.put(VipTier.VipBand.OLYMPIAN_BASE, jobs != null ? jobs.getDouble("olympian-base", 0.05) : 0.05);
        jobsBonus.put(VipTier.VipBand.OLYMPIAN_HIGH, jobs != null ? jobs.getDouble("olympian-high", 0.10) : 0.10);
        jobsBonus.put(VipTier.VipBand.TITAN, jobs != null ? jobs.getDouble("titan", 0.20) : 0.20);

        logger.info("Boosters cargados: " + definitions.size());
    }

    private BoosterDef parse(String id, ConfigurationSection s) {
        List<DayOfWeek> days = new ArrayList<>();
        for (String d : s.getStringList("days")) {
            days.add(DayOfWeek.valueOf(d.trim().toUpperCase(Locale.ROOT)));
        }
        List<int[]> windows = new ArrayList<>();
        for (String w : s.getStringList("windows")) {
            String[] parts = w.split("-");
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            windows.add(new int[]{start.toSecondOfDay(), end.toSecondOfDay()});
        }
        List<BoostType> affects = new ArrayList<>();
        for (String a : s.getStringList("affects")) {
            affects.add(BoostType.valueOf(a.trim().toUpperCase(Locale.ROOT)));
        }
        double global = s.getDouble("global-multiplier", 2.0);
        boolean announce = s.getBoolean("announce", true);
        return new BoosterDef(id, days, windows, affects, global, announce);
    }

    /** ¿Hay algun booster activo ahora para ese tipo? */
    public boolean isActive(BoostType type) {
        return globalMultiplier(type) > 1.0;
    }

    /** Mayor multiplicador global activo para el tipo (1.0 si ninguno). */
    public double globalMultiplier(BoostType type) {
        LocalDateTime now = LocalDateTime.now();
        double best = 1.0;
        for (BoosterDef def : definitions) {
            if (def.affects.contains(type) && def.isActive(now)) {
                best = Math.max(best, def.global);
            }
        }
        return best;
    }

    /**
     * Multiplicador efectivo para un jugador: global * (1 + extra VIP de su banda).
     * Si no hay booster activo devuelve 1.0.
     */
    public double effectiveMultiplier(Player player, BoostType type) {
        double global = globalMultiplier(type);
        if (global <= 1.0) {
            return 1.0;
        }
        VipTier tier = vipManager.getTier(player);
        if (tier == null) {
            return global;
        }
        double extra = vipExtra.getOrDefault(tier.getBand(), 0.0);
        return global * (1.0 + extra);
    }

    /** Bono permanente de Jobs de la banda del jugador (0.0 si no es VIP). */
    public double permanentJobsBonus(Player player) {
        VipTier tier = vipManager.getTier(player);
        if (tier == null) {
            return 0.0;
        }
        return jobsBonus.getOrDefault(tier.getBand(), 0.0);
    }

    /**
     * Multiplicador del dinero de Jobs: el de dinero con booster (global x extra de banda) por el
     * bono permanente de la banda. 1.0 para quien no es VIP y no hay booster.
     */
    public double jobsMoneyMultiplier(Player player) {
        return effectiveMultiplier(player, BoostType.MONEY) * (1.0 + permanentJobsBonus(player));
    }

    public List<BoosterDef> getDefinitions() {
        return definitions;
    }

    /** Definicion inmutable de un booster. */
    public static final class BoosterDef {
        public final String id;
        public final List<DayOfWeek> days;
        public final List<int[]> windows; // segundos del dia [start,end]
        public final List<BoostType> affects;
        public final double global;
        public final boolean announce;

        BoosterDef(String id, List<DayOfWeek> days, List<int[]> windows,
                   List<BoostType> affects, double global, boolean announce) {
            this.id = id;
            this.days = days;
            this.windows = windows;
            this.affects = affects;
            this.global = global;
            this.announce = announce;
        }

        public boolean isActive(LocalDateTime now) {
            if (!days.isEmpty() && !days.contains(now.getDayOfWeek())) {
                return false;
            }
            int sec = now.toLocalTime().toSecondOfDay();
            for (int[] w : windows) {
                if (w[0] <= w[1]) {
                    if (sec >= w[0] && sec < w[1]) {
                        return true;
                    }
                } else { // ventana que cruza medianoche
                    if (sec >= w[0] || sec < w[1]) {
                        return true;
                    }
                }
            }
            return windows.isEmpty();
        }
    }
}
