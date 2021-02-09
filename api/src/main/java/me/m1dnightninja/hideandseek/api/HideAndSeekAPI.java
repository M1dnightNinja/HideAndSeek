package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.lang.AbstractLangProvider;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;

import java.util.Random;

public class HideAndSeekAPI {

    private static HideAndSeekAPI INSTANCE;
    private static ILogger LOGGER;

    private final Random random;

    private final HideAndSeekRegistry registry;
    private final SessionManager sessionManager;

    private final AbstractLangProvider langProvider;

    private MainSettings mainSettings;

    public HideAndSeekAPI(ILogger logger, HideAndSeekRegistry reg, AbstractLangProvider langProvider) {

        if(INSTANCE == null) {
            INSTANCE = this;
            LOGGER = logger;
        }

        this.registry = reg;
        this.sessionManager = new SessionManager();
        this.random = new Random();
        this.langProvider = langProvider;

        this.mainSettings = new MainSettings();

    }

    public AbstractLangProvider getLangProvider() {
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
