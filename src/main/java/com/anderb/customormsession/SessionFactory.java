package com.anderb.customormsession;

import lombok.RequiredArgsConstructor;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class SessionFactory {
    private final DataSource dataSource;


    public SessionFactory(String url, String username, String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        this.dataSource = dataSource;
    }

    public Session createSession() {
        return new Session(dataSource);
    }
}
