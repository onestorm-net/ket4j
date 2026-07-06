package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.TestErrorEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathSanitizerTest {

    private String sanitizeMessage(PathSanitizer sanitizer, String message) {
        TestErrorEvent event = new TestErrorEvent(message);
        sanitizer.sanitize(event);
        return event.getMessage();
    }

    @Test
    void stripsBasePath() {
        PathSanitizer sanitizer = new PathSanitizer("/app/build/");
        assertThat(sanitizeMessage(sanitizer, "/app/build/net/onestorm/Foo.java"))
                .isEqualTo("net/onestorm/Foo.java");
    }

    @Test
    void stripsBasePathMultipleOccurrences() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizeMessage(sanitizer, "/app/Foo.java at /app/Bar.java"))
                .isEqualTo("Foo.java at Bar.java");
    }

    @Test
    void nullBasePathSkipsPathStripping() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizeMessage(sanitizer, "/opt/myapp/Foo.java")).isEqualTo("/opt/myapp/Foo.java");
    }

    @Test
    void blankBasePathSkipsPathStripping() {
        PathSanitizer sanitizer = new PathSanitizer("   ");
        assertThat(sanitizeMessage(sanitizer, "/opt/myapp/Foo.java")).isEqualTo("/opt/myapp/Foo.java");
    }

    @Test
    void redactsLinuxHomeUsername() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizeMessage(sanitizer, "/home/johndoe/project/Foo.java"))
                .isEqualTo("/home/[REDACTED:user]/project/Foo.java");
    }

    @Test
    void redactsMacUsersUsername() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizeMessage(sanitizer, "/Users/johndoe/project/Foo.java"))
                .isEqualTo("/Users/[REDACTED:user]/project/Foo.java");
    }

    @Test
    void stripsBasePathAndRedactsUsername() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizeMessage(sanitizer, "/app/src/Foo.java loaded by /home/johndoe/config"))
                .isEqualTo("src/Foo.java loaded by /home/[REDACTED:user]/config");
    }

    @Test
    void doesNotModifyUnrelatedInput() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizeMessage(sanitizer, "no paths here")).isEqualTo("no paths here");
    }
}
