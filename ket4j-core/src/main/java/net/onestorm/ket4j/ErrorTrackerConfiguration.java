package net.onestorm.ket4j;

import net.onestorm.ket4j.sanitizer.AwsApiKeySanitizer;
import net.onestorm.ket4j.sanitizer.BearerTokenSanitizer;
import net.onestorm.ket4j.sanitizer.BsnSanitizer;
import net.onestorm.ket4j.sanitizer.DsnPasswordSanitizer;
import net.onestorm.ket4j.sanitizer.EmailSanitizer;
import net.onestorm.ket4j.sanitizer.Ipv4Sanitizer;
import net.onestorm.ket4j.sanitizer.JwtSanitizer;
import net.onestorm.ket4j.sanitizer.PathSanitizer;
import net.onestorm.ket4j.sanitizer.Sanitizer;
import net.onestorm.ket4j.sanitizer.StripeApiKeySanitizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ErrorTrackerConfiguration {

    private final String kendoUrl;
    private final String projectId;
    private final String token;
    private final String environment;
    private final String release;
    private final double connectTimeoutSeconds;
    private final double timeoutSeconds;
    private final List<Sanitizer> sanitizers;

    private ErrorTrackerConfiguration(Builder builder) {
        this.kendoUrl = builder.kendoUrl;
        this.projectId = builder.projectId;
        this.token = builder.token;
        this.environment = builder.environment;
        this.release = builder.release;
        this.connectTimeoutSeconds = builder.connectTimeoutSeconds;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.sanitizers = List.copyOf(builder.sanitizers);
    }

    public String getKendoUrl() {
        return kendoUrl;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getToken() {
        return token;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getRelease() {
        return release;
    }

    public double getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public double getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public List<Sanitizer> getSanitizers() {
        return sanitizers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String kendoUrl;
        private String projectId;
        private String token;
        private String environment = "production";
        private String release = null;
        private double connectTimeoutSeconds = 2.0;
        private double timeoutSeconds = 5.0;
        private List<Sanitizer> sanitizers = defaultSanitizers();

        private Builder() {
        }

        public Builder kendoUrl(String kendoUrl) {
            this.kendoUrl = kendoUrl;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder release(String release) {
            this.release = release;
            return this;
        }

        public Builder connectTimeoutSeconds(double connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        public Builder timeoutSeconds(double timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder sanitizers(List<Sanitizer> sanitizers) {
            this.sanitizers = sanitizers;
            return this;
        }

        public ErrorTrackerConfiguration build() {
            if (kendoUrl == null || kendoUrl.isBlank()) {
                throw new IllegalArgumentException("kendoUrl is required");
            }
            if (projectId == null || projectId.isBlank()) {
                throw new IllegalArgumentException("projectId is required");
            }
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException("token is required");
            }
            if (connectTimeoutSeconds <= 0) {
                throw new IllegalArgumentException("connectTimeoutSeconds must be > 0");
            }
            if (timeoutSeconds <= 0) {
                throw new IllegalArgumentException("timeoutSeconds must be > 0");
            }
            return new ErrorTrackerConfiguration(this);
        }

        private static List<Sanitizer> defaultSanitizers() {
            List<Sanitizer> sanitizers = new ArrayList<>();
            sanitizers.add(new JwtSanitizer());
            sanitizers.add(new BearerTokenSanitizer());
            sanitizers.add(new DsnPasswordSanitizer());
            sanitizers.add(new StripeApiKeySanitizer());
            sanitizers.add(new AwsApiKeySanitizer());
            sanitizers.add(new Ipv4Sanitizer());
            sanitizers.add(new EmailSanitizer());
            sanitizers.add(new BsnSanitizer());
            sanitizers.add(new PathSanitizer(null));
            return sanitizers;
        }
    }
}
