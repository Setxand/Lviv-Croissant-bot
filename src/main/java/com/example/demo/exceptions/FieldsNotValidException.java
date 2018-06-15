package com.example.demo.exceptions;

import java.util.ResourceBundle;

import static com.example.demo.constantEnum.Error.FIELDS_NOT_VALID;

public class FieldsNotValidException extends RuntimeException {
    public FieldsNotValidException() {
        super(ResourceBundle.getBundle("difMessages").getString(FIELDS_NOT_VALID.name()));
    }
}
