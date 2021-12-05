package com.anderb.breskulorm.action;

import com.anderb.breskulorm.EntityKey;
import com.anderb.breskulorm.EntityMetadata;
import com.anderb.breskulorm.EntityPersister;
import com.anderb.breskulorm.Session;
import com.anderb.breskulorm.exception.OrmException;

public class InsertAction extends Action {

    public InsertAction(EntityKey id, Object instance, Session session) {
        super(id, instance, session);
    }

    @Override
    void execute() throws OrmException {
        EntityKey key = getKey();
        EntityMetadata metadata = key.getMetadata();
        Session session = getSession();
        Object instance = getInstance();
        EntityPersister persister = metadata.getPersister();
        persister.insert(key, instance, session);
    }
}
