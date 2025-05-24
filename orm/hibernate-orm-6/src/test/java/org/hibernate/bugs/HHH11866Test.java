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
import jakarta.persistence.Transient;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.MutationQuery;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DomainModel(
        annotatedClasses = {HHH11866Test.Document.class, Document2.class}
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
                @Setting(name = AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY,
                        value = "org.hibernate.bugs.HHH11866Test$EntityDirtinessStrategy")
        }
)
@SessionFactory
class HHH11866Test {

    @Test
    void hhh11866Test_bytecode(SessionFactoryScope scope) {

        // prepare document
        scope.inTransaction(session -> {

            MutationQuery nativeMutationQuery = session.createNativeMutationQuery(
                    "insert into Document2 (id,name) values (1,'title')");
            nativeMutationQuery.executeUpdate();

        });

        // assert document
        scope.inTransaction(session -> {

            final Document2 document = session.createQuery("select d from Document2 d", Document2.class)
                    .getSingleResult();
            assertNotNull(document);
            assertEquals("title", document.getName());

            assertFalse(session.isDirty());

            document.setName("test");

            assertTrue(session.isDirty());

            session.flush();

            assertFalse(session.isDirty());
        });
    }

    @Test
    void hhh11866Test_customStrategy(SessionFactoryScope scope) {

        // prepare document
        scope.inTransaction(session -> {

            MutationQuery nativeMutationQuery = session.createNativeMutationQuery(
                    "insert into Document (id,name) values (1,'title')");
            nativeMutationQuery.executeUpdate();

        });

        // assert document
        scope.inTransaction(session -> {

            final Document document = session.createQuery("select d from Document d", Document.class)
                    .getSingleResult();
            assertNotNull(document);
            assertEquals("title", document.getName());

            assertFalse(session.isDirty());

            document.setName("test");

            assertTrue(session.isDirty());

            session.flush();

            assertFalse(session.isDirty());
        });
    }

    public interface DirtyAware {

        Set<String> getDirtyProperties();

        void clearDirtyProperties();
    }

    @Entity(name = "Document")
    public static class Document extends SelfDirtyCheckingEntity {

        @Id
        @GeneratedValue
        Long id;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
            markDirtyProperty();
        }
    }

    public static class EntityDirtinessStrategy implements CustomEntityDirtinessStrategy {

        @Override
        public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session) {
            return entity instanceof DirtyAware;
        }

        @Override
        public boolean isDirty(Object entity, EntityPersister persister, Session session) {
            return !cast(entity).getDirtyProperties().isEmpty();
        }

        @Override
        public void resetDirty(Object entity, EntityPersister persister, Session session) {
            if (entity instanceof DirtyAware) {
                cast(entity).clearDirtyProperties();
            }
        }

        @Override
        public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext) {
            if (entity instanceof DirtyAware) {
                final DirtyAware dirtyAware = cast(entity);
                dirtyCheckContext.doDirtyChecking(
                        attributeInformation -> {
                            String propertyName = attributeInformation.getName();
                            return dirtyAware.getDirtyProperties().contains(propertyName);
                        }
                );
            }
        }

        private DirtyAware cast(Object entity) {
            return (DirtyAware) entity;
        }
    }

    public static abstract class SelfDirtyCheckingEntity implements DirtyAware {

        private final Map<String, String> setterToPropertyMap = new HashMap<>();

        @Transient
        private final Set<String> dirtyProperties = new LinkedHashSet<>();

        public SelfDirtyCheckingEntity() {
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(getClass());
                PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
                for (PropertyDescriptor descriptor : descriptors) {
                    Method setter = descriptor.getWriteMethod();
                    if (setter != null) {
                        setterToPropertyMap.put(setter.getName(), descriptor.getName());
                    }
                }
            } catch (IntrospectionException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Set<String> getDirtyProperties() {
            return dirtyProperties;
        }

        @Override
        public void clearDirtyProperties() {
            dirtyProperties.clear();
        }

        protected void markDirtyProperty() {
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            dirtyProperties.add(setterToPropertyMap.get(methodName));
        }
    }
}
