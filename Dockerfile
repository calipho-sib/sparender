FROM anapsix/alpine-java
COPY dist/app.jar /home/app.jar
COPY config.properties config.properties
CMD ["java","-jar", "-Dehcache.path=/home/cache", "/home/app.jar"]
