package com.anderb.customormsession;

import com.anderb.breskulcp.BreskulCPDataSource;
import com.anderb.breskulcp.DataSourceConfigs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionTest {

    private static SessionFactory factory;

    @BeforeAll
    static void setUp() throws Exception {
        DataSourceConfigs configs = DataSourceConfigs.builder()
                .jdbcUrl("jdbc:h2:mem:testdb")
                .username("sa")
                .password("sa")
                .driverClassName("org.h2.Driver")
                .poolSize(15)
                .connectionTimeout(60_000)
                .build();
        BreskulCPDataSource breskulCPDataSource = new BreskulCPDataSource(configs);
        prepareDB(breskulCPDataSource);
        factory = new SessionFactory(breskulCPDataSource);

    }

    @Test
    void find_whenPersonWithGivenIdExists_shouldFindAndCreatePersonInstance() {
        Session session = factory.createSession();
        Person person = session.find(Person.class, 1L);
        assertEquals(1L, person.getId());
        assertEquals("Andrii", person.getFirstName());
        assertEquals("Bobrov", person.getLastName());
    }

    private static void prepareDB(BreskulCPDataSource dataSource) throws Exception {
        try (var conn = dataSource.getConnection();
             var stm = conn.createStatement();
             var is = SessionTest.class.getClassLoader().getResourceAsStream("prepare-person.sql")) {

            String content = new String(is.readAllBytes());
            stm.execute(content);
        }
    }
}