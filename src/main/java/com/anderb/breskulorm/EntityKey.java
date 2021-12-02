package com.anderb.breskulorm;

import lombok.Data;

@Data
public class EntityKey {
    private final Object id;
    private final Class<?> type;

    public static EntityKey of(Object id, Class<?> type) {
        return new EntityKey(id, type);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
