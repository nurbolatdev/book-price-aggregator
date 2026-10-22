FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Ubuntu-based JRE: Playwright's Chromium does not run on Alpine (musl)
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
COPY --from=build /app/target/*.jar app.jar
# Headless Chromium and its system libraries, installed by the Playwright version the app is built with;
# wget is for the compose healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && java -cp app.jar -Dloader.main=com.microsoft.playwright.CLI \
        org.springframework.boot.loader.launch.PropertiesLauncher install --with-deps --only-shell chromium \
    && rm -rf /var/lib/apt/lists/* /tmp/*
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
