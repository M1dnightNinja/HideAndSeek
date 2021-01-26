package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.ILogger;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;

import java.util.Random;

public class HideAndSeekAPI {

    private static HideAndSeekAPI INSTANCE;
    private static ILogger LOGGER;

    private final Random random;

    private final HideAndSeekRegistry registry;
    private final SessionManager sessionManager;

    public HideAndSeekAPI(ILogger logger, HideAndSeekRegistry reg) {

        if(INSTANCE == null) {
            INSTANCE = this;
            LOGGER = logger;
        }

        this.registry = reg;
        this.sessionManager = new SessionManager();
        this.random = new Random();

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
