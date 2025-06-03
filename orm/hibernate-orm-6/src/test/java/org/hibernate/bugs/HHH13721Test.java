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
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


@DomainModel(
        annotatedClasses = {
                HHH13721Test.Author.class, HHH13721Test.Book.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
                @Setting(name = AvailableSettings.FORMAT_SQL, value = "true")
        }
)
@SessionFactory
class HHH13721Test {

    @Test
    void hhh13721Test(SessionFactoryScope scope) {

        // prepare data
        scope.inTransaction(session -> {

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
        });

        // find and assert
        scope.inTransaction(session -> {

            Author author = session.createSelectionQuery("select a from Author a", Author.class).getSingleResult();
            assertNotNull(author);
            assertEquals("author", author.name);
            assertEquals(1, author.books.size());

            Book book = author.books.get(0);
            assertEquals("book", book.name);
            assertEquals(3, book.chapters.size());

            List<String> chapters = book.chapters.stream().map(c -> c.name).collect(Collectors.toList());
            assertTrue(chapters.contains("chapter1"));
            assertTrue(chapters.contains("chapter2"));
            assertTrue(chapters.contains("chapter3"));
        });
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
