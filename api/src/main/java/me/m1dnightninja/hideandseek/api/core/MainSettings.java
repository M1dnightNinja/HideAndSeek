package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;

public class MainSettings {

    private boolean enableAntiCheat = true;
    private MIdentifier preferredClassesItem = MIdentifier.create("minecraft", "diamond");

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

        out.set("enable_anti_cheat", enableAntiCheat);
        out.set("preferred_classes_item", preferredClassesItem);

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

    public MIdentifier getPreferredClassesItem() {
        return preferredClassesItem;
    }

    public void setPreferredClassesItem(MIdentifier preferredClassesItem) {
        this.preferredClassesItem = preferredClassesItem;
    }

    public void fromConfig(ConfigSection section) {

        if(section == null) return;

        if(section.has("enable_anti_cheat")) {
            enableAntiCheat = section.getBoolean("enable_anti_cheat");
        }

        if(section.has("preferred_classes_item", MIdentifier.class)) {
            preferredClassesItem = section.get("preferred_classes_item", MIdentifier.class);
        }

    }

}
