package me.deecaad.core.mechanics.conditions;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityTypeCondition extends Condition {

    private EntityType type;

    /**
     * Default constructor for serializer.
     */
    public EntityTypeCondition() {
    }

    public EntityTypeCondition(EntityType type) {
        this.type = type;
    }

    @Override
    public boolean isAllowed0(CastData cast) {
        return cast.getTarget() != null && cast.getTarget().getType() == type;
    }

    @Override
    public String getKeyword() {
        return "EntityType";
    }

    @Override
    public @Nullable String getWikiLink() {
        return "https://github.com/WeaponMechanics/MechanicsMain/wiki/EntityTypeCondition";
    }

    @NotNull
    @Override
    public Condition serialize(SerializeData data) throws SerializerException {
        EntityType type = data.of("Entity").assertExists().getEnum(EntityType.class);
        return applyParentArgs(data, new EntityTypeCondition(type));
    }
}
