package kg.course.library.web;

import kg.course.library.Book;
import kg.course.library.Library;

import java.util.List;

/**
 * Ответ API, представляющий книгу каталога с количеством экземпляров.
 */
public record BookResponse(
        String isbn,
        String title,
        List<String> authors,
        int year,
        int totalCopies,
        int availableCopies
) {

    /**
     * Преобразует доменный объект {@link Book} в безопасный ответ API.
     * Доменные объекты наружу не возвращаются: наружу уходят только DTO.
     */
    public static BookResponse from(Book book, Library library) {
        return new BookResponse(
                book.isbn().value(),
                book.title(),
                book.authors(),
                book.year(),
                library.totalCopies(book.isbn()),
                library.availableCopies(book.isbn())
        );
    }
}
