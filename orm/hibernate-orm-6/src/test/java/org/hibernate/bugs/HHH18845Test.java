/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Optional;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.junit.jupiter.api.Assertions.*;

@DomainModel(
        annotatedClasses = {
                HHH18845Test.Document.class, HHH18845Test.Person.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
        }
)
@SessionFactory
class HHH18845Test {

    @Test
    void hhh18845Test_personWithGivenId(SessionFactoryScope scope) {

        // prepare
        EntityExistsException exception = assertThrows(EntityExistsException.class, () -> scope.inTransaction(session -> {
            Person person = new Person();
            person.id = 123L;
            session.persist(person);
        }));
        // note: when @DocumentIdGenerator or @GeneratedValue is used and an id was set manually, an exception is thrown
        assertTrue(exception.getMessage().contains("detached entity passed to persist: org.hibernate.bugs.HHH18845Test$Person"));
    }

    @Test
    void hhh18845Test_personWithoutGivenId(SessionFactoryScope scope) {

        // prepare
        scope.inTransaction(session -> {
            Person person = new Person();
            session.persist(person);
        });

        // assert
        scope.inTransaction(session -> {
            Person person = session.createSelectionQuery("select d from Person d", Person.class).getSingleResult();
            assertEquals(456, person.id); // generator is called as expected
            session.remove(person);
        });
    }

    @Test
    void hhh18845Test_documentWithGivenId(SessionFactoryScope scope) {

        // prepare
        scope.inTransaction(session -> {
            Document document = new Document();
            document.id = 123L;
            document.version = 42L;
            session.persist(document);
        });

        // assert
        scope.inTransaction(session -> {
            Document document = session.createSelectionQuery("select d from Document d", Document.class).getSingleResult();
            assertEquals(123, document.id);
            assertEquals(42, document.version);
            session.remove(document);
        });
    }

    @Test
    void hhh18845Test_documentWithoutGivenId(SessionFactoryScope scope) {

        // prepare
        scope.inTransaction(session -> {
            Document document = new Document();
            document.version = 42L;
            session.persist(document);
        });

        // assert
        scope.inTransaction(session -> {
            Document document = session.createSelectionQuery("select d from Document d", Document.class).getSingleResult();
            assertEquals(456, document.id); // fails, because generator not called
            assertEquals(42, document.version);
            session.remove(document);
        });
    }

    @IdGeneratorType(DocumentIdentifyGenerator.class)
    @Retention(RUNTIME)
    @Target({FIELD, METHOD})
    public @interface DocumentIdGenerator {
    }

    public record DocumentEntityId(long id, long version) {
    }

    @IdClass(DocumentEntityId.class)
    @Entity(name = "Document")
    public static class Document {

        @Id
        @DocumentIdGenerator
        Long id;

        @Id
        Long version;
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @DocumentIdGenerator
        Long id;
    }

    public static class DocumentIdentifyGenerator implements IdentifierGenerator {

        @Override
        public Object generate(SharedSessionContractImplementor session, Object object) {
            if (object instanceof Document document) {
                return Optional.ofNullable(document.id).orElse(456L);
            } else if (object instanceof Person person) {
                return Optional.ofNullable(person.id).orElse(456L);
            }
            return null;
        }

        @Override
        public EnumSet<EventType> getEventTypes() {
            return INSERT_ONLY; //applied only on insert new record
        }
    }
}
