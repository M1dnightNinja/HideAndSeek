package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;

public class MainSettings {

    private boolean enableAntiCheat = true;

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

    public ConfigSection toConfig() {

        ConfigSection out = new ConfigSection();

        out.set("enable-anti-cheat", enableAntiCheat);

        return out;
    }

}
