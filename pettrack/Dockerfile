FROM maven:3.9.6-eclipse-temurin-21

WORKDIR /app

COPY . /app

ENV SPRING_DATASOURCE_URL=jdbc:oracle:thin:@oracle.fiap.com.br:1521:ORCL
ENV SPRING_DATASOURCE_USERNAME=rm563719
ENV SPRING_DATASOURCE_PASSWORD=111206

RUN useradd -m -s /bin/bash appuser

USER appuser

EXPOSE 8080

CMD ["bash", "-c", "mvn clean package -DskipTests && java -jar target/*.jar"]