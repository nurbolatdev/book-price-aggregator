FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -B clean package -DskipTests -q

# Ubuntu 24.04 (noble) JRE, pinned: Playwright's Chromium does not run on Alpine (musl) or on newer Ubuntu releases it does not support
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
RUN groupadd --system --gid 10001 kitapradar \
    && useradd --system --uid 10001 --gid kitapradar --create-home --home-dir /home/kitapradar kitapradar
COPY --from=build --chown=kitapradar:kitapradar /app/target/*.jar app.jar
# Headless Chromium and its system libraries, installed by the Playwright version the app is built with;
# wget is for the compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher install --with-deps --only-shell chromium \
    && chmod -R o+rX /ms-playwright \
    && rm -rf /var/lib/apt/lists/* /tmp/*
# Set after the installer above so that JVM is not affected
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
USER kitapradar
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health/liveness || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
