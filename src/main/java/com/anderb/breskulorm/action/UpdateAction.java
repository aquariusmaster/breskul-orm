package com.anderb.breskulorm.action;

import com.anderb.breskulorm.EntityKey;
import com.anderb.breskulorm.EntityPersister;
import com.anderb.breskulorm.Session;
import com.anderb.breskulorm.exception.OrmException;


public class UpdateAction extends Action {

    public UpdateAction(EntityKey key, Object instance, Session session) {
        super(key, instance, session);
    }

    @Override
    void execute() throws OrmException {
        EntityKey key = getKey();
        Session session = getSession();
        Object instance = getInstance();
        EntityPersister persister = key.getMetadata().getPersister();
        persister.update(key, instance, session);
        session.getPersistenceContext().put(key, instance);
        session.saveStateToSnapshotIfNeeded(key, instance);
    }
}
