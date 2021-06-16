package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.hideandseek.api.core.HideAndSeekRegistry;
import me.m1dnightninja.hideandseek.api.core.MainSettings;
import me.m1dnightninja.hideandseek.api.core.SessionManager;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.integration.MidnightItemsIntegration;
import me.m1dnightninja.hideandseek.api.integration.MidnightMenusIntegration;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigProvider;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.module.skin.Skin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;

public class HideAndSeekAPI {

    private static HideAndSeekAPI INSTANCE;
    private static ILogger LOGGER;

    private final Random random;

    private final HideAndSeekRegistry registry;
    private final SessionManager sessionManager;

    private final ILangProvider langProvider;

    private final MainSettings mainSettings;
    private final File dataFolder;

    private String resourcePack = "https://github.com/M1dnightNinja/HideAndSeek/blob/master/empty.zip?raw=true";
    private String resourcePackHash = "F8CC3481867628951AD312B9FB886223856F7AB0";

    private final AbstractDimensionManager<?> dimensionManager;

    public HideAndSeekAPI(ILogger logger, File dataFolder, ConfigSection configDefaults, ConfigSection langDefaults, AbstractDimensionManager<?> dimensionManager) {

        MidnightCoreAPI.getConfigRegistry().registerSerializer(Region.class, Region.SERIALIZER);

        if(INSTANCE == null) {
            INSTANCE = this;
            LOGGER = logger;
        }

        this.registry = new HideAndSeekRegistry();
        this.sessionManager = new SessionManager();
        this.random = new Random();
        this.dataFolder = dataFolder;
        this.dimensionManager = dimensionManager;

        File langFolder = new File(dataFolder, "lang");

        ConfigProvider prov = MidnightCoreAPI.getInstance().getDefaultConfigProvider();
        this.langProvider = MidnightCoreAPI.getInstance().getModule(ILangModule.class).createLangProvider(langFolder, prov, langDefaults);

        try {
            Class.forName("me.m1dnightninja.midnightitems.api.MidnightItemsAPI");
            MidnightItemsIntegration.init();
        } catch (ClassNotFoundException ex) {
            // Ignore
        }

        try {
            Class.forName("me.m1dnightninja.midnightmenus.api.MidnightMenusAPI");
            MidnightMenusIntegration.init();
        } catch (ClassNotFoundException ex) {
            // Ignore
        }

        FileConfig config = new FileConfig(new File(dataFolder, "config" + prov.getFileExtension()));
        this.mainSettings = new MainSettings(config, configDefaults);

        Properties properties = new Properties();
        File file = new File("server.properties");

        try {
            FileReader reader = new FileReader(file);

            properties.load(reader);

            String res = properties.getProperty("resource-pack");
            String sha = properties.getProperty("resource-pack-sha");

            if(res != null && !res.isEmpty()) {
                resourcePack = res;
                if(sha != null && !sha.isEmpty()) {
                    resourcePackHash = sha;
                } else {
                    resourcePackHash = "";
                }
            }
            reader.close();

        } catch (IOException ex) {

            ex.printStackTrace();
        }
    }

    public long reload() {

        long time = System.currentTimeMillis();

        sessionManager.shutdownAll();
        registry.clear();

        getMainSettings().reload();
        loadContent();

        return System.currentTimeMillis() - time;
    }

    private void loadContent() {

        ConfigProvider prov = MidnightCoreAPI.getInstance().getDefaultConfigProvider();

        Pattern valid = Pattern.compile("[a-z0-9_.-]+");

        File mapsFolder = new File(dataFolder, "maps");
        File skinsFile = new File(dataFolder, "skins" + prov.getFileExtension());
        File lobbiesFile = new File(dataFolder, "lobbies" + prov.getFileExtension());
        File classesFile = new File(dataFolder, "classes" + prov.getFileExtension());

        if(skinsFile.exists()) {

            ConfigSection root = prov.loadFromFile(skinsFile);
            if(root != null && root.has("skins", List.class)) {

                for(ConfigSection sec : root.getListFiltered("skins", ConfigSection.class)) {

                    if(!sec.has("id")) {
                        LOGGER.warn("Unable to parse a saved skin! Missing id!");
                        continue;
                    }

                    String id = sec.getString("id");

                    if(!valid.matcher(id).matches()) {
                        LOGGER.warn("Unable to register saved skin with id " + id + "! id does not match " + valid.pattern() + "!");
                        continue;
                    }

                    if(registry.getSkin(id) != null) {
                        LOGGER.warn("Unable to register saved skin with id " + id + "! id is already taken!");
                        continue;
                    }

                    try {
                        SavedSkin skin = new SavedSkin(id, Skin.SERIALIZER.deserialize(sec));
                        registry.registerSkin(skin);

                    } catch (IllegalStateException ex) {
                        LOGGER.warn("Unable to parse saved skin with id " + id + "! An error occurred!");
                        ex.printStackTrace();
                    }
                }
            }

            int amount = registry.getSkins().size();
            if(amount > 0) LOGGER.info("Registered " + (amount) + (amount == 1 ? " skin!" : " skins!"));
        }

        if(classesFile.exists()) {

            ConfigSection root = prov.loadFromFile(classesFile);
            if(root != null && root.has("classes", List.class)) {

                for(AbstractClass clazz : root.getListFiltered("classes", AbstractClass.class)) {

                    String id = clazz.getId();

                    if(!valid.matcher(id).matches()) {
                        LOGGER.warn("Unable to register class with id " + id + "! id does not match " + valid.pattern() + "!");
                        continue;
                    }

                    if(registry.getSkin(id) != null) {
                        LOGGER.warn("Unable to register class with id " + id + "! id is already taken!");
                        continue;
                    }

                    registry.registerClass(clazz);
                }
            }

            int amount = registry.getClasses().size();
            if(amount > 0) LOGGER.info("Registered " + (amount) + (amount == 1 ? " class!" : " classes!"));
        }

        if(mapsFolder.exists()) {

            File[] maps = mapsFolder.listFiles();
            if(maps != null) for(File mapDir : maps) {

                if(!mapDir.isDirectory()) continue;

                String mapId = mapDir.getName();
                if(!valid.matcher(mapId).matches()) {
                    LOGGER.warn("Unable to parse map with id " + mapId + "! id does not match " + valid.pattern() + "!");
                    continue;
                }

                if(registry.getMap(mapId) != null) {
                    LOGGER.warn("Unable to register map with id " + mapId + "! id is already taken!");
                    continue;
                }


                File config = new File(mapDir, "map" + prov.getFileExtension());
                File world = new File(mapDir, "world");

                if(!config.exists() || config.isDirectory()) {
                    LOGGER.warn("Unable to parse map with id " + mapId + "! Missing " + config.getName() + "!");
                    continue;
                }

                if(!world.exists() || !world.isDirectory()) {
                    LOGGER.warn("Unable to parse map with id " + mapId + "! Missing world folder!");
                    continue;
                }

                ConfigSection sec = prov.loadFromFile(config);

                try {
                    Map map = Map.parse(sec, mapId, mapDir);
                    registry.registerMap(map);

                } catch (IllegalStateException ex) {
                    LOGGER.warn("Unable to parse map with id " + mapId + "! An error occurred!");
                    ex.printStackTrace();
                }

            }

            int amount = registry.getMaps().size();
            if(amount > 0) LOGGER.info("Registered " + (amount) + (amount == 1 ? " map!" : " maps!"));
        }

        if(lobbiesFile.exists()) {

            ConfigSection root = prov.loadFromFile(lobbiesFile);

            if(root != null && root.has("lobbies", List.class)) {

                for(ConfigSection sec : root.getListFiltered("lobbies", ConfigSection.class)) {

                    if(!sec.has("id")) {
                        LOGGER.warn("Unable to parse a lobby! Missing id!");
                        continue;
                    }

                    String id = sec.getString("id");

                    if(!valid.matcher(id).matches()) {
                        LOGGER.warn("Unable to register lobby with id " + id + "! id does not match " + valid.pattern() + "!");
                        continue;
                    }

                    if(id.equals("world") || id.startsWith("world_")) {
                        LOGGER.warn("Unable to register lobby with id " + id + "! invalid id!");
                        continue;
                    }

                    if(registry.getSkin(id) != null) {
                        LOGGER.warn("Unable to register a lobby with id " + id + "! id is already taken!");
                        continue;
                    }

                    try {
                        Lobby lobby = Lobby.parse(sec);
                        registry.registerLobby(lobby);

                    } catch (IllegalStateException ex) {
                        LOGGER.warn("Unable to parse a lobby with id " + id + "! An error occurred!");
                        ex.printStackTrace();
                    }
                }
            }

            int amount = registry.getLobbies().size();
            if(amount > 0) LOGGER.info("Registered " + (amount) + (amount == 1 ? " lobby!" : " lobbies!"));
        }
    }

    public AbstractDimensionManager<?> getDimensionManager() {
        return dimensionManager;
    }

    public String getResourcePack() {
        return resourcePack;
    }

    public String getResourcePackHash() {
        return resourcePackHash;
    }

    public ILangProvider getLangProvider() {
        return langProvider;
    }

    public MainSettings getMainSettings() {
        return mainSettings;
    }

    public HideAndSeekRegistry getRegistry() {
        return registry;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public Random getRandom() {
        return random;
    }

    public static HideAndSeekAPI getInstance() {
        return INSTANCE;
    }

    public static ILogger getLogger() { return LOGGER; }
}
