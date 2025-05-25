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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;


@DomainModel(
        annotatedClasses = {
                HHH18898Test.MyEntity.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
        }
)
@SessionFactory
class HHH18898Test {

    // Correct HQL, works
    @Test
    void hhh18898Test_1(SessionFactoryScope scope) {
        run(scope, "select z from MyEntity z where datum.value=:datum");
    }

    // Wrong syntax #1: With Hibernate 6.5.2, gives a NullPointerException
    @Test
    void hhh18898Test_2(SessionFactoryScope scope) {
        run(scope, "select z from MyEntity z where datum=:datum");
    }

    // Wrong syntax the other way round: With Hibernate 6.5.2, works fine (but why?)
    @Test
    void hhh18898Test_3(SessionFactoryScope scope) {
        run(scope, "select z from MyEntity z where :datum=datum");
    }

    private void run(SessionFactoryScope scope, String hql) {

        scope.inTransaction(session -> {
            QueryImplementor<MyEntity> query = session.createQuery(hql, MyEntity.class);
            query.setParameter("datum", new MyDate(LocalDate.now()), MyDateJavaType.TYPE);
//            query.setParameter("datum", LocalDate.now(), DateJdbcType.class);
            query.getResultList();
        });
    }

    @Embeddable
    public static class EmbeddableDatum {
        private LocalDate value;

        //        @JavaType(MyDateJavaType.class)
        public LocalDate getValue() {
            return value;
        }

        public void setValue(LocalDate datum) {
            this.value = datum;
        }
    }

    @Entity(name = "MyEntity")
    public static class MyEntity {

        @Id
        @Column(name = "id")
        long id;

        @Embedded
        @AttributeOverride(name = "value", column = @Column(name = "DATUM"))
        EmbeddableDatum datum;
    }


    public static class MyDate {
        private final LocalDate wrapped;

        public MyDate(LocalDate dateValue) {
            wrapped = dateValue;
        }

        public LocalDate toLocalDate() {
            return wrapped;
        }
    }

    public static class MyDateJavaType extends AbstractClassJavaType<MyDate> {
        private static final long serialVersionUID = 1L;
        private static final MyDateJavaType INSTANCE = new MyDateJavaType();
        public static final BasicType<MyDate> TYPE = new AbstractSingleColumnStandardBasicType<>(DateJdbcType.INSTANCE,
                INSTANCE) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return "MyDateJavaType";
            }
        };

        protected MyDateJavaType() {
            super(MyDate.class);
        }

        @Override
        public <X> X unwrap(final MyDate value, final Class<X> type, final WrapperOptions options) {
            LocalDate dateValue = (value == null ? null : value.toLocalDate());
            return LocalDateJavaType.INSTANCE.<X>unwrap(dateValue, type, options);
        }

        @Override
        public <X> MyDate wrap(final X value, final WrapperOptions options) {
            if (value instanceof MyDate) {
                return (MyDate) value;
            }
            LocalDate dateValue = LocalDateJavaType.INSTANCE.wrap(value, options);
            return dateValue == null ? null : new MyDate(dateValue);
        }

        @Override
        public JdbcType getRecommendedJdbcType(final JdbcTypeIndicators context) {
            return context.getJdbcType(SqlTypes.DATE);
        }
    }

}
