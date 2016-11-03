FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/hiiop.jar /hiiop/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/hiiop/app.jar"]
