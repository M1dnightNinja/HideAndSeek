package me.m1dnightninja.hideandseek.api.integration;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.AbstractGameInstance;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightitems.api.requirement.ItemRequirementType;
import me.m1dnightninja.midnightmenus.api.menu.MenuRequirementType;

public class MidnightMenusIntegration {

    public static void init() {

        MenuRequirementType.register("hideandseek:role", (player, data) -> {

            if(data == null) return false;

            AbstractSession inst = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
            return inst instanceof AbstractGameInstance && ((AbstractGameInstance) inst).getPosition(player) == PositionType.getById(data);
        });

        MenuRequirementType.register("hideandseek:seeker", (player, data) -> {

            AbstractSession inst = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
            return inst instanceof AbstractGameInstance && ((AbstractGameInstance) inst).getPosition(player).isSeeker();
        });

    }

}
