/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bugs;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
        annotatedClasses = {
                HHH18835Test.Person.class,
                HHH18835Test.Document.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
                // @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
        }
)
@SessionFactory
public class HHH18835Test {

    @Test
    void hhh18835Test(SessionFactoryScope scope) {

        // prepare person
        scope.inTransaction(session -> {
            Person person = new Person();
            person.name = "Peter";
            session.persist(person);
        });

        // insert document
        scope.inTransaction(session -> {
            session.createMutationQuery(
                            "insert into Document(name,owner) select concat(p.name,'s document'), p from Person p")
                    .executeUpdate();
        });

        // assert person
        scope.inTransaction(session -> {
            Person person = session.createQuery("select p from Person p", Person.class).getSingleResult();
            assertNotNull(person);
            assertEquals("Peter", person.name);
        });

        // assert document
        scope.inTransaction(session -> {
            Document document = session.createQuery("select d from Document d", Document.class).getSingleResult();
            assertNotNull(document);
            assertEquals("Peters document", document.name);
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        Long id;

        String name;
    }

    @Entity(name = "Document")
    public static class Document {

        @Id
        @GeneratedValue
        Long id;

        String name;

        @ManyToOne
        Person owner;
    }
}
