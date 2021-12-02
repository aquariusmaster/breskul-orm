package com.anderb.breskulorm.exception;

public class OrmException extends RuntimeException {

    public OrmException(String message) {
        super(message);
    }

    public OrmException(String message, Throwable cause) {
        super(message, cause);
    }
}
