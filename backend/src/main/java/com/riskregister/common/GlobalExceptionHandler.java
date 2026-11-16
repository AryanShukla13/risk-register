package com.riskregister.common;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/** Maps every failure to an RFC 7807 problem body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record FieldProblem(String field, String message) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex) {
        List<FieldProblem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toProblem)
                .toList();
        return validationProblem(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mie && !mie.getPath().isEmpty()) {
            String field = mie.getPath().get(mie.getPath().size() - 1).getFieldName();
            String message = (mie instanceof InvalidFormatException ife && ife.getTargetType().isEnum())
                    ? "must be one of " + List.of(ife.getTargetType().getEnumConstants())
                    : "has an invalid value or type";
            return validationProblem(List.of(new FieldProblem(field, message)));
        }
        return validationProblem(List.of(new FieldProblem("body", "Request body is missing or malformed JSON")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> badParam(MethodArgumentTypeMismatchException ex) {
        String allowed = ex.getRequiredType() != null && ex.getRequiredType().isEnum()
                ? "must be one of " + List.of(ex.getRequiredType().getEnumConstants())
                : "has an invalid value";
        return validationProblem(List.of(new FieldProblem(ex.getName(), allowed)));
    }

    @ExceptionHandler(BadRequestException.class)
    ResponseEntity<ProblemDetail> badRequest(BadRequestException ex) {
        return validationProblem(List.of(new FieldProblem(ex.getField(), ex.getMessage())));
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ProblemDetail> conflict(BusinessRuleException ex) {
        return problem(HttpStatus.CONFLICT, "Business rule violated", ex.getMessage(), null);
    }

    private FieldProblem toProblem(FieldError fe) {
        return new FieldProblem(fe.getField(), fe.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldProblem> errors) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", "Request validation failed", errors);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail,
                                                  List<FieldProblem> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        if (errors != null) {
            pd.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(pd);
    }
}
