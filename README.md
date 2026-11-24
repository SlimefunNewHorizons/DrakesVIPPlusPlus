<!-- credits:start -->
> **Credits & license.** Port and maintenance by [JackStar6677-1](https://github.com/JackStar6677-1) and [DrakesCraft Labs](https://github.com/SlimefunNewHorizons). Original authors: DrakesCraft-Labs (plugin.yml). Upstream license: **Other** (see [LICENSE](LICENSE)). Details: [docs/UPSTREAM_ATTRIBUTION.md](docs/UPSTREAM_ATTRIBUTION.md) · Policy: [Rights & Attribution](https://github.com/SlimefunNewHorizons/.github/blob/main/RIGHTS_AND_ATTRIBUTION.md). Independent community project, not affiliated with or endorsed by the original authors or the official Slimefun team.
>
> **Créditos y licencia.** Port y mantenimiento por [JackStar6677-1](https://github.com/JackStar6677-1) y [DrakesCraft Labs](https://github.com/SlimefunNewHorizons). Autores originales: DrakesCraft-Labs (plugin.yml). Licencia original: **Other**. Detalle en [docs/UPSTREAM_ATTRIBUTION.md](docs/UPSTREAM_ATTRIBUTION.md).
<!-- credits:end -->

<div align="center">

# 👑 DrakesVIPPlusPlus (`DrakesVIP++`)

**Sistema de perks activos por rango VIP (15 tiers en 3 bandas) y boosters de fin de semana para DrakesCraft**

<p>
  <img src="https://img.shields.io/badge/Purpur-26.2_%2F_1.21.x-8B5CF6?style=for-the-badge&logo=minecraft&logoColor=white" alt="Purpur 26.2"/>
  <img src="https://img.shields.io/badge/Java-21%2F25-F89820?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21/25"/>
  <img src="https://img.shields.io/badge/Version-1.0.0-22C55E?style=for-the-badge" alt="Version 1.0.0"/>
</p>

</div>

> ### 🏰 Comunidad Oficial de DrakesCraft
>
> * 🎮 **IP del Servidor**: `mc.drakescraft.cl` *(Java & Bedrock)*
> * 💬 **Discord Oficial**: [discord.gg/rv3vtXZTk7](https://discord.gg/rv3vtXZTk7)
> * 🌐 **Portal Web & Guía**: [web.drakescraft.cl](https://web.drakescraft.cl)
> * 🛒 **Tienda Oficial**: [drakescraft-v2.tebex.io](https://drakescraft-v2.tebex.io)

---

## Qué hace

`DrakesVIPPlusPlus` gestiona los beneficios activos en juego para los **15 tiers VIP** de DrakesCraft (agrupados en las bandas `OLYMPIAN_BASE`, `OLYMPIAN_HIGH` y `TITAN`) junto con el scheduler de **boosters de fin de semana**.

Por diseño, es un plugin de solo lectura respecto a rangos: **lee los grupos desde LuckPerms** y **no modifica la entrega de compras Tebex**, la cual permanece aislada y autoritativa en `Odysseia` / `DrakesServer`.

## Características

- **Buffs pasivos por banda (`config.yml` / `tiers.yml`):** corazones extra (`extra-hearts`) y efectos ambientales infinitos (`SPEED`, `NIGHT_VISION`, etc.).
- **Habilidad activa VIP (bandas alta y titán):** disparo con `agacharse + F (swap-hand)` y cooldown configurable, desactivable por prefijo de mundo (`clasico`, `pvp`, `arena`) para preservar el balance.
- **Cosméticos y feedback visual:** aura cosmética VIP (`drakesvip.cosmetic.aura`), efecto al conectar (`join-burst`) y efecto al eliminar enemigos (`kill-flourish`).
- **Boosters programados (`boosters.yml`):** scheduler configurable para ventanas de booster globales de fin de semana.

## Comandos y Permisos

| Comando / Permiso | Descripción |
| :--- | :--- |
| `/vip` *(aliases: `/vips`, `/drakesvip`)* | Muestra tu rango VIP activo, buffs y estado del booster. |
| `/vip reload` | Recarga `config.yml`, `tiers.yml` y `boosters.yml` *(requiere `drakesvip.admin`)*. |
| `drakesvip.admin` *(default: `op`)* | Acceso a `/vip reload`. |
| `drakesvip.cosmetic.aura` *(default: `true`)* | Habilita el aura cosmética VIP. |

## Compatibilidad

| Componente | Requisito |
| :--- | :--- |
| **Servidor** | **Purpur 26.2** (Temporada 2) / Paper **1.21.x** |
| **Java** | Compilado con **JDK 21** (compatible con **Java 21 y Java 25**) |
| **Dependencia requerida** | LuckPerms |
| **Dependencias opcionales** | Vault, PlaceholderAPI, AuraSkills, Odysseia |
| **Versión** | `1.0.0` |

## Compilación

Requisitos: **JDK 21+** y **Maven 3.9+**.

```bash
mvn clean package
```

El JAR compilado se genera en `target/DrakesVIPPlusPlus-1.0.0.jar`.

---

## 📄 License & Intellectual Property

Copyright © 2026 [**JackStar6677-1**](https://github.com/JackStar6677-1) · [**DrakesCraft Labs**](https://github.com/SlimefunNewHorizons). All Rights Reserved.

This software is **Source-Available** for public inspection and technical audit. Redistribution, commercial repackaging, or unauthorized derivative distribution without explicit written permission from the author is strictly prohibited.
