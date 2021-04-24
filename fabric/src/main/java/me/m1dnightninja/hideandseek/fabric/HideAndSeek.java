package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.fabric.command.MainCommand;
import me.m1dnightninja.hideandseek.fabric.game.*;
import me.m1dnightninja.hideandseek.fabric.gamemode.ClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.manager.DimensionManager;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.fabric.util.LangUtil;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.lang.PlaceholderSupplier;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.common.config.JsonConfigProvider;
import me.m1dnightninja.midnightcore.common.config.JsonWrapper;
import me.m1dnightninja.midnightcore.fabric.Logger;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.MidnightCoreModInitializer;
import me.m1dnightninja.midnightcore.fabric.api.event.*;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangModule;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangProvider;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class HideAndSeek implements MidnightCoreModInitializer {

    private static HideAndSeek instance;

    private HideAndSeekAPI api;
    private DimensionManager dimensionManager;

    private File configFolder;

    private LangProvider langProvider;
    private ConfigProvider configProvider;

    private ILogger logger;
    private File langFolder;

    @Override
    public void onInitialize() {

        instance = this;
        logger = new Logger(LogManager.getLogger());

        configFolder = Paths.get("config/HideAndSeek").toFile();
        if(!configFolder.exists() && !(configFolder.mkdirs() && configFolder.setReadable(true) && configFolder.setWritable(true))) {
            logger.warn("Unable to create config folder!");
            return;
        }

        this.configProvider = new JsonConfigProvider();

        langFolder = new File(configFolder, "lang");
        if(!langFolder.exists() || !langFolder.isDirectory()) {

            if(langFolder.exists() && !FileUtils.deleteQuietly(langFolder)) {
                logger.warn("Unable to delete conflicting lang file!");
                return;
            }

            if(!langFolder.mkdir()) {
                logger.warn("Unable to create lang folder!");
            }
        }
    }

    @Override
    public void onAPICreated(MidnightCore midnightCore, MidnightCoreAPI midnightCoreAPI) {

        if(!langFolder.exists()) return;

        if(langFolder.listFiles().length == 0) {

            File f = new File(langFolder, "en_us.json");
            new JsonWrapper(f).save();
        }

        ConfigSection sec = configProvider.loadFromStream(getClass().getResourceAsStream("/assets/hideandseek/lang/en_us.json"));
        if(!MidnightCoreAPI.getInstance().areAllModulesLoaded("midnightcore:skin", "midnightcore:dimension", "midnightcore:lang", "midnightcore:save_point", "midnightcore:player_data")) {

            logger.warn("One or more required MidnightCore modules are not loaded!");
            return;
        }

        loadLang(MidnightCoreAPI.getInstance().getModule(LangModule.class), langFolder, sec);

        dimensionManager = new DimensionManager();

        api = new HideAndSeekAPI(logger, langProvider);

        loadGameModes();
        loadConfig();

        Event.register(PlayerDisconnectEvent.class, this, event -> {
            AbstractSession sess = api.getSessionManager().getSession(event.getPlayer().getUUID());
            if(sess != null) sess.removePlayer(event.getPlayer().getUUID());
        });

        Event.register(EntityDamageEvent.class, this, event -> {
            if(!(event.getEntity() instanceof ServerPlayer)) return;

            UUID damager = null;
            if(event.getSource().getEntity() instanceof ServerPlayer) {
                damager = event.getSource().getEntity().getUUID();
            }

            event.setCancelled(api.getSessionManager().onDamaged(event.getEntity().getUUID(), damager, ConversionUtil.convertDamageSource(event.getSource()), event.getAmount()));
        });

        Event.register(PlayerFoodLevelChangeEvent.class, this, event -> {
            if(event.getNewFoodLevel() < event.getPreviousFoodLevel() && api.getSessionManager().getSession(event.getPlayer().getUUID()) != null) {
                event.setCancelled(true);
            }
        });

        Event.register(ServerTickEvent.class, this, event -> api.getSessionManager().tick());
        Event.register(PlayerJoinEvent.class, this, event -> HideAndSeekAPI.getInstance().getRegistry().loadData(event.getPlayer().getUUID()));

        CommandRegistrationCallback.EVENT.register((commandDispatcher, b) -> new MainCommand().register(commandDispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> api.getSessionManager().shutdownAll());
    }


    public DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    public LangProvider getLangProvider() {
        return langProvider;
    }

    private void loadGameModes() {
        api.getRegistry().registerGameType(new GameType("classic", langProvider.getMessage("gamemode.classic", (UUID) null)) {
            @Override
            public AbstractGameInstance create(AbstractLobbySession lobby, UUID player, AbstractMap map) {
                return new ClassicGameMode(lobby, player, map);
            }
        });
    }

    private void loadConfig() {

        File mainConfig = new File(configFolder, "config.json");

        File skinConfig = new File(configFolder, "skins.json");
        File classesConfig = new File(configFolder, "classes.json");
        File lobbyConfig = new File(configFolder, "lobbies.json");

        File mapsFolder = new File(configFolder, "maps");

        if(mainConfig.exists() && !mainConfig.isDirectory()) {

            ConfigSection sec = configProvider.loadFromFile(mainConfig);
            api.getMainSettings().fromConfig(sec);

        } else {
            if(mainConfig.exists() && !FileUtils.deleteQuietly(mainConfig)) {
                HideAndSeekAPI.getLogger().warn("Unable to delete conflicting main config file!");
            } else {

                configProvider.saveToFile(api.getMainSettings().toConfig(), mainConfig);
            }
        }

        if(skinConfig.exists() && !skinConfig.isDirectory()) {

            ConfigSection sec = configProvider.loadFromFile(skinConfig);
            if(sec.has("skins", List.class)) {
                for(Object o : sec.get("skins", List.class)) {
                    if(!(o instanceof ConfigSection)) continue;
                    api.getRegistry().registerSkin(SkinOption.parse((ConfigSection) o));
                }
            }
        }

        if(classesConfig.exists() && !classesConfig.isDirectory()) {

            ConfigSection sec = configProvider.loadFromFile(classesConfig);
            if(sec.has("classes", List.class)) {
                for(Object o : sec.get("classes", List.class)) {
                    if(!(o instanceof ConfigSection)) continue;

                    api.getRegistry().registerClass(GameClass.parse((ConfigSection) o));
                }
            }
        }

        if(mapsFolder.exists() && mapsFolder.isDirectory()) {

            for(File f : mapsFolder.listFiles()) {
                if(!f.isDirectory()) continue;

                File config = new File(f, "map.json");
                File world = new File(f, "world");

                if(!config.exists() || config.isDirectory()) continue;
                if(!world.exists() || !world.isDirectory()) continue;

                ConfigSection sec = configProvider.loadFromFile(config);
                api.getRegistry().registerMap(Map.parse(sec, f.getName(), f));

            }
        }

        if(lobbyConfig.exists() && !lobbyConfig.isDirectory()) {

            ConfigSection sec = configProvider.loadFromFile(lobbyConfig);
            if(sec.has("lobbies", List.class)) {
                for(Object o : sec.get("lobbies", List.class)) {
                    if(!(o instanceof ConfigSection)) continue;

                    Lobby l = Lobby.parse((ConfigSection) o);
                    if(l.getId().startsWith("world_") || l.getId().equals("world") || !l.getId().matches("[a-z0-9_.-]+")) {
                        HideAndSeekAPI.getLogger().warn("Unable to register lobby " + l.getId() + "! Invalid ID!");
                        continue;
                    }

                    api.getRegistry().registerLobby(l);
                }
            }
        }

        for(AbstractClass clazz : HideAndSeekAPI.getInstance().getRegistry().getClasses()) {
            clazz.updateEquivalencies();
        }

        int skins = HideAndSeekAPI.getInstance().getRegistry().getSkins().size();
        int classes = HideAndSeekAPI.getInstance().getRegistry().getClasses().size();
        int maps = HideAndSeekAPI.getInstance().getRegistry().getMaps().size();
        int lobbies = HideAndSeekAPI.getInstance().getRegistry().getLobbies().size();

        HideAndSeekAPI.getLogger().info("Loaded " + skins + (skins == 1 ? " skin" : " skins"));
        HideAndSeekAPI.getLogger().info("Loaded " + classes + (classes == 1 ? " class" : " classes"));
        HideAndSeekAPI.getLogger().info("Loaded " + maps + (maps == 1 ? " map" : " maps"));
        HideAndSeekAPI.getLogger().info("Loaded " + lobbies + (lobbies == 1 ? " lobby" : " lobbies"));

    }

    public void reload() {
        api.getSessionManager().shutdownAll();
        api.getRegistry().clear();

        api.resetMainSettings();

        langProvider.reloadAllEntries();

        loadGameModes();
        loadConfig();
    }


    private void loadLang(LangModule module, File langFolder, ConfigSection defaults) {

        LangUtil.registerPlaceholders(module);

        module.registerInlinePlaceholderSupplier("hideandseek_region_id", PlaceholderSupplier.create(Region.class, Region::getId));
        module.registerPlaceholderSupplier("hideandseek_region_name", PlaceholderSupplier.create(Region.class, reg -> MComponent.Serializer.parse(reg.getDisplay())));

        langProvider = module.createLangProvider(langFolder, configProvider, defaults);
    }

    public static HideAndSeek getInstance() {
        return instance;
    }

}
