package tz.go.tirdo.teltp.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String type, Object id) {
        super("%s not found: %s".formatted(type, id));
    }
}
