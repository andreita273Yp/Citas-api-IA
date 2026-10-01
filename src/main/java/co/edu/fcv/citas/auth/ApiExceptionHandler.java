package co.edu.fcv.citas.auth;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(ApiException.class) ProblemDetail api(ApiException e){var p=ProblemDetail.forStatusAndDetail(e.status(),e.getMessage());p.setTitle(e.status().getReasonPhrase());return p;}
 @ExceptionHandler(MethodArgumentNotValidException.class) ProblemDetail invalid(MethodArgumentNotValidException e){var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Solicitud inválida");p.setTitle("Bad Request");return p;}
}
