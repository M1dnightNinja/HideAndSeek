package me.m1dnightninja.hideandseek.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import me.m1dnightninja.hideandseek.fabric.command.LaunchEntityCommand;
import me.m1dnightninja.hideandseek.fabric.command.MainCommand;
import me.m1dnightninja.hideandseek.fabric.gamemode.ClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.manager.DimensionManager;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.fabric.Logger;
import me.m1dnightninja.midnightcore.fabric.api.JsonConfiguration;
import me.m1dnightninja.midnightcore.fabric.api.event.EntityDamageEvent;
import me.m1dnightninja.midnightcore.fabric.api.event.PlayerDisconnectEvent;
import me.m1dnightninja.midnightcore.fabric.api.event.PlayerFoodLevelChangeEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

public class HideAndSeek implements ModInitializer {

    private static HideAndSeek instance;

    private HideAndSeekAPI api;
    private DimensionManager dimensionManager;

    private File configFolder;

    @Override
    public void onInitialize() {

        instance = this;
        ILogger logger = new Logger(LogManager.getLogger());

        configFolder = Paths.get("config/HideAndSeek").toFile();
        if(!configFolder.exists() && !(configFolder.mkdirs() && configFolder.setReadable(true) && configFolder.setWritable(true))) {
            logger.warn("Unable to create config folder!");
            return;
        }

        dimensionManager = new DimensionManager();

        api = new HideAndSeekAPI(logger, new HideAndSeekRegistry());

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

        CommandRegistrationCallback.EVENT.register((commandDispatcher, b) -> {
            new MainCommand().register(commandDispatcher);
            new LaunchEntityCommand().register(commandDispatcher);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> api.getSessionManager().shutdownAll());

    }

    public HideAndSeekAPI getAPI() {
        return api;
    }

    public DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    private void loadGameModes() {
        api.getRegistry().registerGameType("classic", ClassicGameMode::new);
    }

    private void loadConfig() {

        File skinConfig = new File(configFolder, "skins.json");
        File classesConfig = new File(configFolder, "classes.json");
        File lobbyConfig = new File(configFolder, "lobbies.json");

        File mapsFolder = new File(configFolder, "maps");

        if(skinConfig.exists() && !skinConfig.isDirectory()) {
            try {
                JsonConfiguration conf = JsonConfiguration.loadFromFile(skinConfig);

                if(conf.getRoot().has("skins") && conf.getRoot().get("skins").isJsonArray()) {
                    for(JsonElement ele : conf.getRoot().getAsJsonArray("skins")) {
                        try {
                            if(!ele.isJsonObject()) continue;
                            api.getRegistry().registerSkin(ParseUtil.parseSkinOption(ele.getAsJsonObject()));
                        } catch(IllegalStateException | NullPointerException ex) {
                            HideAndSeekAPI.getLogger().warn("An exception occurred while trying to parse a skin!");
                            ex.printStackTrace();
                        }
                    }
                }

            } catch(JsonParseException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to read skin configuration!");
            }
        }

        if(classesConfig.exists() && !classesConfig.isDirectory()) {
            try {
                JsonConfiguration conf = JsonConfiguration.loadFromFile(classesConfig);

                if(conf.getRoot().has("classes") && conf.getRoot().get("classes").isJsonArray()) {
                    for(JsonElement ele : conf.getRoot().getAsJsonArray("classes")) {
                        try {
                            if(!ele.isJsonObject()) continue;
                            api.getRegistry().registerClass(GameClass.parse(ele.getAsJsonObject()));
                        } catch(IllegalStateException | NullPointerException ex) {
                            HideAndSeekAPI.getLogger().warn("An exception occurred while trying to parse a class!");
                            ex.printStackTrace();
                        }
                    }
                }

            } catch(JsonParseException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to read class configuration!");
            }
        }

        if(mapsFolder.exists() && mapsFolder.isDirectory()) {

            for(File f : mapsFolder.listFiles()) {
                if(!f.isDirectory()) continue;

                File config = new File(f, "map.json");
                File world = new File(f, "world");

                if(!config.exists() || config.isDirectory()) continue;
                if(!world.exists() || !world.isDirectory()) continue;

                JsonConfiguration conf;
                try {
                    conf = JsonConfiguration.loadFromFile(config);
                } catch(JsonParseException ex) {
                    HideAndSeekAPI.getLogger().warn("An exception occurred while trying to parse map " + f.getName());
                    ex.printStackTrace();
                    continue;
                }

                if(conf.getRoot() == null) {
                    continue;
                }

                api.getRegistry().registerMap(Map.parse(f.getName(), conf.getRoot(), f));

            }
        }

        if(lobbyConfig.exists() && !lobbyConfig.isDirectory()) {

            try {
                JsonConfiguration conf = JsonConfiguration.loadFromFile(lobbyConfig);

                if(conf.getRoot().has("lobbies") && conf.getRoot().get("lobbies").isJsonArray()) {
                    for(JsonElement ele : conf.getRoot().getAsJsonArray("lobbies")) {
                        try {
                            if(!ele.isJsonObject()) continue;

                            Lobby l = Lobby.parse(ele.getAsJsonObject());
                            if(l.getId().startsWith("world_") || l.getId().equals("world") || !l.getId().matches("[a-z0-9_.-]+")) {
                                HideAndSeekAPI.getLogger().warn("Unable to register lobby " + l.getId() + "! Invalid ID!");
                                continue;
                            }

                            api.getRegistry().registerLobby(l);
                        } catch(IllegalStateException | NullPointerException ex) {
                            HideAndSeekAPI.getLogger().warn("An exception occurred while trying to parse a lobby!");
                            ex.printStackTrace();
                        }
                    }
                }

            } catch(JsonParseException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to read lobby configuration!");
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

        loadGameModes();
        loadConfig();
    }

    public static HideAndSeek getInstance() {
        return instance;
    }

}
