package com.anderb.breskulorm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class EntityKey {
    private final Serializable identifier;
    private final EntityMetadata metadata;

    public static EntityKey of(Serializable id, EntityMetadata metadata) {
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
