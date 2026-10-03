FROM --platform=$BUILDPLATFORM docker.klnsdr.com/nyx-cli:1.5 AS builder

WORKDIR /app

COPY . .

RUN nyx build

FROM gcr.io/distroless/java21

WORKDIR /app

COPY --from=builder /app/build/*.jar /app/app.jar

EXPOSE 5000

CMD ["app.jar"]