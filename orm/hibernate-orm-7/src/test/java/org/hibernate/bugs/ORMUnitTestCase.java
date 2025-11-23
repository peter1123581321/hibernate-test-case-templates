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
    void savePerson(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            MyPerson person = new MyPerson("name");
            person.myJson = new MyJson();
            session.persist(person);

            // Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: update not needed
        });
    }

    @Test
    void saveTwoPersons_modifyFirst_oneTransaction(SessionFactoryScope scope) {
        scope.inTransaction(session -> {

            MyPerson first = new MyPerson("name");
            first.myJson = new MyJson();
            session.persist(first);

            MyPerson second = new MyPerson("name");
            second.myJson = new MyJson();
            session.persist(second);

            first.name = "changed";

            // Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            // Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: second update not needed
        });

    }

    @Test
    void saveTwoPersons_modifyFirst_threeTransactions(SessionFactoryScope scope) {
        scope.inTransaction(session -> {

            MyPerson first = new MyPerson("name");
            first.myJson = new MyJson();
            session.persist(first);

            MyPerson second = new MyPerson("name");
            second.myJson = new MyJson();
            session.persist(second);

            // Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            // Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: updates not needed
        });

        scope.inTransaction(session -> {

            MyPerson myPerson = session
                    .createQuery("select p from MyPerson p", MyPerson.class)
                    .getResultList()
                    .stream().findFirst().get();

            myPerson.name = "changed";

            // Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: second update not needed
        });

        scope.inTransaction(session -> {

            session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();

            // Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            // Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: no updates needed
        });
    }

    @Test
    void saveThreePersons_batchProcessing(SessionFactoryScope scope) {
        scope.inTransaction(session -> {

            MyPerson first = new MyPerson("name");
            first.myJson = new MyJson();
            session.persist(first);

            MyPerson second = new MyPerson("name");
            second.myJson = new MyJson();
            session.persist(second);

            MyPerson third = new MyPerson("name");
            third.myJson = new MyJson();
            session.persist(third);

            List<MyPerson> persons = session.createQuery("select p from MyPerson p", MyPerson.class).getResultList();

            for (MyPerson person : persons) {

                session.createQuery("select p from MyPerson p where p.id=:id", MyPerson.class).setParameter("id", person.id).getSingleResult();

            }

            //  Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            //  Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            //  Hibernate: insert into MyPerson (my_json,name,version,id) values (?,?,?,?)
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0 where mp1_0.id=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0 where mp1_0.id=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: select mp1_0.id,mp1_0.my_json,mp1_0.name,mp1_0.version from MyPerson mp1_0 where mp1_0.id=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?
            //  Hibernate: update MyPerson set my_json=?,name=?,version=? where id=? and version=?

            // note: no updates needed
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

        String text = "fubar";

//        public boolean equals(Object other) {
//            return other instanceof MyJson myJson && text.equals(myJson.text);
//        }
    }
}
