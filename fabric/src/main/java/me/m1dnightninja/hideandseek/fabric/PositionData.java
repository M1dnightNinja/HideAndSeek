package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.ILangModule;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class PositionData extends AbstractPositionData {

    public static void registerPlaceholders(ILangModule<Component> mod) {

        mod.registerStringPlaceholder("hideandseek_position_type", mod.createSupplier(AbstractPositionData.class, obj -> obj.getType().getId()));
        mod.registerStringPlaceholder("hideandseek_position_color", mod.createSupplier(AbstractPositionData.class, obj -> obj.getColor().toHex()));

        mod.registerRawPlaceholder("hideandseek_position_name", mod.createSupplier(PositionData.class, PositionData::getRawName));
        mod.registerRawPlaceholder("hideandseek_position_name_plural", mod.createSupplier(PositionData.class, PositionData::getRawPluralName));
        mod.registerRawPlaceholder("hideandseek_position_name_proper", mod.createSupplier(PositionData.class, PositionData::getRawProperName));

        mod.registerStringPlaceholder("hideandseek_player_position_type", mod.createSupplier(ServerPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getType().getId();
        }));

        mod.registerStringPlaceholder("hideandseek_player_position_color", mod.createSupplier(ServerPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getColor().toHex();
        }));

        mod.registerRawPlaceholder("hideandseek_player_position_name", mod.createSupplier(ServerPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return ((PositionData) sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID()))).getRawName();
        }));

        mod.registerRawPlaceholder("hideandseek_player_position_name_proper", mod.createSupplier(ServerPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return ((PositionData) sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID()))).getRawProperName();
        }));

        mod.registerRawPlaceholder("hideandseek_player_position_name_plural", mod.createSupplier(ServerPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return ((PositionData) sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID()))).getRawPluralName();
        }));

    }

    private MutableComponent rawName;
    private MutableComponent rawPluralName;
    private MutableComponent rawProperName;

    public PositionData(PositionType type) {
        super(type);
    }

    public MutableComponent getRawName() {
        return rawName;
    }

    public MutableComponent getRawPluralName() {
        return rawPluralName;
    }

    public MutableComponent getRawProperName() {
        return rawProperName;
    }

    public static PositionData parse(ConfigSection obj, PositionType type, Map map) {

        PositionData out = new PositionData(type);
        out.fromConfig(obj, map);

        out.rawName = TextUtil.parse(out.getName());

        if(out.pluralName == null) {
            out.rawPluralName = out.rawName.copy().append("s");
            out.pluralName = Component.Serializer.toJson(out.rawPluralName);
        } else {
            out.rawPluralName = TextUtil.parse(out.getPluralName());
        }

        if(out.properName == null) {
            out.rawProperName = new TextComponent("The ").setStyle(out.rawName.getStyle()).append(out.rawName.copy());
            out.properName = Component.Serializer.toJson(out.rawProperName);
        } else {
            out.rawProperName = TextUtil.parse(out.getProperName());
        }

        return out;
    }

}
