package me.m1dnightninja.hideandseek.api.integration;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.AbstractGameInstance;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightitems.api.MidnightItemsAPI;
import me.m1dnightninja.midnightitems.api.item.MidnightItem;
import me.m1dnightninja.midnightitems.api.requirement.ItemRequirementType;

public class MidnightItemsIntegration {

    public static void init() {

        ItemRequirementType.register("hideandseek:role", (player, stack, item, data) -> {

            if(data == null) return false;

            AbstractSession inst = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
            return inst instanceof AbstractGameInstance && ((AbstractGameInstance) inst).getPosition(player) == PositionType.getById(data);
        });

        ItemRequirementType.register("hideandseek:seeker", (player, stack, item, data) -> {

            AbstractSession inst = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
            return inst instanceof AbstractGameInstance && ((AbstractGameInstance) inst).getPosition(player).isSeeker();
        });

    }

    public static MItemStack getItem(MIdentifier mid) {

        MidnightItem it = MidnightItemsAPI.getInstance().getItemRegistry().get(mid);
        if(it == null) return null;

        return it.getItemStack();
    }

}
