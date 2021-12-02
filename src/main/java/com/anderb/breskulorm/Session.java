package com.anderb.breskulorm;

import com.anderb.breskulorm.exception.OrmException;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Session implements AutoCloseable {

    private final DataSource dataSource;
    private final EntityMetadataResolver metadataResolver;

    private final Map<EntityKey, Object[]> snapshots = new HashMap<>();
    private final Map<EntityKey, Object> persistenceContext = new HashMap<>();
    private boolean readOnly;
    private boolean closed;

    public Session(DataSource dataSource, EntityMetadataResolver metadataResolver) {
        this.dataSource = dataSource;
        this.metadataResolver = metadataResolver;
    }

    public <T> T find(Class<T> type, Object id) {
        checkOpen();
        Object entity = persistenceContext.computeIfAbsent(
                EntityKey.of(id, metadataResolver.getEntityMetadata(type)),
                this::loadFromDatasource);
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
                    var currentState = toSnapshot(entry.getKey().getMetadata(), entry.getValue());
                    if (isDirty(snapshots.get(entry.getKey()), currentState)) {
                        executeUpdate(entry.getKey(), currentState);
                    }
                });
        snapshots.clear();
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
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
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = prepareFindByIdStatement(connection, key);
            ResultSet resultSet = stm.executeQuery();
            T entity = mapToEntity(resultSet, key.getMetadata());
            saveStateToSnapshotIfNeeded(key, entity);
            return entity;
        } catch (Exception e) {
            throw new OrmException("Cannot load entity from DB", e);
        }
    }

    private <T> void saveStateToSnapshotIfNeeded(EntityKey key, T entity) {
        if (!readOnly) {
            saveStateToSnapshot(key, entity);
        }
    }

    private <T> T mapToEntity(ResultSet resultSet, EntityMetadata metadata) throws Exception {
        if (!resultSet.next()) {
            return null;
        }
        T instance = (T) metadata.getType().getDeclaredConstructor().newInstance();
        for (Map.Entry<String, Field> entry : metadata.getFields().entrySet()) {
            Field field = entry.getValue();
            field.setAccessible(true);
            field.set(instance, resultSet.getObject(entry.getKey()));
        }
        return instance;
    }

    private <T> void saveStateToSnapshot(EntityKey key, T entity) {
        Object[] state = toSnapshot(key.getMetadata(), entity);
        snapshots.put(key, state);
    }

    private <T> Object[] toSnapshot(EntityMetadata metadata, T entity) {
        return metadata.getFields().values()
                .stream()
                .filter(field -> !field.getName().equals(metadata.getIdColumnName()))
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

    private void executeUpdate(EntityKey key, Object[] currentState) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement stm = prepareUpdateStatement(connection, key, currentState);
            int rowsUpdated = stm.executeUpdate();
            if (rowsUpdated != 1) {
                throw new OrmException("Cannot update entity " + key);
            }
        } catch (Exception e) {
            throw new OrmException("Error", e);
        }
    }

    private PreparedStatement prepareFindByIdStatement(Connection connection, EntityKey key) throws SQLException {
        PreparedStatement stm = connection.prepareStatement(
                String.format(
                        "SELECT * FROM %s WHERE %s=?",
                        key.getMetadata().getTableName(),
                        key.getMetadata().getIdColumnName()
                )
        );
        stm.setObject(1, key.getIdentifier());
        return stm;
    }

    private PreparedStatement prepareUpdateStatement(Connection connection,
                                                     EntityKey key,
                                                     Object[] currentState) throws SQLException {
        EntityMetadata metadata = key.getMetadata();
        String sql = String.format(
                "UPDATE %s SET %s WHERE %s=?",
                metadata.getTableName(),
                metadata.getUpdateSetValue(),
                metadata.getIdColumnName()
        );
        PreparedStatement stm = connection.prepareStatement(sql);
        int i = 1;
        for (; i <= currentState.length; i++) {
            stm.setObject(i, currentState[i - 1]);
        }
        stm.setObject(i, key.getIdentifier());
        return stm;
    }

}
