package com.anderb.breskulorm;

import com.anderb.breskulorm.exception.OrmException;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public class Transaction {
    private final Connection connection;
    private final Session session;
    private boolean isActive;

    public void begin() {
        if (session.isClosed()) {
            throw new IllegalStateException("Cannot begin Transaction on closed Session/EntityManager");
        }
        try {
            connection.setAutoCommit(false);
            isActive = true;
        } catch (SQLException e) {
            throw new OrmException("Cannot begin transaction", e);
        }
    }

    public void commit() {
        try {
            if (isActive) {
                isActive = false;
                connection.commit();
            }
        } catch (SQLException e) {
            throw new OrmException("Cannot begin transaction", e);
        }
    }

    public void rollback() {
        try {
            if (isActive) {
                isActive = false;
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new OrmException("Cannot begin transaction", e);
        }
    }

    public boolean isActive() {
        return isActive;
    }

}
