package com.anderb.customormsession;

public class MainOrm {
    public static void main(String[] args) {
        SessionFactory factory = new SessionFactory(
                "jdbc:postgresql://93.175.204.87:5432/postgres",
                "postgres",
                "postgres"
        );
        Session session = factory.createSession();
        Person person = session.find(Person.class, 21L);
        System.out.println(person);
//        Person me = new Person();
//        me.setFirstName("Andrii");
//        me.setLastName("Bobrov");
//        em.persist(me);
//        em.getTransaction().commit();
//        Person person = em.find(Person.class, 17L);
//        System.out.println(me);
//        em.close();
    }
}
