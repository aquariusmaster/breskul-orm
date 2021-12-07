package com.anderb.breskulorm.action;

import com.anderb.breskulorm.EntityKey;
import com.anderb.breskulorm.Session;
import com.anderb.breskulorm.exception.OrmException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public abstract class Action {
    private EntityKey key;
    private Object instance;
    private Session session;

    abstract void execute() throws OrmException;
}
