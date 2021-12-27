package me.deecaad.core.compatibility.entity;

public enum EntityMeta {

    FIRE,
    SNEAKING,
    MOUNTED,
    SPRINTING,
    SWIMMING,
    INVISIBLE,
    GLOWING,
    GLIDING;

    private final byte mask;
    private final int  index;

    EntityMeta() {
        this.index = ordinal();
        this.mask = (byte) (1 << index);
    }

    public int getIndex() {
        return index;
    }

    public byte getMask() {
        return mask;
    }
}
