# Base image
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Optional: fix DNS inside container (for Alpine)
RUN echo "nameserver 8.8.8.8" > /etc/resolv.conf \
    && echo "nameserver 8.8.4.4" >> /etc/resolv.conf

# Copy your app jar
COPY target/ads-portal-be-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run your app
ENTRYPOINT ["java", "-jar", "app.jar"]
