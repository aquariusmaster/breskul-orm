package com.anderb.breskulorm;

import com.anderb.breskulorm.exception.OrmException;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Map;

import static com.anderb.breskulorm.annotation.GenerationType.IDENTITY;
import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class EntityPersister {

    public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
    };

    public Serializable getIdValue(Object instance, EntityMetadata metadata) {
        try {
            Field idField = metadata.getIdField();
            idField.setAccessible(true);
            return (Serializable) idField.get(instance);
        } catch (IllegalAccessException e) {
            throw new OrmException("Error fetch entity id value", e);
        }
    }

    public Object loadFromDatasource(EntityKey key, Session session) {
        try {
            PreparedStatement stm = prepareFindByIdStatement(session.getConnection(), key);
            ResultSet resultSet = stm.executeQuery();
            return key.getMetadata().getPersister().mapToEntity(resultSet, key.getMetadata());
        } catch (Exception e) {
            throw new OrmException("Cannot load entity from DB", e);
        }
    }

    public void update(EntityKey key, Object instance, Session session) {
        try {
            PreparedStatement stm = prepareUpdateStatement(session, key, instance);
            int rowsUpdated = stm.executeUpdate();
            if (rowsUpdated != 1) {
                throw new OrmException("Cannot update entity " + key);
            }
        } catch (Exception e) {
            throw new OrmException("Error", e);
        }
    }

    public Serializable insert(EntityKey key, Object instance, Session session) {
        try {
            EntityMetadata metadata = key.getMetadata();
            PreparedStatement stm = prepareInsertStatement(session, key, instance);
            int rowsUpdated = stm.executeUpdate();
            if (rowsUpdated != 1) {
                throw new OrmException("Cannot insert entity " + metadata + ". No rows affected.");
            }
            try (ResultSet generatedKeys = stm.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Serializable newId = (Serializable) generatedKeys.getObject(1);
                    setIdentifier(metadata, instance, newId);
                    return newId;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (Exception e) {
            throw new OrmException("Error", e);
        }
    }

    public void delete(EntityKey key, Session session) {
        try {
            PreparedStatement stm = prepareDeleteStatement(session.getConnection(), key);
            int rowsUpdated = stm.executeUpdate();
            if (rowsUpdated != 1) {
                throw new OrmException("Cannot insert entity " + key);
            }
            session.getPersistenceContext().remove(key);
            session.getSnapshots().remove(key);
        } catch (Exception e) {
            throw new OrmException("Error", e);
        }
    }

    public void setIdentifier(EntityMetadata metadata, Object instance, Serializable value) {
        Field idField = metadata.getIdField();
        setValueToField(idField, instance, value);
    }

    public void setValueToField(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new OrmException("Cannot set value to field", e);
        }
    }

    public <T> T mapToEntity(ResultSet resultSet, EntityMetadata metadata) throws Exception {
        if (!resultSet.next()) {
            return null;
        }
        T instance = (T) metadata.getType().getDeclaredConstructor().newInstance();
        for (Map.Entry<String, Field> entry : metadata.getFields().entrySet()) {
            setValueToField(entry.getValue(), instance, resultSet.getObject(entry.getKey()));
        }
        return instance;
    }

    public Serializable generateIdentifier(EntityMetadata entityMetadata, Session session) {
        if (entityMetadata.getIdGenerationType() == IDENTITY) {
            return POST_INSERT_INDICATOR;
        }
        return callNextSequenceValue(session);
    }

    private PreparedStatement prepareDeleteStatement(Connection connection, EntityKey key)
            throws SQLException {
        EntityMetadata metadata = key.getMetadata();
        String sql = String.format(
                "DELETE FROM %s WHERE %s=?",
                metadata.getTableName(),
                metadata.getIdColumnName()
        );
        PreparedStatement stm = connection.prepareStatement(sql);
        stm.setObject(1, key.getIdentifier());
        return stm;
    }

    private PreparedStatement prepareInsertStatement(Session session, EntityKey key, Object instance)
            throws SQLException {
        EntityMetadata metadata = key.getMetadata();
        Serializable id = key.getIdentifier();
        Object[] currentState = session.toSnapshot(metadata, instance);
        boolean includeId = id != null;
        String sql = String.format(
                "INSERT INTO %s(%s) VALUES(%s)",
                metadata.getTableName(),
                metadata.getInsertSetValueSql(),
                getValuesSigns(metadata.getFields().values().size(), includeId)
        );
        PreparedStatement stm = session.getConnection().prepareStatement(sql, RETURN_GENERATED_KEYS);
        int i = 1;
        while (i <= currentState.length) {
            stm.setObject(i, currentState[i - 1]);
            i++;
        }
        if (includeId) {
            stm.setObject(i, id);
        }
        return stm;
    }

    private String getValuesSigns(int valuesSize, boolean includeIdField) {
        int questionNumber = includeIdField ? valuesSize : valuesSize - 1;
        return "?, ".repeat(questionNumber).substring(0, 3 * questionNumber - 2);
    }

    private PreparedStatement prepareUpdateStatement(Session session,
                                                     EntityKey key,
                                                     Object instance) throws SQLException {
        EntityMetadata metadata = key.getMetadata();
        Object[] currentState = session.toSnapshot(key.getMetadata(), instance);
        String sql = String.format(
                "UPDATE %s SET %s WHERE %s=?",
                metadata.getTableName(),
                metadata.getUpdateSetValueSql(),
                metadata.getIdColumnName()
        );
        PreparedStatement stm = session.getConnection().prepareStatement(sql);
        for (int i = 1; i <= currentState.length; i++) {
            stm.setObject(i, currentState[i - 1]);
        }
        stm.setObject(currentState.length + 1, key.getIdentifier());
        return stm;
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

    private Serializable callNextSequenceValue(Session session) {
        try {
            CallableStatement stm = session.getConnection()
                    .prepareCall("call next value for orm_sequence");
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return (Serializable) rs.getObject(1);
            }
            throw new OrmException("Cannot get sequence next value. ResultSet is empty!");
        } catch (OrmException e) {
            throw e;
        } catch (Exception e) {
            throw new OrmException("Cannot get sequence next value", e);
        }
    }

}
