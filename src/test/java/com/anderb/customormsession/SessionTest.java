package com.anderb.customormsession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionTest {

    @BeforeEach
    void setUp() {
        SessionFactory factory = new SessionFactory(
                "jdbc:postgresql://93.175.204.87:5432/postgres",
                "postgres",
                "postgres"
        );
        Session session = factory.createSession();
        Person person = session.find(Person.class, 21L);
        System.out.println(person);
    }

    @Test
    void find() {
    }
}