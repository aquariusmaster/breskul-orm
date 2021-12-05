package com.anderb.breskulorm.action;

import com.anderb.breskulorm.EntityKey;
import com.anderb.breskulorm.EntityMetadata;
import com.anderb.breskulorm.Session;
import com.anderb.breskulorm.exception.OrmException;

import java.io.Serializable;

public class IdentityInsertAction extends Action {
    private final EntityMetadata metadata;

    public IdentityInsertAction(EntityMetadata metadata, Object instance, Session session) {
        super(null, instance, session);
        this.metadata = metadata;
    }

    @Override
    void execute() throws OrmException {
        Session session = getSession();
        Object instance = getInstance();
        Serializable id = metadata.getPersister().insert(EntityKey.of(null, metadata), instance, session);
        EntityKey key = EntityKey.of(id, metadata);
        session.getPersistenceContext().put(key, instance);
        session.saveStateToSnapshotIfNeeded(key, instance);
    }
}
