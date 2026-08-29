package com.example.portfolio.exception;

import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);
	
	// 커스텀 예외를 모두 여기서 처리
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorDto> handleCustomException(CustomException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Internal application error [{}]", ex.getErrorCode().getCode(), ex);
            return ErrorDto.toResponseEntity(new CustomException(
                    ex.getStatus(), ex.getErrorCode(), "Request could not be completed"
            ));
        }
        return ErrorDto.toResponseEntity(ex);
    }
    
    // DB 관련 예외는 여기서 모두 처리
    @ExceptionHandler(DataAccessException.class) 
    protected ResponseEntity<ErrorDto> handleDataAccessException(DataAccessException ex) {
        log.error("Database operation failed", ex);
        CustomException customException = new CustomException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.DATABASE_ERROR,
            "Request could not be completed"
        );
        return ErrorDto.toResponseEntity(customException);
    }
    
    // 처리되지 않은 모든 예외를 처리
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorDto> handleAllException(Exception ex) {
        log.error("Unexpected application error", ex);
        CustomException customException = new CustomException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.UNKNOWN,
            "Request could not be completed"
        );
        return ErrorDto.toResponseEntity(customException);
    }
}
