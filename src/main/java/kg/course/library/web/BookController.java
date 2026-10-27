package kg.course.library.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.course.library.Book;
import kg.course.library.Isbn;
import kg.course.library.Library;
import kg.course.library.LibraryReports;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * REST-контроллер каталога книг.
 */
@RestController
@RequestMapping("/api/books")
@Tag(name = "Книги", description = "Управление каталогом книг")
public class BookController {

    private final Library library;
    private final LibraryReports reports;

    public BookController(Library library, LibraryReports reports) {
        this.library = library;
        this.reports = reports;
    }

    /** Список всех книг; при указании параметра {@code q} — поиск по названию. */
    @GetMapping
    @Operation(summary = "Список книг", description = "Возвращает все книги; при параметре q — поиск по названию (searchByTitle)")
    public List<BookResponse> findAll(@RequestParam(value = "q", required = false) String q) {
        List<Book> books = (q == null || q.isBlank())
                ? library.findAllBooks()
                : reports.searchByTitle(q);
        return books.stream()
                .map(book -> BookResponse.from(book, library))
                .toList();
    }

    /** Книга по ISBN. */
    @GetMapping("/{isbn}")
    @Operation(summary = "Книга по ISBN")
    public BookResponse findByIsbn(@PathVariable String isbn) {
        Book book = library.getBook(new Isbn(isbn));
        return BookResponse.from(book, library);
    }

    /**
     * Добавление книги. Повторное создание книги с тем же ISBN — 409 Conflict.
     */
    @PostMapping
    @Operation(summary = "Добавить книгу")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        Isbn isbn = new Isbn(request.isbn());
        if (library.findBook(isbn).isPresent()) {
            throw new IllegalStateException("Книга с ISBN " + isbn.value() + " уже есть в каталоге");
        }

        Book book = new Book(isbn, request.title(), request.authors(), request.year());
        library.addBook(book, request.copies());

        BookResponse body = BookResponse.from(book, library);
        return ResponseEntity.created(URI.create("/api/books/" + isbn.value())).body(body);
    }

    /** Удаление книги; если у книги есть активные выдачи — 409 Conflict. */
    @DeleteMapping("/{isbn}")
    @Operation(summary = "Удалить книгу")
    public ResponseEntity<Void> delete(@PathVariable String isbn) {
        library.removeBook(new Isbn(isbn));
        return ResponseEntity.noContent().build();
    }
}
