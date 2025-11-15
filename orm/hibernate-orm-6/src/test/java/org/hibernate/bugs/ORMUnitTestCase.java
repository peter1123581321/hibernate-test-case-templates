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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DomainModel(
        annotatedClasses = {
                ORMUnitTestCase.MyPerson.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "false")
        }
)
@SessionFactory
class ORMUnitTestCase {

    @Test
    void hhh123Test(SessionFactoryScope scope) {

        // insert (warum weren hier nach 10 insert noch 10 updates gemacht?)
        scope.inTransaction(session -> {
            for (int i = 0; i < 10; i++) {
                MyPerson person = new MyPerson("Peter" + i);
                person.myJson = new MyJson();
                session.persist(person);
            }
        });

        // assert (warum werden hier nach dem select dann noch 10 updates ausgeführt?)
        scope.inTransaction(session -> {
            List<MyPerson> persons = session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();
            assertEquals(10, persons.size());
            for (int i = 0; i < 10; i++) {
                MyPerson person = persons.get(i);
                assertEquals(1, person.version);
            }
        });

        // process

        // n*(n+1)/2, variante wie im transfer service
        scope.inTransaction(session -> {
            List<Long> ids = session.createQuery("select id from MyPerson p", Long.class).getResultList();
            for (int i = 0; i < 10; i++) {
                MyPerson myPerson = session.createQuery("select p from MyPerson p where id=:id", MyPerson.class)
                        .setParameter("id", ids.get(i)).getSingleResult();
                myPerson.name = "Neu" + i;
            }
        });

        // hier werden nach jedem der 11 select anschließend 10 updates ausgeführt
        /*
        scope.inTransaction(session -> {
            List<MyPerson> persons = session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();
            for (int i = 0; i < 10; i++) {
                session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();
                MyPerson person = persons.get(i);
                person.name = "Neu" + i;
            }
        });
         */

        // zuerst 11 selects und anschließend 10 updates -- ist das die richtige variante?
        /*
        scope.inTransaction(session -> {
            List<MyPerson> persons = session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();
            for (int i = 0; i < 10; i++) {
                int personIndex = i;
                AtomicReference<MyPerson> myPerson = new AtomicReference<>();
                scope.inTransaction(session2 -> {
                    session2.setDefaultReadOnly(true);
                    myPerson.set(session2.createQuery("select p from MyPerson p where id=:id", MyPerson.class)
                            .setParameter("id", persons.get(personIndex).id)
                            .getSingleResult());
                });
                myPerson.get().name = "Neuer Name";
            }
        });
         */

        // assert
        scope.inTransaction(session -> {
            List<MyPerson> persons = session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();
            assertEquals(10, persons.size());
            for (int i = 0; i < 10; i++) {
                MyPerson person = persons.get(i);
                assertEquals(1, person.version);
            }
        });
    }

    @Entity(name = "MyPerson")
    public static class MyPerson {

        @Id
        @GeneratedValue
        Long id;

        String name;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "my_json")
        MyJson myJson;

        @Version
        int version;

        public MyPerson() {
        }

        public MyPerson(String name) {
            this.name = name;
        }
    }

    public static class MyJson {
        public String name = "name";
    }
}
