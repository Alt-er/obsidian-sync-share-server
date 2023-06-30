package com.alter.obsyncshare.advice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {


//    @ExceptionHandler(MaxUploadSizeExceededException.class)
//    public ResponseEntity<Object> handleMaxFileSizeExceeded(MaxUploadSizeExceededException ex) {
//        String errorMessage = "上传文件大小超出限制";
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
//    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        // 自定义处理逻辑
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}