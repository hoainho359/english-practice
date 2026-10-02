package org.example.supperapp.examservice.exception;

import org.example.supperapp.examservice.dto.response.ApiResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(Exception exception) {
        log.error("Exception: ", exception);
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(value = PdfImportException.class)
    ResponseEntity<ApiResponse> handlingPdfImportException(PdfImportException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .code(2001)
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(ExamSubmissionException.class)
    ResponseEntity<ApiResponse> handlingExamSubmissionException(ExamSubmissionException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.builder()
                        .code(exception.getCode())
                        .message(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        ErrorCode errorCode = ErrorCode.PAYLOAD_TOO_LARGE;
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        return ResponseEntity.badRequest()
                .body(ApiResponse.builder()
                        .code(ErrorCode.INVALID_REQUEST.getCode())
                        .message(message)
                        .build());
    }

    @ExceptionHandler(TransientAiException.class)
    ResponseEntity<ApiResponse> handlingTransientAiException(TransientAiException exception) {
        log.warn("AI provider is temporarily unavailable: {}", exception.getMessage());
        if (exception.getMessage() != null && exception.getMessage().contains("429")) {
            return aiError(ErrorCode.AI_RATE_LIMITED);
        }
        return aiError(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(NonTransientAiException.class)
    ResponseEntity<ApiResponse> handlingNonTransientAiException(NonTransientAiException exception) {
        log.error("AI provider rejected the request", exception);
        return aiError(ErrorCode.AI_PROVIDER_ERROR);
    }

    private ResponseEntity<ApiResponse> aiError(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
