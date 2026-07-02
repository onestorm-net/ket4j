package net.onestorm.ket4j.sanitizer;

@FunctionalInterface
public interface Sanitizer {

    String sanitize(String input);

    default String sanitize(String input, Throwable throwable) {
        return sanitize(input);
    }

}
