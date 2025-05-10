package org.hibernate.bugs;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
class JPAUnitTestCase {

    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("templatePU");
    }

    @AfterEach
    void destroy() {
        entityManagerFactory.close();
    }

    // Entities are auto-discovered, so just add them anywhere on class-path
    // Add your tests, using standard JUnit.
    @Test
    void hhh123Test() throws Exception {

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        savePerson(entityManager);
        insertDocument(entityManager);
        assertPerson(entityManager);
        assertDocument(entityManager);

        entityManager.close();
    }


    private void savePerson(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        Person person = new Person();
        person.name = "Peter";
        entityManager.persist(person);
        entityManager.getTransaction().commit();
    }

    private void insertDocument(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("insert into Document(name,owner) select concat(p.name,'s document'), p from Person p").executeUpdate();
        entityManager.getTransaction().commit();
    }

    private void assertPerson(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        Person person = entityManager.createQuery("select p from Person p", Person.class).getSingleResult();
        assertNotNull(person);
        assertEquals("Peter", person.name);
        entityManager.getTransaction().commit();
    }

    private void assertDocument(EntityManager entityManager) {
        entityManager.getTransaction().begin();
        Document document = entityManager.createQuery("select d from Document d", Document.class).getSingleResult();
        assertNotNull(document);
        assertEquals("Peters document", document.name);
        entityManager.getTransaction().commit();
    }


}
