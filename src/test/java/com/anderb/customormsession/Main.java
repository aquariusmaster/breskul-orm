package com.anderb.customormsession;

public class Main {
    public static void main(String[] args) {
//        EntityManagerFactory emf = Persistence.createEntityManagerFactory("taras-db");
//        EntityManager em = emf.createEntityManager();
//        em.getTransaction().begin();
        Person me = new Person();
        me.setFirstName("Andrii");
        me.setLastName("Bobrov");
//        em.persist(me);
//        em.getTransaction().commit();
//        Person person = em.find(Person.class, 17L);
//        System.out.println(me);
//        em.close();
    }
}
