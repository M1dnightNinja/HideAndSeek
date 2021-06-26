package me.m1dnightninja.hideandseek.spigot;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.common.util.LangUtil;
import me.m1dnightninja.hideandseek.spigot.command.MainCommand;
import me.m1dnightninja.hideandseek.spigot.game.GameClass;
import me.m1dnightninja.hideandseek.spigot.game.MainListener;
import me.m1dnightninja.hideandseek.spigot.gamemode.ClassicGameMode;
import me.m1dnightninja.hideandseek.spigot.manager.DimensionManager;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.common.JavaLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class HideAndSeek extends JavaPlugin {

    private static HideAndSeek instance;

    private BukkitTask task;

    @Override
    public void onEnable() {

        MidnightCoreAPI.getConfigRegistry().registerSerializer(AbstractClass.class, GameClass.SERIALIZER);
        ConfigProvider configProvider = MidnightCoreAPI.getInstance().getDefaultConfigProvider();

        ConfigSection langDefaults = configProvider.loadFromStream(getResource("en_us.yml"));
        ConfigSection configDefaults = configProvider.loadFromStream(getResource("config.yml"));

        ILogger logger = new JavaLogger(getLogger());
        if(!MidnightCoreAPI.getInstance().areAllModulesLoaded("midnightcore:skin", "midnightcore:lang", "midnightcore:save_point", "midnightcore:player_data")) {

            logger.warn("One or more required MidnightCore modules are not loaded!");
            return;
        }

        MidnightCoreAPI.getConfigRegistry().registerSerializer(AbstractClass.class, GameClass.SERIALIZER);
        LangUtil.registerPlaceholders(MidnightCoreAPI.getInstance().getModule(ILangModule.class));

        HideAndSeekAPI api = new HideAndSeekAPI(logger, getDataFolder(), configDefaults, langDefaults, new DimensionManager());

        loadGameModes();

        api.reload();

        PluginCommand cmd = getCommand("has");
        if(cmd == null) {
            throw new IllegalStateException("plugin.yml is corrupted!");
        }

        MainCommand exe = new MainCommand();
        cmd.setExecutor(exe);
        cmd.setTabCompleter(exe);

        Bukkit.getPluginManager().registerEvents(new MainListener(), this);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                api.getSessionManager().tick();
            }
        }.runTaskTimer(this, 0L, 1L);

    }

    @Override
    public void onDisable() {
        HideAndSeekAPI.getInstance().getSessionManager().shutdownAll();
        task.cancel();
    }

    public long reload() {
        loadGameModes();
        return HideAndSeekAPI.getInstance().reload();
    }

    private void loadGameModes() {

        HideAndSeekAPI.getLogger().warn("registering gamemodes");

        HideAndSeekAPI.getInstance().getRegistry().registerGameType(new GameType("classic", HideAndSeekAPI.getInstance().getLangProvider().getMessage("gamemode.classic", (MPlayer) null)) {
            @Override
            public AbstractGameInstance create(AbstractLobbySession lobby, MPlayer player, Map map) {
                return new ClassicGameMode(lobby, player, map);
            }
        });
    }

    public static HideAndSeek getInstance() {
        return instance;
    }
}
