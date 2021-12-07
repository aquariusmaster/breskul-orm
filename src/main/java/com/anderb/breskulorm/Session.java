package com.anderb.breskulorm;

import com.anderb.breskulorm.action.*;
import com.anderb.breskulorm.exception.OrmException;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.anderb.breskulorm.EntityPersister.POST_INSERT_INDICATOR;


public class Session implements AutoCloseable {
    private final Connection connection;
    private final EntityMetadataResolver metadataResolver;
    private final ActionQueue actionQueue;

    private final Transaction transaction;
    private final Map<EntityKey, Object> persistenceContext = new HashMap<>();
    private final Map<EntityKey, Object[]> snapshots = new HashMap<>();
    private boolean readOnly;
    private boolean closed;

    public Session(Connection connection, EntityMetadataResolver metadataResolver) {
        this.connection = connection;
        this.metadataResolver = metadataResolver;
        transaction = new Transaction(connection, this);
        transaction.begin();
        actionQueue = new ActionQueue();
    }

    public <T> T find(Class<T> type, Object id) {
        checkOpen();
        Object entity = persistenceContext.computeIfAbsent(
                EntityKey.of((Serializable) id, metadataResolver.getEntityMetadata(type)),
                this::loadFromDatasource);
        return type.cast(entity);
    }

    public void update(Object entity) {
        checkOpen();
        EntityKey key = getEntityKey(entity);
        if (key == null) throw new IllegalStateException("Cannot get entity in context");
        if (isDirty(key, entity)) {
            fireUpdate(key, entity);
        }
    }

    public void persist(Object entity) {
        checkOpen();

        EntityKey entityKey = getEntityKey(entity);
        if (entityKey != null) return; //Ignoring persistent instance
        EntityMetadata metadata = metadataResolver.getEntityMetadata(entity.getClass());
        EntityPersister persister = metadata.getPersister();
        Serializable generatedId = persister.generateIdentifier(metadata, this);
        if (generatedId == POST_INSERT_INDICATOR) {
            actionQueue.execute(new IdentityInsertAction(metadata, entity, this));
            return;
        }
        EntityKey key = EntityKey.of(generatedId, metadata);
        persister.setIdentifier(metadata, entity, generatedId);
        persistenceContext.put(key, entity);
        saveStateToSnapshotIfNeeded(key, entity);
        firePersist(key, entity);
    }

    public void delete(Object entity) {
        checkOpen();
        EntityKey entityKey = getEntityKey(entity);
        if (entityKey == null) throw new IllegalArgumentException("Removing a detached instance " + entity);
        fireDelete(entityKey, entity);
    }

    public void flush() {
        persistenceContext.forEach(this::fireUpdate);
        actionQueue.executeActions();
    }

    public boolean isClosed() {
        return closed;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Connection getConnection() {
        return connection;
    }

    public EntityMetadataResolver getMetadataResolver() {
        return metadataResolver;
    }

    public <T> void saveStateToSnapshotIfNeeded(EntityKey key, T entity) {
        if (!readOnly && entity != null) {
            saveStateToSnapshot(key, entity);
        }
    }

    public <T> Object[] toSnapshot(EntityMetadata metadata, T entity) {
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
                })
                .toArray();
    }

    public Map<EntityKey, Object> getPersistenceContext() {
        return persistenceContext;
    }

    public Map<EntityKey, Object[]> getSnapshots() {
        return snapshots;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private void checkOpen() {
        if (isClosed()) {
            throw new IllegalStateException("Session is closed");
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    public ActionQueue getActionQueue() {
        return actionQueue;
    }

    public void close() {
        closed = true;
        flush();
        transaction.commit();
        clear();
        closeConnection();
    }

    /**
     * Clear session persistent context without flush to db
     */
    public void clear() {
        persistenceContext.clear();
        snapshots.clear();
    }

    private EntityKey getEntityKey(Object entity) {
        return persistenceContext
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(entity))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private Object loadFromDatasource(EntityKey key) {
        Object entity = key.getMetadata().getPersister().loadFromDatasource(key, this);
        saveStateToSnapshotIfNeeded(key, entity);
        return entity;
    }

    private <T> void saveStateToSnapshot(EntityKey key, T entity) {
        snapshots.put(key, toSnapshot(key.getMetadata(), entity));
    }

    private void fireUpdate(EntityKey key, Object instance) {
        actionQueue.addAction(new UpdateAction(key, instance, this));
    }

    private void firePersist(EntityKey key, Object instance) {
        actionQueue.addAction(new InsertAction(key, instance, this));
    }

    private void fireDelete(EntityKey entityKey, Object instance) {
        actionQueue.addAction(new DeleteAction(entityKey, instance, this));
    }

    private boolean isDirty(EntityKey key, Object entity) {
        if (isReadOnly()) return true;
        Object[] snapshot = snapshots.get(key);
        if (snapshot != null) {
            return isDirty(snapshot, toSnapshot(key.getMetadata(), entity));
        }
        return true;
    }

    private boolean isDirty(Object[] previousState, Object[] currentState) {
        for (int i = 0; i < previousState.length; i++) {
            if (!previousState[i].equals(currentState[i])) {
                return true;
            }
        }
        return false;
    }

}
