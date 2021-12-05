package com.anderb.breskulorm;

import com.anderb.breskulorm.annotation.Column;
import com.anderb.breskulorm.annotation.GenerationType;
import com.anderb.breskulorm.annotation.Id;
import com.anderb.breskulorm.annotation.Table;
import com.anderb.breskulorm.exception.OrmException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.anderb.breskulorm.annotation.GenerationType.IDENTITY;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class EntityMetadataResolver {

    private final Map<Class<?>, EntityMetadata> metadataMap;
    private final EntityPersister entityPersister = new EntityPersister();

    public EntityMetadataResolver(Class<?>... entityClasses) {
        metadataMap = Arrays.stream(entityClasses)
                .collect(toMap(identity(), this::generateEntityMetadata));
    }

    public EntityMetadata getEntityMetadata(Class<?> entityType) {
        if (metadataMap.containsKey(entityType)) {
            return metadataMap.get(entityType);
        }
        throw new OrmException("EntityMetadata does not exist for type " + entityType);
    }

    public EntityMetadata generateEntityMetadata(Class<?> entityClass) {
        var fields = getEntityFields(entityClass);
        String idColumnName = getIdColumnName(fields);
        Field idField = getIdField(fields, entityClass);
        GenerationType idGenerationType = getIdGenerationType(idField);
        return EntityMetadata
                .builder()
                .type(entityClass)
                .idColumnName(idColumnName)
                .tableName(getTableName(entityClass))
                .fields(fields)
                .idField(idField)
                .updateSetValueSql(getUpdateSetValue(fields, idColumnName))
                .insertSetValueSql(getInsertSetValue(fields, idColumnName, idGenerationType == IDENTITY))
                .persister(entityPersister)
                .idGenerationType(idGenerationType)
                .build();
    }

    private GenerationType getIdGenerationType(Field idField) {
        return idField.getAnnotation(Id.class).generatedValue();
    }

    private Field getIdField(LinkedHashMap<String, Field> fields, Class<?> type) {
        return fields
                .values()
                .stream()
                .filter(field -> field.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException(String.format("Entity %s has not @Id field annotation", type))
                );
    }

    private LinkedHashMap<String, Field> getEntityFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Column.class))
                .sorted(comparing(Field::getName))
                .collect(toMap(
                        this::getColumnName,
                        identity(),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate field name %s", u));
                        },
                        LinkedHashMap::new));
    }

    private String getIdColumnName(LinkedHashMap<String, Field> fields) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getValue().isAnnotationPresent(Id.class))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find @Id annotation for entity"));
    }

    private String getColumnName(Field field) {
        if (field.isAnnotationPresent(Id.class)) {
            Id id = field.getAnnotation(Id.class);
            if (id.value().isEmpty()) {
                return field.getName();
            }
            return id.value();
        }
        Column column = field.getAnnotation(Column.class);
        if (column.value().isEmpty()) {
            return field.getName();
        }
        return column.value();
    }

    private String getTableName(Class<?> type) {
        if (type.isAnnotationPresent(Table.class)) {
            return type.getAnnotation(Table.class).value();
        }
        throw new OrmException("Table annotation does not exists for entity: " + type);
    }

    private String getUpdateSetValue(LinkedHashMap<String, Field> fields, String idColumnName) {
        return fields.keySet()
                .stream()
                .filter(fieldName -> !fieldName.equals(idColumnName))
                .collect(joining("=?, ", "", "=?"));
    }

    private String getInsertSetValue(LinkedHashMap<String, Field> fields, String idColumnName, boolean excludeId) {
        var valuesWithoutId = fields.keySet()
                .stream()
                .filter(fieldName -> !fieldName.equals(idColumnName))
                .collect(joining(", "));
        if (excludeId) {
            return valuesWithoutId;
        }
        return valuesWithoutId + ", " + idColumnName;
    }

}
