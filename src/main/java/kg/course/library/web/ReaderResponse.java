package kg.course.library.web;

import kg.course.library.Reader;
import kg.course.library.ReaderCategory;

/**
 * Ответ API, представляющий читателя.
 */
public record ReaderResponse(
        String cardNumber,
        String fullName,
        ReaderCategory category
) {

    public static ReaderResponse from(Reader reader) {
        return new ReaderResponse(reader.cardNumber(), reader.fullName(), reader.category());
    }
}
