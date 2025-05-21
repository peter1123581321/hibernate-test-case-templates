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
                // Add your entities here.
                InsertTest.Person.class,
                InsertTest.Document.class
        }
)
@ServiceRegistry(
        // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
        settings = {
                // For your own convenience to see generated queries:
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
                // @Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
        }
)
@SessionFactory
public class InsertTest {

    @Test
    void hhh18835Test(SessionFactoryScope scope) {


        scope.inTransaction(session -> {
            Person person = new Person();
            person.name = "Peter";
            session.persist(person);
        });

		/*
		with id: SimpleInsertQueryPlan
		without id: MultiTableInsertQueryPlan
		 */
        scope.inTransaction(session -> {

//			Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();

            session.createMutationQuery(
//							"insert into Document(id, name,owner) select 3,concat(p.name,'s document'), p from Person p" )
                            "insert into Document(name,owner) select concat(p.name,'s document'), p from Person p")
//							"insert into Document(name,owner) values ('test',:person)" )
//					.setParameter( "person", person )
                    .executeUpdate();

        });

        scope.inTransaction(session -> {
            Person person = session.createQuery("select p from Person p", Person.class).getSingleResult();
            assertNotNull(person);
            assertEquals("Peter", person.name);
        });

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
//	@SecondaryTable(name = "test")
    public static class Document {

        @Id
        @GeneratedValue
//		@GeneratedValue(
//				strategy = GenerationType.TABLE,
//				generator = "hibernate_sequence"
//		)
//		@SequenceGenerator(
//				name = "hibernate_sequence",
//				allocationSize = 1
//		)
        Long id;

        String name;

//		@Column(table = "test")
//		String test;

        @ManyToOne
        Person owner;

//		@Version
//		int version;
    }
}
