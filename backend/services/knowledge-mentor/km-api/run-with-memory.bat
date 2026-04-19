@echo off
cd /d "%~dp0"
set MAVEN_OPTS=-Xmx3g -Xms1g
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx3g -Xms1g -XX:+UseG1GC"
