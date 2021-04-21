package me.m1dnightninja.hideandseek.api.integration;

import me.m1dnightninja.midnightcore.api.module.skin.Skin;
import me.m1dnightninja.skinsetter.api.SkinSetterAPI;

public class SkinSetterIntegration {

    public static Skin getSkin(String id) {

        SkinSetterAPI api = SkinSetterAPI.getInstance();
        if(api == null) return null;

        return api.getSkinRegistry().getSkin(id);
    }

}
