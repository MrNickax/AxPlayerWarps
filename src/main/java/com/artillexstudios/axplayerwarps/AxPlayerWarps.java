package com.artillexstudios.axplayerwarps;

import com.artillexstudios.axapi.AxPlugin;
import com.artillexstudios.axapi.config.Config;
import com.artillexstudios.axapi.dependencies.DependencyManagerWrapper;
import com.artillexstudios.axapi.executor.ThreadedQueue;
import com.artillexstudios.axapi.libs.boostedyaml.dvs.versioning.BasicVersioning;
import com.artillexstudios.axapi.libs.boostedyaml.settings.dumper.DumperSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.general.GeneralSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.loader.LoaderSettings;
import com.artillexstudios.axapi.libs.boostedyaml.settings.updater.UpdaterSettings;
import com.artillexstudios.axapi.utils.AsyncUtils;
import com.artillexstudios.axapi.utils.MessageUtils;
import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axapi.utils.featureflags.FeatureFlags;
import com.artillexstudios.axguiframework.GuiManager;
import com.artillexstudios.axplayerwarps.category.CategoryManager;
import com.artillexstudios.axplayerwarps.commands.CommandManager;
import com.artillexstudios.axplayerwarps.database.Database;
import com.artillexstudios.axplayerwarps.database.impl.MySQL;
import com.artillexstudios.axplayerwarps.guis.BlacklistGui;
import com.artillexstudios.axplayerwarps.guis.CategoryGui;
import com.artillexstudios.axplayerwarps.guis.EditWarpGui;
import com.artillexstudios.axplayerwarps.guis.FavoritesGui;
import com.artillexstudios.axplayerwarps.guis.MyWarpsGui;
import com.artillexstudios.axplayerwarps.guis.RateWarpGui;
import com.artillexstudios.axplayerwarps.guis.RecentsGui;
import com.artillexstudios.axplayerwarps.guis.WarpsGui;
import com.artillexstudios.axplayerwarps.guis.WhitelistGui;
import com.artillexstudios.axplayerwarps.hooks.HookManager;
import com.artillexstudios.axplayerwarps.input.InputListener;
import com.artillexstudios.axplayerwarps.libraries.Libraries;
import com.artillexstudios.axplayerwarps.listeners.MoveListener;
import com.artillexstudios.axplayerwarps.listeners.PlayerListeners;
import com.artillexstudios.axplayerwarps.listeners.WorldListeners;
import com.artillexstudios.axplayerwarps.placeholders.WarpPlaceholders;
import com.artillexstudios.axplayerwarps.redis.WarpMessageHandler;
import com.artillexstudios.axplayerwarps.sorting.SortingManager;
import com.artillexstudios.axplayerwarps.utils.UpdateNotifier;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import com.artillexstudios.axplayerwarps.warps.WarpQueue;
import com.artillexstudios.axplayerwarps.world.WorldManager;
import com.nickax.yadro.core.connection.LettuceConnectionConfig;
import com.nickax.yadro.core.messaging.LettucePublisher;
import com.nickax.yadro.core.messaging.LettuceSubscriber;
import com.nickax.yadro.paper.server.ServerTransfer;
import org.bukkit.Bukkit;
import revxrsal.zapper.DependencyManager;
import revxrsal.zapper.relocation.Relocation;

import java.io.File;

public final class AxPlayerWarps extends AxPlugin {

    private static AxPlugin instance;
    private static ThreadedQueue<Runnable> threadedQueue;
    private static Database database;
    public static MessageUtils MESSAGEUTILS;
    public static Config CONFIG;
    public static Config HOOKS;
    public static Config LANG;
    public static Config CURRENCIES;
    public static Config INPUT;
    public static LettucePublisher PUBLISHER;
    public static String REDIS_CHANNEL;
    public static ServerTransfer SERVER_TRANSFER;
    private LettuceSubscriber subscriber;

    public static ThreadedQueue<Runnable> getThreadedQueue() {
        return threadedQueue;
    }

    public static Database getDatabase() {
        return database;
    }

    public static AxPlugin getInstance() {
        return instance;
    }

    @Override
    public void dependencies(DependencyManagerWrapper manager) {
        instance = this;
        manager.repository("https://jitpack.io/");
        manager.repository("https://repo.codemc.org/repository/maven-public/");
        manager.repository("https://repo.papermc.io/repository/maven-public/");
        manager.repository("https://repo.artillex-studios.com/releases/");

        DependencyManager dependencyManager = manager.wrapped();
        for (Libraries lib : Libraries.values()) {
            dependencyManager.dependency(lib.fetchLibrary());
            for (Relocation relocation : lib.relocations()) {
                dependencyManager.relocate(relocation);
            }
        }
    }

    // todo future plans
    // - desc color codes
    // - teleport price tax
    public void enable() {
        instance = this;

        CONFIG = new Config(new File(getDataFolder(), "config.yml"), getResource("config.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        HOOKS = new Config(new File(getDataFolder(), "hooks.yml"), getResource("hooks.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        LANG = new Config(new File(getDataFolder(), "lang.yml"), getResource("lang.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build());
        CURRENCIES = new Config(new File(getDataFolder(), "currencies.yml"), getResource("currencies.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build());
        INPUT = new Config(new File(getDataFolder(), "input.yml"), getResource("input.yml"), GeneralSettings.builder().setUseDefaults(false).build(), LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("version")).build());

        InputConverter.start();

        threadedQueue = new ThreadedQueue<>("AxPlayerWarps-Datastore-thread");

        MESSAGEUTILS = new MessageUtils(LANG.getBackingDocument(), "prefix", CONFIG.getBackingDocument());

        LettuceConnectionConfig lettuceConnectionConfig = LettuceConnectionConfig.builder(
                CONFIG.getString("redis.address"),
                CONFIG.getInt("redis.port"),
                null,
                null,
                CONFIG.getString("redis.username"),
                CONFIG.getString("redis.password"),
                getLogger()
        ).build();

        PUBLISHER = new LettucePublisher(lettuceConnectionConfig);
        REDIS_CHANNEL = CONFIG.getString("redis.channel");

        SERVER_TRANSFER = new ServerTransfer(this);
        SERVER_TRANSFER.enable();

        subscriber = new LettuceSubscriber(lettuceConnectionConfig);
        subscriber.listen(new String[]{REDIS_CHANNEL}, new WarpMessageHandler());

        CategoryGui.reload();
        GuiManager.registerGuiType("categories", CategoryGui.class);
        WarpsGui.reload();
        GuiManager.registerGuiType("warps", WarpsGui.class);
        RateWarpGui.reload();
        EditWarpGui.reload();
        FavoritesGui.reload();
        GuiManager.registerGuiType("favorites", FavoritesGui.class);
        RecentsGui.reload();
        GuiManager.registerGuiType("recents", RecentsGui.class);
        MyWarpsGui.reload();
        GuiManager.registerGuiType("my-warps", MyWarpsGui.class);
        WhitelistGui.reload();
        BlacklistGui.reload();

        WarpPlaceholders.load();

        database = new MySQL();
        database.setup();

        HookManager.setupHooks();

        WorldManager.reload();
        CategoryManager.reload();
        SortingManager.reload();

        CommandManager.load();

        WarpManager.load();
        WarpQueue.start();

        getServer().getPluginManager().registerEvents(new WorldListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerListeners(), this);
        getServer().getPluginManager().registerEvents(new MoveListener(), this);
        getServer().getPluginManager().registerEvents(new InputListener(), this);

        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#44f1d7[AxPlayerWarps] Loaded plugin! Using &f" + database.getType() + " &#44f1d7database to store data!"));

        if (CONFIG.getBoolean("update-notifier.enabled", true)) new UpdateNotifier(this, 6657);
    }

    public void disable() {
        SERVER_TRANSFER.disable();
        subscriber.close();
        PUBLISHER.close();
        database.disable();
        AsyncUtils.stop();
    }

    public void updateFlags() {
        FeatureFlags.USE_LEGACY_HEX_FORMATTER.set(true);
        FeatureFlags.ASYNC_UTILS_POOL_SIZE.set(3);
        FeatureFlags.ENABLE_PACKET_LISTENERS.set(true);
        FeatureFlags.PLACEHOLDER_API_HOOK.set(true);
        FeatureFlags.PLACEHOLDER_API_IDENTIFIER.set("axplayerwarps");
    }
}
