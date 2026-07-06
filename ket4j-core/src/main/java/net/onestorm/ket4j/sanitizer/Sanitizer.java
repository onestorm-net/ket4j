package net.onestorm.ket4j.sanitizer;

import net.onestorm.ket4j.ErrorEvent;

@FunctionalInterface
public interface Sanitizer {

    void sanitize(ErrorEvent event);

}
