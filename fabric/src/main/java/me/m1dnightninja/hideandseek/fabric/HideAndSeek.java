package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.common.util.LangUtil;
import me.m1dnightninja.hideandseek.fabric.command.MainCommand;
import me.m1dnightninja.hideandseek.fabric.game.GameClass;
import me.m1dnightninja.hideandseek.fabric.gamemode.ClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.manager.DimensionManager;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.Logger;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.MidnightCoreModInitializer;
import me.m1dnightninja.midnightcore.fabric.api.event.*;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Paths;

public class HideAndSeek implements MidnightCoreModInitializer {

    private static HideAndSeek instance;

    private HideAndSeekAPI api;

    private File configFolder;

    private ILogger logger;

    @Override
    public void onInitialize() {

        instance = this;
        logger = new Logger(LogManager.getLogger());

        configFolder = Paths.get("config/HideAndSeek").toFile();
        if(!configFolder.exists() && !configFolder.mkdirs()) {
            logger.warn("Unable to create config folder!");
        }


    }

    @Override
    public void onAPICreated(MidnightCore midnightCore, MidnightCoreAPI midnightCoreAPI) {

        ConfigProvider configProvider = MidnightCoreAPI.getInstance().getDefaultConfigProvider();

        ConfigSection langDefaults = configProvider.loadFromStream(getClass().getResourceAsStream("/assets/hideandseek/lang/en_us.json"));
        ConfigSection configDefaults = configProvider.loadFromStream(getClass().getResourceAsStream("/assets/hideandseek/config.json"));

        if(!MidnightCoreAPI.getInstance().areAllModulesLoaded("midnightcore:skin", "midnightcore:dimension", "midnightcore:lang", "midnightcore:save_point", "midnightcore:player_data")) {

            logger.warn("One or more required MidnightCore modules are not loaded!");
            return;
        }

        MidnightCoreAPI.getConfigRegistry().registerSerializer(AbstractClass.class, GameClass.SERIALIZER);
        LangUtil.registerPlaceholders(midnightCoreAPI.getModule(ILangModule.class));

        api = new HideAndSeekAPI(logger, configFolder, configDefaults, langDefaults, new DimensionManager());
        loadGameModes();

        api.reload();

        Event.register(PlayerDisconnectEvent.class, this, event -> {
            AbstractSession sess = api.getSessionManager().getSession(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(event.getPlayer().getUUID()));
            if(sess != null) sess.removePlayer(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(event.getPlayer().getUUID()));
        });

        Event.register(EntityDamageEvent.class, this, event -> {
            if(!(event.getEntity() instanceof ServerPlayer)) return;

            ServerPlayer damager = null;
            if(event.getSource().getEntity() instanceof ServerPlayer) {
                damager = (ServerPlayer) event.getSource().getEntity();
            }

            event.setCancelled(api.getSessionManager().onDamaged(FabricPlayer.wrap((ServerPlayer) event.getEntity()), damager == null ? null : FabricPlayer.wrap(damager), ConversionUtil.convertDamageSource(event.getSource()), event.getAmount()));
        });

        Event.register(PlayerFoodLevelChangeEvent.class, this, event -> {
            if(event.getNewFoodLevel() < event.getPreviousFoodLevel() && api.getSessionManager().getSession(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(event.getPlayer().getUUID())) != null) {
                event.setCancelled(true);
            }
        });

        Event.register(ServerTickEvent.class, this, event -> api.getSessionManager().tick());
        Event.register(PlayerJoinEvent.class, this, event -> HideAndSeekAPI.getInstance().getRegistry().loadData(event.getPlayer().getUUID()));

        CommandRegistrationCallback.EVENT.register((commandDispatcher, b) -> new MainCommand().register(commandDispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> api.getSessionManager().shutdownAll());
    }

    public long reload() {
        long time = api.reload();
        loadGameModes();
        return time;
    }

    private void loadGameModes() {
        api.getRegistry().registerGameType(new GameType("classic", HideAndSeekAPI.getInstance().getLangProvider().getMessage("gamemode.classic", (MPlayer) null)) {
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
