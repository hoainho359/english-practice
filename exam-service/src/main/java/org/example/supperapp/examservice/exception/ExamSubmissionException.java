package org.example.supperapp.examservice.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ExamSubmissionException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public ExamSubmissionException(int code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
