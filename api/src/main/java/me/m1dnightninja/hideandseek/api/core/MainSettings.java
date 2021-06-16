package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;

public class MainSettings {

    private boolean enableAntiCheat = true;
    private final FileConfig fileConfig;
    private final ConfigSection defaults;

    public MainSettings(FileConfig config, ConfigSection configDefaults) {

        this.fileConfig = config;
        this.defaults = configDefaults;

        fileConfig.getRoot().fill(defaults);
        fileConfig.save();

        fromConfig(config.getRoot());
    }

    public void save() {

        ConfigSection out = fileConfig.getRoot();

        out.set("enable-anti-cheat", enableAntiCheat);

        fileConfig.save();
    }

    public void reload() {

        fileConfig.reload();
        fileConfig.getRoot().fill(defaults);
        fileConfig.save();

        fromConfig(fileConfig.getRoot());
    }


    public boolean isAntiCheatEnabled() {
        return enableAntiCheat;
    }

    public void setAntiCheatEnabled(boolean enableAntiCheat) {
        this.enableAntiCheat = enableAntiCheat;
    }

    public void fromConfig(ConfigSection section) {

        if(section == null) return;

        if(section.has("enable-anti-cheat")) {
            enableAntiCheat = section.getBoolean("enable-anti-cheat");
        }

    }

}
