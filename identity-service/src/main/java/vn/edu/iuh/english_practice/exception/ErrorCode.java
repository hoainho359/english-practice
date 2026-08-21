package vn.edu.iuh.english_practice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error"),
    NOT_AUTHORIZE(1111, "not authorize error"),
    UNAUTHORIZED(1007, "You do not have permission"),
    UNAUTHENTICATED(1006, "Unauthenticated"),
    NOT_FOUND(1000, "Router not found"),
    WRONG_ACCOUNT(1001, "wrong username or password error")
    ;
    private String message;
    private int code;

    ErrorCode(int code,String message) {
        this.message = message;
        this.code = code;
    }
}
