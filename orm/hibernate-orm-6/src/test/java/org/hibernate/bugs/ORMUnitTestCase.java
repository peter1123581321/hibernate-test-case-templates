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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
        annotatedClasses = {
                ORMUnitTestCase.Person.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
        }
)
@SessionFactory
class ORMUnitTestCase {

    @Test
    void testHHH9812(SessionFactoryScope scope) {

        scope.inTransaction(session -> {
            Person person = new Person();
            person.foo = true;
            session.persist(person);
        });

        scope.inTransaction(session -> {
            Person person = session.createSelectionQuery("select p from Person p", Person.class).getSingleResult();
            assertEquals(1, person.formula);

        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        long id;

        boolean foo;

        @Formula("CAST(foo AS unsigned)")
        int formula;
    }
}
