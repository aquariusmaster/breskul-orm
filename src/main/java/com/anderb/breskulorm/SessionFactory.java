package com.anderb.breskulorm;

import javax.sql.DataSource;

public class SessionFactory {
    private final DataSource dataSource;
    private final EntityMetadataResolver entityMetadataResolver;

    public SessionFactory(DataSource dataSource, Class<?>... entityClasses) {
        this.dataSource = dataSource;
        entityMetadataResolver = new EntityMetadataResolver(entityClasses);
    }

    public Session createSession() {
        return new Session(dataSource, entityMetadataResolver);
    }
}
