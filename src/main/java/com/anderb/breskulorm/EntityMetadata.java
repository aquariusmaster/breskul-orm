package com.anderb.breskulorm;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

@Builder
@Data
public class EntityMetadata {
    private final Class<?> type;
    private final String idColumnName;
    private final String tableName;
    private final LinkedHashMap<String, Field> fields;
    private final String updateSetValue;
}
