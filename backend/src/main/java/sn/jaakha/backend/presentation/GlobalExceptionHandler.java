package sn.jaakha.backend.presentation;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sn.jaakha.backend.domain.exception.ApiException;
import sn.jaakha.backend.domain.exception.FieldValidationException;
import sn.jaakha.backend.presentation.response.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(FieldValidationException.class)
    ResponseEntity<ApiErrorResponse> handleFieldValidation(FieldValidationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(
                        Instant.now(),
                        exception.getStatus().value(),
                        exception.getMessage(),
                        exception.getFields()
                ));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(Instant.now(), exception.getStatus().value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (left, right) -> left));
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Donnees invalides.", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableJson(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Le corps de la requete est invalide. Verifiez le JSON et les valeurs envoyees.",
                        Map.of("request", "Le JSON doit etre bien forme. Les roles/statuts doivent utiliser les valeurs proposees par Swagger.")
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "Un parametre de requete est invalide.",
                        Map.of(exception.getName(), "La valeur '" + exception.getValue() + "' n'est pas acceptee pour ce parametre.")
                ));
    }
}
