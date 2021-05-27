package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.hideandseek.api.core.HideAndSeekRegistry;
import me.m1dnightninja.hideandseek.api.core.MainSettings;
import me.m1dnightninja.hideandseek.api.core.SessionManager;
import me.m1dnightninja.hideandseek.api.game.Region;
import me.m1dnightninja.hideandseek.api.integration.MidnightItemsIntegration;
import me.m1dnightninja.hideandseek.api.integration.MidnightMenusIntegration;
import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;

import java.util.Random;

public class HideAndSeekAPI {

    private static HideAndSeekAPI INSTANCE;
    private static ILogger LOGGER;

    private final Random random;

    private final HideAndSeekRegistry registry;
    private final SessionManager sessionManager;

    private final ILangProvider langProvider;

    private MainSettings mainSettings;

    public HideAndSeekAPI(ILogger logger, ILangProvider langProvider) {

        MidnightCoreAPI.getConfigRegistry().registerSerializer(Region.class, Region.SERIALIZER);

        if(INSTANCE == null) {
            INSTANCE = this;
            LOGGER = logger;
        }

        this.registry = new HideAndSeekRegistry();
        this.sessionManager = new SessionManager();
        this.random = new Random();
        this.langProvider = langProvider;

        this.mainSettings = new MainSettings();

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

    }

    public ILangProvider getLangProvider() {
        return langProvider;
    }

    public MainSettings getMainSettings() {
        return mainSettings;
    }

    public void resetMainSettings() {
        this.mainSettings = new MainSettings();
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
