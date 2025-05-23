/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bugs;

import jakarta.persistence.*;
import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DomainModel(
        annotatedClasses = {
                HHH18891Test.Person.class,
                HHH18891Test.Document.class,
                HHH18891Test.DocumentException.class,
                HHH18891Test.DocumentIgnore.class
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
public class HHH18891Test {

    @Test
    void hhh18891TestWithNotFoundIgnore(SessionFactoryScope scope) {

        // prepare document
        scope.inTransaction(session -> {
            Query nativeQuery = session.createNativeQuery(
                    "insert into DocumentIgnore (id,owner) values (123,42)");
            nativeQuery.executeUpdate();
        });

        // assert document
        scope.inTransaction(session -> {
            final DocumentIgnore document = session.find(DocumentIgnore.class, 123);
            assertNotNull(document);
            assertEquals(123, document.id);
            assertNull(document.owner);
        });
    }

    @Test
    void hhh18891TestWithNotFoundException(SessionFactoryScope scope) {

        // prepare document
        scope.inTransaction(session -> {
            Query nativeQuery = session.createNativeQuery(
                    "insert into DocumentException (id,owner) values (123,42)");
            nativeQuery.executeUpdate();
        });

        // assert document
        scope.inTransaction(session -> {
            assertThrows(FetchNotFoundException.class, () ->
                    session.find(DocumentException.class, 123));
        });
    }

    @Test
    void hhh18891TestWithoutNotFoundAnnotation(SessionFactoryScope scope) {

        // prepare document
        scope.inTransaction(session -> {
            Query nativeQuery = session.createNativeQuery(
                    "insert into Document (id,owner) values (123,42)");
            nativeQuery.executeUpdate();
        });

        // assert document
        scope.inTransaction(session -> {
            final Document document = session.find(Document.class, 123);
            assertNotNull(document);
            assertEquals(123, document.id);
            assertNull(document.owner);
        });
    }

    @Entity(name = "DocumentIgnore")
    public static class DocumentIgnore {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne
        @NotFound(action = NotFoundAction.IGNORE)
        @JoinColumnOrFormula(column = @JoinColumn(name = "owner", referencedColumnName = "id", insertable = false,
                updatable = false))
        @JoinColumnOrFormula(formula = @JoinFormula(value = "'fubar'", referencedColumnName = "name"))
        Person owner;
    }

    @Entity(name = "DocumentException")
    public static class DocumentException {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne
        @NotFound(action = NotFoundAction.EXCEPTION)
        @JoinColumnOrFormula(column = @JoinColumn(name = "owner", referencedColumnName = "id", insertable = false,
                updatable = false))
        @JoinColumnOrFormula(formula = @JoinFormula(value = "'fubar'", referencedColumnName = "name"))
        Person owner;
    }

    @Entity(name = "Document")
    public static class Document {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne
        @JoinColumnOrFormula(column = @JoinColumn(name = "owner", referencedColumnName = "id", insertable = false,
                updatable = false))
        @JoinColumnOrFormula(formula = @JoinFormula(value = "'fubar'", referencedColumnName = "name"))
        Person owner;
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        Long id;

        String name;
    }
}
