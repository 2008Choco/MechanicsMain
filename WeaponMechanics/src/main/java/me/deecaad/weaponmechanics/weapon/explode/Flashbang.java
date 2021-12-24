package me.deecaad.weaponmechanics.weapon.explode;

import me.deecaad.core.file.Serializer;
import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.StringUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.mechanics.CastData;
import me.deecaad.weaponmechanics.mechanics.Mechanics;
import me.deecaad.weaponmechanics.weapon.explode.exposures.ExplosionExposure;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import me.deecaad.weaponmechanics.weapon.projectile.ICustomProjectile;
import me.deecaad.weaponmechanics.wrappers.IEntityWrapper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.util.Collection;

import static me.deecaad.weaponmechanics.WeaponMechanics.debug;

public class Flashbang implements Serializer<Flashbang> {

    private double distance;
    private double distanceSquared;
    private Mechanics mechanics;

    public Flashbang() { }

    public Flashbang(double distance, Mechanics mechanics) {
        this.distance = distance;
        this.distanceSquared = distance * distance;
        this.mechanics = mechanics;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
        this.distanceSquared = distance * distance;
    }

    public Mechanics getMechanics() {
        return mechanics;
    }

    public void setMechanics(Mechanics mechanics) {
        this.mechanics = mechanics;
    }

    /**
     * Triggers this flashbang at this location, effecting all living entities
     * in the radius <code>distance</code>
     *
     * @param exposure The exposure type used
     * @param projectile The projectile used
     * @param origin The center of the flashbang
     */
    public void trigger(ExplosionExposure exposure, WeaponProjectile projectile, Location origin) {
        Collection<Entity> entities = origin.getWorld().getNearbyEntities(origin, distance, distance, distance);
        for (Entity entity : entities) {
            if (entity.getType() != EntityType.PLAYER) {
                continue;
            }
            LivingEntity livingEntity = (LivingEntity) entity;
            if (canEffect(exposure, origin, livingEntity)) {
                effect(projectile, livingEntity);
            }
        }
    }

    public boolean canEffect(ExplosionExposure exposure, Location origin, LivingEntity entity) {

        // Check to make sure the entity is in the same world
        // of the flashbang (This check is needed for the distance check)
        if (origin.getWorld() != entity.getWorld()) {
            return false;
        }

        Location eye = entity.getEyeLocation();
        double distanceSquared = origin.distanceSquared(eye);

        // Check to make sure the entity is within the flashbang's radius
        if (this.distanceSquared < distanceSquared) {
            return false;
        }

        // Check if the explosion exposure can effect the entity
        return exposure.canSee(origin, entity);
    }

    public void effect(WeaponProjectile projectile, LivingEntity entity) {
        if (mechanics != null) {
            IEntityWrapper wrapper = WeaponMechanics.getEntityWrapper(entity);
            mechanics.use(new CastData(wrapper, projectile.getWeaponTitle(), projectile.getWeaponStack()));
        }
    }

    @Override
    public String getKeyword() {
        return "Flashbang";
    }

    @Override
    public Flashbang serialize(File file, ConfigurationSection configurationSection, String path) {

        double distance = configurationSection.getDouble(path + ".Effect_Distance", -1);
        if (distance == -1) {
            return null;
        }

        Mechanics mechanics = new Mechanics().serialize(file, configurationSection, path + ".Mechanics");

        debug.validate(distance > 0.0, "Flashbang Effect_Distance should be a positive number! Found: " + distance,
                StringUtil.foundAt(file, path + ".Distance"));
        debug.validate(LogLevel.WARN, distance < 100.0, StringUtil.foundLarge(distance, file, path + ".Effect_Distance"));

        // Since the flashbang depends on mechanics for applying blindness to effected
        // entities, not specifying the mechanics is always a mistake
        if (mechanics == null) {
            debug.error("Flashbang MUST use Mechanics in order to make people blind. You forgot to add Mechanics!",
                    StringUtil.foundAt(file, path + ".Mechanics"));
            return null;
        }

        return new Flashbang(distance, mechanics);
    }
}