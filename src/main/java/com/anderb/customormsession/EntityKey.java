package com.anderb.customormsession;

import lombok.Data;

@Data
public class EntityKey {
    private final Object id;
    private final Class<?> type;

    public static EntityKey of(Object id, Class<?> type) {
        return new EntityKey(id, type);
    }
}
