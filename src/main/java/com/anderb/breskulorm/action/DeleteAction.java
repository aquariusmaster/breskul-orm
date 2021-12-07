package com.anderb.breskulorm.action;

import com.anderb.breskulorm.EntityKey;
import com.anderb.breskulorm.EntityPersister;
import com.anderb.breskulorm.Session;
import com.anderb.breskulorm.exception.OrmException;

public class DeleteAction extends Action {

    public DeleteAction(EntityKey id, Object instance, Session session) {
        super(id, instance, session);
    }

    @Override
    void execute() throws OrmException {
        EntityKey key = getKey();
        Session session = getSession();
        EntityPersister persister = key.getMetadata().getPersister();
        persister.delete(key, session);
        session.getPersistenceContext().remove(key);
        session.getSnapshots().remove(key);
    }
}
