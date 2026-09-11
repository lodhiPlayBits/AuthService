package com.lodhi.auth.exceptions;

public class BlankFieldException extends RuntimeException {
    public BlankFieldException (String message){
        super(message);
    }

    public BlankFieldException(){
        super("field cannot be blank!!!");
    }

}
