package com.drakescraft.vip.api;

import com.drakescraft.vip.manager.BoosterManager;
import com.drakescraft.vip.manager.BoosterManager.BoostType;
import com.drakescraft.vip.manager.VipManager;
import com.drakescraft.vip.model.VipTier;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * Punto de acceso publico para otros plugins de DrakesCraft (p. ej. DrakesSlimeMarket
 * o las tiendas) que necesiten aplicar el multiplicador de dinero del booster VIP.
 *
 * <p>Uso tipico en el pago de una venta:
 * <pre>{@code
 *   double pago = base * DrakesVipApi.getMoneyMultiplier(player);
 * }</pre>
 */
public final class DrakesVipApi {

    private static VipManager vipManager;
    private static BoosterManager boosterManager;

    private DrakesVipApi() {
    }

    /** Inicializado por el plugin en onEnable. */
    public static void init(VipManager vip, BoosterManager booster) {
        vipManager = vip;
        boosterManager = booster;
    }

    private static boolean ready() {
        return vipManager != null && boosterManager != null;
    }

    /** Multiplicador de dinero efectivo (1.0 si no hay booster). Nunca null. */
    public static double getMoneyMultiplier(Player player) {
        return ready() ? boosterManager.effectiveMultiplier(player, BoostType.MONEY) : 1.0;
    }

    /**
     * Multiplicador del dinero que paga Jobs: booster de dinero activo por el bono permanente de la
     * banda VIP. Lo consume DrakesRankup al potenciar los pagos de Jobs. 1.0 si no aplica.
     */
    public static double getJobsMoneyMultiplier(Player player) {
        return ready() ? boosterManager.jobsMoneyMultiplier(player) : 1.0;
    }

    public static double getXpMultiplier(Player player) {
        return ready() ? boosterManager.effectiveMultiplier(player, BoostType.XP) : 1.0;
    }

    public static double getSkillMultiplier(Player player) {
        return ready() ? boosterManager.effectiveMultiplier(player, BoostType.SKILL) : 1.0;
    }

    @Nullable
    public static VipTier getTier(Player player) {
        return ready() ? vipManager.getTier(player) : null;
    }

    public static boolean isVip(Player player) {
        return ready() && vipManager.isVip(player);
    }

    public static boolean isMoneyBoosterActive() {
        return ready() && boosterManager.isActive(BoostType.MONEY);
    }
}
