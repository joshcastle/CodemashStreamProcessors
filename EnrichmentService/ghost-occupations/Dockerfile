FROM openjdk:17
ENV PORT 8080

EXPOSE 8080

COPY ./target/*.jar /usr/app/app.jar

WORKDIR /usr/app

CMD ["java", "-jar", "app.jar"]