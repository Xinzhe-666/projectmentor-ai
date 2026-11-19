FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
COPY ledger.jar /app/ledger.jar
ENTRYPOINT ["java", "-jar", "/app/ledger.jar"]
