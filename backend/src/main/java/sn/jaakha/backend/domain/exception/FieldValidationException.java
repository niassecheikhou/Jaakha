package sn.jaakha.backend.domain.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class FieldValidationException extends ApiException {
    private final Map<String, String> fields;

    public FieldValidationException(Map<String, String> fields) {
        super(HttpStatus.BAD_REQUEST, "Donnees invalides.");
        this.fields = fields;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
