package com.hpursan.sanlam_assessment.exception;

public class WithdrawalFailedException extends RuntimeException {
    public WithdrawalFailedException(String message) {
        super(message);
    }

    public WithdrawalFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
