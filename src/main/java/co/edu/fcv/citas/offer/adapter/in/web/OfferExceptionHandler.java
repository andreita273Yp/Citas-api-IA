package co.edu.fcv.citas.offer.adapter.in.web;

import co.edu.fcv.citas.offer.domain.OfferException;
import co.edu.fcv.citas.shared.web.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de la oferta a los códigos del contrato (400, 404, 409). */
@RestControllerAdvice
class OfferExceptionHandler {
    @ExceptionHandler(OfferException.class)
    ProblemDetail offer(OfferException e) {
        HttpStatus status = switch (e) {
            case OfferException.InvalidData ignored -> HttpStatus.BAD_REQUEST;
            case OfferException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case OfferException.Duplicate ignored -> HttpStatus.CONFLICT;
        };
        return ApiExceptionHandler.problem(status, e.getMessage());
    }
}
