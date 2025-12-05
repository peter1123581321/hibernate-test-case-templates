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

import jakarta.persistence.*;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DomainModel(
        annotatedClasses = {
                ORMUnitTestCase.Author.class
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
    void hhh8535Test(SessionFactoryScope scope) {

        // This situation can only happen via human being or bad migration/clone script.
        // Simulate this record being updated post table generation.
        scope.inTransaction(session -> {
            session.createNativeMutationQuery(
                    "UPDATE generator SET next_val = null where sequence_name = 'Author'"
            ).executeUpdate();
        });

        HibernateException hibernateException = assertThrows(HibernateException.class,
                () -> scope.inTransaction(session -> {
                    Author author = new Author();
                    session.persist(author);
                }));

        assertEquals("next_val for sequence_name 'Author' is null", hibernateException.getMessage());
    }

    @Entity(name = "Author")
    public static class Author {

        @Id
        @GeneratedValue(strategy = GenerationType.TABLE, generator = "generator")
        @TableGenerator(name = "generator", table = "generator")
        long id;
    }
}
