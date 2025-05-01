FROM docker.klnsdr.com/nyx-cli:1.3 as builder

WORKDIR /app

COPY . .

RUN nyx build

FROM gcr.io/distroless/java21

WORKDIR /app

COPY --from=builder /app/build/nyx-1.4.jar /app/app.jar

EXPOSE 5000

CMD ["app.jar"]