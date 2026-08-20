package tz.go.tirdo.teltp.common.exception;

/** Thrown when a domain invariant or workflow rule is violated. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
