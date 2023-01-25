package me.deecaad.core.mechanics.targeters;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.file.serializers.VectorSerializer;
import me.deecaad.core.mechanics.CastData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A targeter returns a list of targets. A target can be a {@link org.bukkit.Location},
 * an {@link org.bukkit.entity.Entity}, or a {@link org.bukkit.entity.Player}.
 */
public abstract class Targeter implements InlineSerializer<Targeter> {

    private VectorSerializer offset;

    /**
     * Default constructor for serializers.
     */
    public Targeter() {
    }

    @Nullable
    public VectorSerializer getOffset() {
        return offset;
    }

    /**
     * Returns <code>true</code> if this targeter specifically targets an
     * entity. Entity targeters also target locations by default, but that
     * location will always be the entity's location.
     *
     * <p>There is 1 caveat, when {@link #getOffset()} is non-null, the
     * targeted location will be different from the targeted entity.
     *
     * @return true if this targeter targets entities, not specific locations.
     */
    public abstract boolean isEntity();

    /**
     * Public method to get every possible target for the mechanic using this
     * targeter. The returned targets will have the offset
     * ({@link #getOffset()}) already applied.
     *
     * @param cast The non-null origin of the cast.
     * @return The list of targets.
     */
    public final List<CastData> getTargets(CastData cast) {
        List<CastData> targets = getTargets0(cast);
        if (offset != null) {
            for (CastData target : targets)
                target.setTargetLocation(target.getTargetLocation().add(offset.getVector(target.getTarget())));
        }

        return targets;
    }

    protected abstract List<CastData> getTargets0(CastData cast);

    protected Targeter applyParentArgs(SerializeData data, Targeter targeter) throws SerializerException {
        VectorSerializer offset = data.of("Offset").serialize(VectorSerializer.class);
        if (!isEntity() && offset != null && offset.isRelative())
            throw data.exception("offset", "Did you try to use relative locations ('~') with '" + getInlineKeyword() + "'?",
                    getInlineKeyword() + " is a LOCATION targeter, so it cannot use relative locations.");

        targeter.offset = offset;
        return targeter;
    }
}
