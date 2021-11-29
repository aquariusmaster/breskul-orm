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

@RequiredArgsConstructor
public class Session {
    private final DataSource dataSource;

    public <T> T find(Class<T> type, Object id) {
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
            PreparedStatement stm = connection.prepareStatement(String.format("SELECT * FROM %s WHERE %s=?", tableName, idFieldName));
            stm.setObject(1, id);
            ResultSet resultSet = stm.executeQuery();
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
        } catch (Exception e) {
            throw new RuntimeException("Error", e);
        }
    }
}
