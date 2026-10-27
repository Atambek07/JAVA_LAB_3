package kg.course.library;

import kg.course.library.config.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты REST API (MockMvc + @SpringBootTest).
 * <p>
 * Свойство {@code library.import-file} в тестах указывает на несуществующий файл,
 * чтобы тесты не зависели от содержимого {@code books.csv} и создавали собственные
 * данные. Дата «сегодня» фиксирована (2026-03-20) через {@link TestClockConfig}.
 * </p>
 */
@SpringBootTest(properties = "library.import-file=absent-books.csv")
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LibraryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Library library;

    // ------------------------------------------------------------------
    // Задание 4. Книги
    // ------------------------------------------------------------------

    @Test
    void createBook_returns201AndLocation() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "978-5-4461-0914-9",
                                  "title": "Java. Эффективное программирование",
                                  "authors": ["Джошуа Блох"],
                                  "year": 2019,
                                  "copies": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/books/9785446109149"))
                .andExpect(jsonPath("$.isbn").value("9785446109149"))
                .andExpect(jsonPath("$.title").value("Java. Эффективное программирование"))
                .andExpect(jsonPath("$.totalCopies").value(2))
                .andExpect(jsonPath("$.availableCopies").value(2));
    }

    @Test
    void createBook_duplicate_returns409() throws Exception {
        createBook("978-5-17-090630-2", "Война и мир");
        createBook("978-5-17-090630-2", "Война и мир")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void createBook_blankTitle_returns400WithTitleInErrors() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "978-5-4461-0960-6",
                                  "title": "   ",
                                  "authors": ["Роберт Мартин"],
                                  "year": 2019,
                                  "copies": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")));
    }

    @Test
    void createBook_invalidIsbn_returns400() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isbn": "123-456",
                                  "title": "Книга с неверным ISBN",
                                  "authors": ["Автор"],
                                  "year": 2020,
                                  "copies": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getBook_byIsbn_returns200() throws Exception {
        createBook("978-5-4461-0914-9", "Java. Эффективное программирование");
        mockMvc.perform(get("/api/books/978-5-4461-0914-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9785446109149"))
                .andExpect(jsonPath("$.title").value("Java. Эффективное программирование"));
    }

    @Test
    void getBook_notFound_returns404ProblemJson() throws Exception {
        mockMvc.perform(get("/api/books/978-0-00-000000-0"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Книга с ISBN 9780000000000 не найдена в каталоге"));
    }

    @Test
    void searchBooks_byTitle_returnsMatching() throws Exception {
        createBook("978-5-4461-0914-9", "Java. Эффективное программирование");
        mockMvc.perform(get("/api/books").param("q", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Java. Эффективное программирование"));
    }

    @Test
    void deleteBook_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/books/978-0-00-000000-0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBook_withActiveLoans_returns409() throws Exception {
        createBook("978-5-4461-1101-2", "Java. Библиотека профессионала");
        registerReader("R-20001", "Guest 1", "GUEST");
        issueBook("R-20001", "978-5-4461-1101-2");

        mockMvc.perform(delete("/api/books/978-5-4461-1101-2"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ------------------------------------------------------------------
    // Задание 5. Читатели и выдачи
    // ------------------------------------------------------------------

    @Test
    void registerReader_returns201AndLocation() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "R-20010",
                                  "fullName": "Иван Петров",
                                  "category": "TEACHER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/readers/R-20010"))
                .andExpect(jsonPath("$.cardNumber").value("R-20010"))
                .andExpect(jsonPath("$.category").value("TEACHER"));
    }

    @Test
    void registerReader_duplicate_returns409() throws Exception {
        registerReader("R-20011", "Пётр Сидоров", "STUDENT");
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "R-20011",
                                  "fullName": "Пётр Сидоров",
                                  "category": "STUDENT"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void registerReader_badCardNumber_returns400() throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "XYZ",
                                  "fullName": "Иван Петров",
                                  "category": "TEACHER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("cardNumber")));
    }

    @Test
    void getReader_byCardNumber_returns200() throws Exception {
        registerReader("R-20012", "Мария Иванова", "STUDENT");
        mockMvc.perform(get("/api/readers/R-20012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value("R-20012"));
    }

    @Test
    void getReader_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/readers/R-99999"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Задание 5. Выдачи: сценарий выдать и вернуть
    // ------------------------------------------------------------------

    @Test
    void issueAndReturnBook_success() throws Exception {
        createBook("978-5-4461-0960-6", "Чистый код");
        registerReader("R-20020", "Гость", "GUEST");

        MvcResult issue = mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "R-20020",
                                  "isbn": "978-5-4461-0960-6"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andReturn();

        String location = issue.getResponse().getHeader("Location");
        String loanId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/loans/{id}/return", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedOn").value("2026-03-20"))
                .andExpect(jsonPath("$.overdue").value(false));
    }

    @Test
    void issueBook_overLimit_returns409() throws Exception {
        createBook("978-5-4461-0914-9", "Java. Эффективное программирование");
        createBook("978-5-17-090630-2", "Война и мир");
        registerReader("R-20021", "Гость с лимитом 1", "GUEST");

        issueBook("R-20021", "978-5-4461-0914-9");

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cardNumber": "R-20021",
                                  "isbn": "978-5-17-090630-2"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void loans_overdueFilter_returnsOnlyOverdueOnFixedDate() throws Exception {
        // Сеем данные через доменную модель: выдаём книгу гостью 01.03.2026
        // (срок возврата — 08.03.2026), на фиксированную дату 20.03.2026 она просрочена.
        Isbn isbn = new Isbn("978-5-17-090630-2");
        library.addBook(new Book(isbn, "Война и мир", java.util.List.of("Л. Н. Толстой"), 2015), 1);
        library.registerReader(new Reader("R-20030", "Гость", ReaderCategory.GUEST));
        library.issueBook("R-20030", isbn, LocalDate.of(2026, 3, 1));

        mockMvc.perform(get("/api/loans").param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].overdue").value(true));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    // ------------------------------------------------------------------
    // Задание 6. Отчёты
    // ------------------------------------------------------------------

    @Test
    void reports_top_nLessThanOne_returns400() throws Exception {
        mockMvc.perform(get("/api/reports/top").param("n", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/reports/top").param("n", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reports_top_defaultN_returns200() throws Exception {
        mockMvc.perform(get("/api/reports/top"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/top").param("n", "3"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/authors"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/by-category"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Обработка ошибок: безопасный ответ 500
    // ------------------------------------------------------------------

    @Test
    void internalError_returns500WithoutExceptionDetails() throws Exception {
        mockMvc.perform(get("/api/test-boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Внутренняя ошибка сервера"))
                .andExpect(content().string(not(containsString("секретный текст"))));
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions createBook(String isbn, String title) throws Exception {
        return mockMvc.perform(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isbn\": \"" + isbn + "\", \"title\": \"" + title + "\","
                        + " \"authors\": [\"Автор\"], \"year\": 2020, \"copies\": 1}"));
    }

    private void registerReader(String cardNumber, String fullName, String category) throws Exception {
        mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\": \"" + cardNumber + "\", \"fullName\": \"" + fullName
                                + "\", \"category\": \"" + category + "\"}"))
                .andExpect(status().isCreated());
    }

    private void issueBook(String cardNumber, String isbn) throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cardNumber\": \"" + cardNumber + "\", \"isbn\": \"" + isbn + "\"}"))
                .andExpect(status().isCreated());
    }
}
