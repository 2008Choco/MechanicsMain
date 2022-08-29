package me.deecaad.weaponmechanics.weapon.weaponevents;

import me.deecaad.weaponmechanics.weapon.skin.SkinList;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Called whenever a {@link me.deecaad.weaponmechanics.weapon.skin.SkinHandler}
 * attempts to change the skin of a weapon. A skin may be changed when an
 * entity zooms in/out, reloads, sprints, runs out of ammo, etc.
 */
public class WeaponSkinEvent extends WeaponEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SkinList skinList;
    private String skin;

    private boolean cancel;

    public WeaponSkinEvent(String weaponTitle, ItemStack weaponStack, LivingEntity shooter, SkinList skinList) {
        super(weaponTitle, weaponStack, shooter);
        this.skinList = skinList;
        this.skin = "Default";
    }

    /**
     * Returns the config options for this skin. You can get this by using
     * <code>WeaponMechanics.getConfigurations().getObject(weaponTitle + ".Skin", SkinList.class)</code>
     *
     * @return The non-null skin list.
     */
    public SkinList getSkinList() {
        return skinList;
    }

    /**
     * If only WeaponMechanics is installed, this method will always return
     * <code>"Default"</code>. If WeaponMechanicsCosmetics is installed, this
     * method may return a different value (Which you can find in the config
     * for this weapon).
     *
     * @return The non-null name of the skin.
     */
    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        if (skin == null)
            skin = "Default";

        this.skin = skin;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
