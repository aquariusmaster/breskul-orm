package com.anderb.customormsession;

import com.anderb.customormsession.annotation.Column;
import com.anderb.customormsession.annotation.Id;
import com.anderb.customormsession.annotation.Table;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.anderb.customormsession.EntityKey.of;

@RequiredArgsConstructor
public class Session {
    private static final String FIND_SQL = "SELECT * FROM %s WHERE %s=?";

    private final DataSource dataSource;

    private final Map<EntityKey, Object> cache = new HashMap<>();

    public <T> T find(Class<T> type, Object id) {
        Object entity = cache.computeIfAbsent(of(id, type), this::loadFromDB);
        return type.cast(entity);
    }

    private <T> T loadFromDB(EntityKey key) {
        Class<T> type = (Class<T>) key.getType();
        Map<String, Field> fields = getEntityFields(type);

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = prepareFindByIdStatement(connection, key, fields);
            ResultSet resultSet = stm.executeQuery();
            return mapToEntity(resultSet, fields, type);
        } catch (Exception e) {
            throw new RuntimeException("Error", e);
        }
    }

    private <T> PreparedStatement prepareFindByIdStatement(Connection connection, EntityKey key, Map<String, Field> fields) throws SQLException {
        PreparedStatement stm = connection.prepareStatement(
                String.format(
                        FIND_SQL,
                        key.getType().getAnnotation(Table.class).value(),
                        getIdColumnName(fields)
                )
        );
        stm.setObject(1, key.getId());
        return stm;
    }

    private Map<String, Field> getEntityFields(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                Id id = field.getAnnotation(Id.class);
                if (id.value().isEmpty()) {
                    fields.put(field.getName(), field);
                } else {
                    fields.put(id.value(), field);
                }
            } else
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                if (column.value().isEmpty()) {
                    fields.put(field.getName(), field);
                } else {
                    fields.put(column.value(), field);
                }
            }
        }
        return fields;
    }

    private String getIdColumnName(Map<String, Field> fields) {
        return fields.entrySet().stream()
                .filter(entry -> entry.getValue().isAnnotationPresent(Id.class))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot find @Id annotation for entity"));
    }

    private <T> T mapToEntity(ResultSet resultSet, Map<String, Field> fields, Class<T> type) throws Exception {
        if (!resultSet.next()) {
            return null;
        }
        T instance = type.getDeclaredConstructor().newInstance();
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            Field field = entry.getValue();
            field.setAccessible(true);
            field.set(instance, resultSet.getObject(entry.getKey()));
        }
        return instance;
    }
}
