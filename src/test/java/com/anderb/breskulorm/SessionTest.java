package com.anderb.breskulorm;

import com.anderb.breskulcp.BreskulCPDataSource;
import com.anderb.breskulcp.DataSourceConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionTest {

    private SessionFactory subject;
    private BreskulCPDataSource spyDataSource;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceConfigs configs = DataSourceConfigs.builder()
                .jdbcUrl("jdbc:h2:mem:testdb")
                .username("sa")
                .password("sa")
                .driverClassName("org.h2.Driver")
                .poolSize(1)
                .connectionTimeout(60_000)
                .build();
        spyDataSource = new BreskulCPDataSource(configs);
        prepareDB(spyDataSource);
        spyDataSource = spy(spyDataSource);
        subject = new SessionFactory(spyDataSource, Person.class, Address.class);
    }

    @Test
    void find_whenPersonWithGivenIdExists_shouldFindAndCreatePersonInstance() {
        Session session = subject.createSession();
        Person person = session.find(Person.class, 1L);
        assertEquals(1L, person.getId());
        assertEquals("Andrii", person.getFirstName());
        assertEquals("Bobrov", person.getLastName());
        session.close();
    }

    @Test
    void find_whenEntityAlreadyInCache_shouldNotCallDB() throws SQLException {
        Session session = subject.createSession();
        Person person = session.find(Person.class, 2L);
        Person person2 = session.find(Person.class, 2L);
        assertNotNull(person);
        assertNotNull(person2);
        verify(spyDataSource, times(1)).getConnection();
        session.close();
    }

    @Test
    void close_whenSessionClosing_shouldUpdateEntityInDB() {
        Session session = subject.createSession();
        Person person = session.find(Person.class, 2L);
        person.setFirstName("Changed");
        session.close();
        Session session2 = subject.createSession();
        Person person2 = session2.find(Person.class, 2L);
        assertEquals("Changed", person2.getFirstName());
        session2.close();
    }

    @Test
    void persist_whenPersistNewPerson_shouldSaveInDBAfterFlush() {
        Session session = subject.createSession();
        Person person = new Person();
        person.setFirstName("New Person Name");
        person.setLastName("New Person Last Name");
        session.persist(person);
        session.flush();
        assertNotNull(person.getId());
        session.close();
    }

    @Test
    void persist_whenPersistEntityWithIdentityGenerationType_shouldSaveInDBImmediately() {
        Session session = subject.createSession();
        Address address = new Address();
        address.setAddressLine("New Address Line");
        address.setCity("New York");
        session.persist(address);
        assertNotNull(address.getId());
        session.close();
    }

    @Test
    void persist_whenPersistEntityWithSequenceGenerationType_shouldGenerateIdBeforeFlushToDB() {
        Session session = subject.createSession();
        Person person = new Person();
        person.setFirstName("New Person Name");
        person.setLastName("New Person Last Name");
        session.persist(person);
        assertNotNull(person.getId());
        assertTrue(session.getPersistenceContext().containsValue(person));
        assertFalse(session.getSnapshots().isEmpty());
        assertEquals(person, session.getActionQueue().getInsertions().get(0).getInstance());
        session.flush();
        assertTrue(session.getActionQueue().getInsertions().isEmpty());
        session.close();
    }

    @Test
    void delete() {
        Session session = subject.createSession();
        Person person = session.find(Person.class, 3L);
        session.delete(person);
        session.flush();
        Person deleted = session.find(Person.class, 3L);
        assertNull(deleted);
        session.close();
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