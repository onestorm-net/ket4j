package net.onestorm.ket4j.sanitizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathSanitizerTest {

    @Test
    void stripsBasePath() {
        PathSanitizer sanitizer = new PathSanitizer("/app/build/");
        assertThat(sanitizer.sanitize("/app/build/net/onestorm/Foo.java"))
                .isEqualTo("net/onestorm/Foo.java");
    }

    @Test
    void stripsBasePathMultipleOccurrences() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizer.sanitize("/app/Foo.java at /app/Bar.java"))
                .isEqualTo("Foo.java at Bar.java");
    }

    @Test
    void nullBasePathSkipsPathStripping() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizer.sanitize("/opt/myapp/Foo.java")).isEqualTo("/opt/myapp/Foo.java");
    }

    @Test
    void blankBasePathSkipsPathStripping() {
        PathSanitizer sanitizer = new PathSanitizer("   ");
        assertThat(sanitizer.sanitize("/opt/myapp/Foo.java")).isEqualTo("/opt/myapp/Foo.java");
    }

    @Test
    void redactsLinuxHomeUsername() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizer.sanitize("/home/johndoe/project/Foo.java"))
                .isEqualTo("/home/[REDACTED:user]/project/Foo.java");
    }

    @Test
    void redactsMacUsersUsername() {
        PathSanitizer sanitizer = new PathSanitizer(null);
        assertThat(sanitizer.sanitize("/Users/johndoe/project/Foo.java"))
                .isEqualTo("/Users/[REDACTED:user]/project/Foo.java");
    }

    @Test
    void stripsBasePathAndRedactsUsername() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizer.sanitize("/app/src/Foo.java loaded by /home/johndoe/config"))
                .isEqualTo("src/Foo.java loaded by /home/[REDACTED:user]/config");
    }

    @Test
    void doesNotModifyUnrelatedInput() {
        PathSanitizer sanitizer = new PathSanitizer("/app/");
        assertThat(sanitizer.sanitize("no paths here")).isEqualTo("no paths here");
    }
}
