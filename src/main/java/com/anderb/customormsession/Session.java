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
import java.util.HashMap;
import java.util.Map;

import static com.anderb.customormsession.EntityKey.of;

@RequiredArgsConstructor
public class Session {
    private static final String FIND_SQL = "SELECT * FROM %s WHERE %s=?";

    private final DataSource dataSource;

    private final Map<EntityKey, Object> cache = new HashMap<>();

    public <T> T find(Class<T> type, Object id) {
        return type.cast(cache.computeIfAbsent(of(id, type), this::loadFromDB));
    }

    private  <T> T loadFromDB(EntityKey key) {
        Class<?> type = key.getType();
        Table table = type.getAnnotation(Table.class);
        String tableName = table.value();
        Map<String, Field> fields = new HashMap<>();
        String idFieldName = null;
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                fields.put(field.getName(), field);
                idFieldName = field.getName();
            }
            if (field.isAnnotationPresent(Column.class)) {
                Column columnA = field.getAnnotation(Column.class);
                fields.put(columnA.name(), field);
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = connection.prepareStatement(String.format(FIND_SQL, tableName, idFieldName));
            stm.setObject(1, key.getId());
            ResultSet resultSet = stm.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            T instance = (T) type.getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                Field field = entry.getValue();
                field.setAccessible(true);
                field.set(instance, resultSet.getObject(entry.getKey()));
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error", e);
        }
    }
}
