package me.deecaad.weaponmechanics.mechanics;

public interface IMechanic {

    /**
     * Use this mechanic with given cast data
     *
     * @param castData the cast data
     */
    void use(CastData castData);

    /**
     * @return whether this mechanic should only be ran if caster is player
     */
    default boolean requirePlayer() {
        return false;
    }

    /**
     * @return whether this mechanic should only be ran if CastData has entity specified
     */
    default boolean requireEntity() {
        return false;
    }
}