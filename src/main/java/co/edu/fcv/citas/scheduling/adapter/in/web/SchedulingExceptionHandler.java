package co.edu.fcv.citas.scheduling.adapter.in.web;

import co.edu.fcv.citas.scheduling.domain.SchedulingException;
import co.edu.fcv.citas.shared.web.ApiExceptionHandler;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de agenda y citas a los códigos del contrato (400, 403, 404, 409). */
@RestControllerAdvice
class SchedulingExceptionHandler {
    @ExceptionHandler(SchedulingException.class)
    ProblemDetail scheduling(SchedulingException e) {
        HttpStatus status = switch (e) {
            case SchedulingException.InvalidData ignored -> HttpStatus.BAD_REQUEST;
            case SchedulingException.NotFound ignored -> HttpStatus.NOT_FOUND;
            case SchedulingException.Conflict ignored -> HttpStatus.CONFLICT;
            case SchedulingException.Forbidden ignored -> HttpStatus.FORBIDDEN;
        };
        return ApiExceptionHandler.problem(status, e.getMessage());
    }

    /** Un interbloqueo o espera de bloqueo vencida bajo concurrencia se informa como conflicto reintentable. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ProblemDetail lockFailure(PessimisticLockingFailureException e) {
        return ApiExceptionHandler.problem(HttpStatus.CONFLICT, "El horario está siendo reservado por otra persona; intente de nuevo");
    }
}
