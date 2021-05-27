package me.m1dnightninja.hideandseek.api.game;

public enum DamageSource {

    IN_FIRE("inFire"),
    LIGHTNING("lightningBolt"),
    FIRE("onFire"),
    LAVA("lava"),
    MAGMA("hotFloor"),
    SUFFOCATE("inWall"),
    CRAMMING("cramming"),
    DROWN("drown"),
    STARVE("starve"),
    CACTUS("cactus"),
    FALL("fall"),
    FLY_INTO_WALL("flyIntoWall"),
    VOID("outOfWorld"),
    GENERIC("generic"),
    MAGIC("magic"),
    WITHER("wither"),
    ANVIL("anvil"),
    FALLING_BLOCK("fallingBlock"),
    DRAGON_BREATH("dragonBreath"),
    DRY_OUT("dryout"),
    SWEET_BERRY_BUSH("sweetBerryBush");

    String translateName;

    DamageSource(String translateName) {
        this.translateName = translateName;
    }

    public String getTranslateName() {
        return translateName;
    }
}
