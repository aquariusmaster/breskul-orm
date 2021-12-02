package com.anderb.breskulorm;

import lombok.Data;

@Data
public class EntityKey {
    private final Object identifier;
    private final EntityMetadata metadata;

    public static EntityKey of(Object id, EntityMetadata metadata) {
        return new EntityKey(id, metadata);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + identifier +
                ", type=" + metadata.getType() +
                '}';
    }
}
