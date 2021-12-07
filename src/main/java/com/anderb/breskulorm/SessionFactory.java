package com.anderb.breskulorm;

import com.anderb.breskulorm.exception.OrmException;

import javax.sql.DataSource;
import java.sql.SQLException;

public class SessionFactory {
    private final DataSource dataSource;
    private final EntityMetadataResolver entityMetadataResolver;

    public SessionFactory(DataSource dataSource, Class<?>... entityClasses) {
        this.dataSource = dataSource;
        entityMetadataResolver = new EntityMetadataResolver(entityClasses);
    }

    public Session createSession() {
        try {
            return new Session(dataSource.getConnection(), entityMetadataResolver);
        } catch (SQLException e) {
            throw new OrmException("Cannot create new Session", e);
        }
    }
}
