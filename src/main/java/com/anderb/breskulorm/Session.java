package com.anderb.breskulorm;

import com.anderb.breskulorm.annotation.Column;
import com.anderb.breskulorm.annotation.Id;
import com.anderb.breskulorm.annotation.Table;
import com.anderb.breskulorm.exception.OrmException;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.anderb.breskulorm.EntityKey.of;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class Session implements AutoCloseable {
    private static final String FIND_SQL = "SELECT * FROM %s WHERE %s=?";
    private static final String UPDATE_SQL = "UPDATE %s SET %s WHERE %s=?";

    private final DataSource dataSource;
    private final Map<EntityKey, Object[]> snapshots = new HashMap<>();
    private final Map<EntityKey, Object> persistenceContext = new HashMap<>();
    private boolean isReadOnly;
    private boolean closed;

    public <T> T find(Class<T> type, Object id) {
        checkOpen();
        Object entity = persistenceContext.computeIfAbsent(of(id, type), this::loadFromDatasource);
        return type.cast(entity);
    }

    public void close() {
        closed = true;
        flush();
        persistenceContext.clear();
    }

    public void flush() {
        persistenceContext
                .entrySet()
                .stream()
                .filter(entry -> snapshots.containsKey(entry.getKey()))
                .forEach(entry -> {
                    var entityFields = getEntityFields(entry.getKey().getType());
                    var currentState = toSnapshot(entry.getValue(), entityFields);
                    if (isDirty(snapshots.get(entry.getKey()), currentState)) {
                        executeUpdate(entry.getKey(), currentState, entityFields);
                    }
                });
        snapshots.clear();
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public boolean isClosed() {
        return closed;
    }

    private void checkOpen() {
        if (isClosed()) {
            throw new IllegalStateException("Session is closed");
        }
    }

    private <T> T loadFromDatasource(EntityKey key) {
        Class<T> type = (Class<T>) key.getType();
        LinkedHashMap<String, Field> fields = getEntityFields(type);

        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = prepareFindByIdStatement(connection, key, fields);
            ResultSet resultSet = stm.executeQuery();
            T entity = mapToEntity(resultSet, fields, type);
            saveStateToSnapshotIfNeeded(key, entity, fields);
            return entity;
        } catch (Exception e) {
            throw new OrmException("Cannot load entity from DB", e);
        }
    }

    private <T> void saveStateToSnapshotIfNeeded(EntityKey key, T entity, LinkedHashMap<String, Field> fields) {
        if (!isReadOnly) {
            saveStateToSnapshot(key, entity, fields);
        }
    }

    private <T> PreparedStatement prepareFindByIdStatement(Connection connection, EntityKey key, LinkedHashMap<String, Field> fields) throws SQLException {
        PreparedStatement stm = connection.prepareStatement(
                String.format(
                        FIND_SQL,
                        getTableName(key.getType()),
                        getIdColumnName(fields)
                )
        );
        stm.setObject(1, key.getId());
        return stm;
    }

    private LinkedHashMap<String, Field> getEntityFields(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Column.class))
                .sorted(comparing(Field::getName))
                .collect(toMap(
                        this::getFieldName,
                        Function.identity(),
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

    private <T> T mapToEntity(ResultSet resultSet, LinkedHashMap<String, Field> fields, Class<T> type) throws Exception {
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

    private <T> void saveStateToSnapshot(EntityKey key, T entity, LinkedHashMap<String, Field> fields) {
        Object[] state = toSnapshot(entity, fields);
        snapshots.put(key, state);
    }

    private <T> Object[] toSnapshot(T entity, LinkedHashMap<String, Field> fields) {
        return fields.values()
                .stream()
                .filter(field -> !field.getName().equals(getIdColumnName(fields)))
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(entity);
                    } catch (IllegalAccessException e) {
                        throw new OrmException("Cannot get entity state", e);
                    }
                }).toArray();

    }

    private boolean isDirty(Object[] previousState, Object[] currentState) {
        for (int i = 0; i < previousState.length; i++) {
            if (!previousState[i].equals(currentState[i])) {
                return true;
            }
        }
        return false;
    }

    private void executeUpdate(EntityKey key, Object[] currentState, LinkedHashMap<String, Field> entityFields) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = prepareUpdateStatement(connection, key, currentState, entityFields);
            int rowsUpdated = stm.executeUpdate();
            if (rowsUpdated != 1) {
                throw new OrmException("Cannot update entity " + key);
            }
        } catch (Exception e) {
            throw new OrmException("Error", e);
        }
    }

    private <T> PreparedStatement prepareUpdateStatement(Connection connection,
                                                         EntityKey key,
                                                         Object[] currentState,
                                                         LinkedHashMap<String, Field> fields) throws SQLException {
        String idColumnName = getIdColumnName(fields);
        String setValues = fields.keySet()
                .stream()
                .filter(field -> !field.equals(idColumnName))
                .collect(Collectors.joining("=?, ", "", "=?"));
        String sql = String.format(
                UPDATE_SQL,
                getTableName(key.getType()),
                setValues,
                idColumnName
        );
        PreparedStatement stm = connection.prepareStatement(sql);
        int i = 1;
        for (; i <= currentState.length; i++) {
            stm.setObject(i, currentState[i - 1]);
        }
        stm.setObject(i, key.getId());
        return stm;
    }

    private String getFieldName(Field field) {
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

}
