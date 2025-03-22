FROM gcr.io/distroless/java21

WORKDIR /app

COPY out/artifacts/nyx_jar/nyx.jar /app/app.jar

EXPOSE 5000

CMD ["app.jar"]