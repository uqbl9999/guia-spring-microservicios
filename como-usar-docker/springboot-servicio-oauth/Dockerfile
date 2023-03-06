FROM openjdk:11
VOLUME /tmp
EXPOSE 9101
ADD target/springboot-servicio-oauth-0.0.1-SNAPSHOT.jar servicio-oauth.jar
ENTRYPOINT ["java","-jar","/servicio-oauth.jar"]