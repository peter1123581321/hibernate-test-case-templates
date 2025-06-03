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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class HHH13721Test extends BaseCoreFunctionalTestCase {

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Author.class,
                Book.class
        };
    }

    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
    }

    @Test
    public void hhh13721Test() {

        Session s = openSession();

        Transaction tx = s.beginTransaction();

        Chapter c1 = new Chapter();
        c1.name = "chapter1";

        Chapter c2 = new Chapter();
        c2.name = "chapter2";

        Chapter c3 = new Chapter();
        c3.name = "chapter3";

        Book book = new Book();
        book.name = "book";
        book.chapters.add(c1);
        book.chapters.add(c2);
        book.chapters.add(c3);

        Author author = new Author();
        author.name = "author";
        author.books.add(book);

        session.persist(author);
        tx.commit();
        s.close();

        s = openSession();
        tx = s.beginTransaction();
        Author savedAuthor = session.createQuery("select a from Author a", Author.class).getSingleResult();
        assertNotNull(savedAuthor);
        assertEquals("author", savedAuthor.name);
        assertEquals(1, savedAuthor.books.size());

        Book savedBook = savedAuthor.books.get(0);
        assertEquals("book", savedBook.name);
        assertEquals(3, savedBook.chapters.size());

        List<String> chapters = savedBook.chapters.stream().map(c -> c.name).collect(Collectors.toList());
        assertTrue(chapters.contains("chapter1"));
        assertTrue(chapters.contains("chapter2"));
        assertTrue(chapters.contains("chapter3"));
        tx.commit();

        s.close();
    }

    @Entity(name = "Author")
    public static class Author {

        @Id
        @GeneratedValue
        long id;

        String name;

        @OneToMany(cascade = CascadeType.ALL)
        List<Book> books = new ArrayList<>();
    }

    @Entity(name = "Book")
    public static class Book {

        @Id
        @GeneratedValue
        long id;

        String name;

        @ElementCollection
        Collection<Chapter> chapters = new ArrayList<>();
    }

    @Embeddable
    public static class Chapter {

        String name;
    }
}
