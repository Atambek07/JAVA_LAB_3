package kg.course.library.web;

import kg.course.library.Loan;

import java.time.LocalDate;

/**
 * Ответ API, представляющий выдачу книги.
 */
public record LoanResponse(
        long id,
        String isbn,
        String cardNumber,
        LocalDate issuedOn,
        LocalDate dueOn,
        LocalDate returnedOn,
        boolean overdue
) {

    /**
     * Преобразует доменный объект {@link Loan} в ответ API.
     *
     * @param loan  выдача из доменной модели
     * @param today текущая дата (берётся из {@code java.time.Clock})
     */
    public static LoanResponse from(Loan loan, LocalDate today) {
        return new LoanResponse(
                loan.id(),
                loan.isbn().value(),
                loan.cardNumber(),
                loan.issuedOn(),
                loan.dueOn(),
                loan.returnedOn(),
                loan.isOverdue(today)
        );
    }
}
