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

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 * <p>
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
@DomainModel(
        annotatedClasses = {
                // Add your entities here.
                Person.class,
                Document.class
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
class ORMUnitTestCase {

    // Add your tests, using standard JUnit 5.
    @Test
    void hhh123Test(SessionFactoryScope scope) {

        scope.inTransaction(session -> {
            Person person = new Person();
            person.name = "Peter";
            session.persist(person);
        });

        scope.inTransaction(session -> session.createMutationQuery("insert into Document(name,owner) select concat(p.name,'s document'), p from Person p").executeUpdate());

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
}
