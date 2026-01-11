package com.artillexstudios.axplayerwarps.hooks;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axintegrations.AxIntegrations;
import com.artillexstudios.axintegrations.integration.protection.ProtectionIntegration;
import com.artillexstudios.axintegrations.integration.protection.ProtectionIntegrations;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.ExperienceHook;
import com.artillexstudios.axplayerwarps.hooks.currency.PlaceholderCurrencyHook;
import com.artillexstudios.axplayerwarps.hooks.currency.VaultHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CURRENCIES;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.HOOKS;

public class HookManager {

    private static final ArrayList<CurrencyHook> currency = new ArrayList<>();

    public static void setupHooks() {
        updateHooks();
    }

    public static void updateHooks() {
        currency.removeIf(currencyHook -> !currencyHook.isPersistent());

        ProtectionIntegrations.values().clear();
        AxIntegrations.INSTANCE.init();

        ProtectionIntegrations.values().removeIf(integration -> !HOOKS.getBoolean("hooks.protection." + integration.id(), false));
        boolean modified = false;
        for (ProtectionIntegration integration : ProtectionIntegrations.values()) {
            if (HOOKS.getString("hooks.protection." + integration.id(), null) == null) {
                modified = true;
                HOOKS.set("hooks.protection." + integration.id(), true);
            }
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into " + integration.id() + "!"));
        }
        if (modified) HOOKS.save();

        if (CURRENCIES.getBoolean("currencies.Experience.register", true))
            currency.add(new ExperienceHook());

        if (CURRENCIES.getBoolean("currencies.Vault.register", true) && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            currency.add(new VaultHook());
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into Vault!"));
        }

        for (String str : CURRENCIES.getSection("placeholder-currencies").getRoutesAsStrings(false)) {
            if (!CURRENCIES.getBoolean("placeholder-currencies." + str + ".register", false)) continue;
            currency.add(new PlaceholderCurrencyHook(str, CURRENCIES.getSection("placeholder-currencies." + str)));
            Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Loaded placeholder currency " + str + "!"));
        }

        for (CurrencyHook hook : currency) hook.setup();
    }

    @SuppressWarnings("unused")
    public static void registerCurrencyHook(@NotNull Plugin plugin, @NotNull CurrencyHook currencyHook) {
        currency.add(currencyHook);
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#33FF33[AxPlayerWarps] Hooked into " + plugin.getName() + "! Note: You must set the currency provider to CUSTOM or it will be overridden after reloading!"));
    }

    @NotNull
    public static ArrayList<CurrencyHook> getCurrency() {
        return currency;
    }

    @Nullable
    public static CurrencyHook getCurrencyHook(@NotNull String name) {
        for (CurrencyHook hook : currency) {
            if (!hook.getName().equals(name)) continue;
            return hook;
        }

        return null;
    }

    public static boolean canBuild(Player player, Location location) {
        for (ProtectionIntegration integration : ProtectionIntegrations.values()) {
            if (integration.canBuild(player, location)) continue;
            return false;
        }
        return true;
    }
}
