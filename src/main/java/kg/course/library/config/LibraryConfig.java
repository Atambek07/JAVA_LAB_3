package kg.course.library.config;

import kg.course.library.BookCsvImporter;
import kg.course.library.ImportResult;
import kg.course.library.ImportedBook;
import kg.course.library.Library;
import kg.course.library.LibraryReports;
import kg.course.library.Reader;
import kg.course.library.ReaderCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Конфигурация приложения «Библиотека»: объявляет единственный экземпляр
 * {@link Library}, системные часы {@link Clock} и начальную загрузку данных.
 */
@Configuration
public class LibraryConfig {

    private static final Logger log = LoggerFactory.getLogger(LibraryConfig.class);

    /** Единственный экземпляр библиотеки для всего приложения. */
    @Bean
    public Library library() {
        return new Library();
    }

    /**
     * Системные часы. Текущая дата во всех контроллерах определяется как
     * {@code LocalDate.now(clock)}, чтобы дату можно было переопределить в тестах.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Переиспользуемый компонент для отчётов и поиска по названию. */
    @Bean
    public LibraryReports libraryReports(Library library) {
        return new LibraryReports(library);
    }

    /**
     * Начальная загрузка данных при старте приложения:
     * импорт книг из CSV-файла и регистрация трёх читателей разных категорий.
     */
    @Bean
    public ApplicationRunner startupDataLoader(Library library,
                                               @Value("${library.import-file}") String importFile) {
        return args -> {
            importBooks(library, importFile);
            registerReaders(library);
        };
    }

    /**
     * Импортирует книги из файла, заданного свойством {@code library.import-file}.
     * <p>
     * Формат CSV (разделитель «;», первая строка — заголовок):
     * {@code isbn;title;authors;year;copies}<br>
     * Авторов в одном поле можно указывать через запятую.
     * </p>
     * <p>
     * Если файл отсутствует — выводится предупреждение в журнал, приложение
     * запускается с пустым каталогом. Ошибки отдельных строк не останавливают
     * импорт остальных корректных строк.
     * </p>
     */
    private void importBooks(Library library, String importFile) {
        Path file = Path.of(importFile);
        if (!Files.exists(file)) {
            log.warn("Файл импорта книг не найден: {} — приложение запускается с пустым каталогом",
                    file.toAbsolutePath());
            return;
        }

        try {
            ImportResult result = new BookCsvImporter().importBooks(file);
            for (ImportedBook imported : result.imported()) {
                library.addBook(imported.book(), imported.copies());
            }
            result.errors().forEach(error ->
                    log.warn("Строка импорта пропущена: {}", error));
            log.info("Импортировано книг: {} из файла {}", result.imported().size(), file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Не удалось прочитать файл импорта {}: {}", file.toAbsolutePath(), e.getMessage());
        }
    }

    /** Регистрирует трёх читателей разных категорий (студент, гость, преподаватель). */
    private void registerReaders(Library library) {
        library.registerReader(new Reader("R-00001", "Нуртилек Калыгулов", ReaderCategory.STUDENT));
        library.registerReader(new Reader("R-00002", "Алексей Смирнов", ReaderCategory.GUEST));
        library.registerReader(new Reader("R-00003", "Иван Петров", ReaderCategory.TEACHER));
        log.info("Регистрация читателей в каталоге: 3 (STUDENT, GUEST, TEACHER)");
    }
}
