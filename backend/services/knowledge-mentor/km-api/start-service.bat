@echo off
cd /d "%~dp0"
set JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx2g -Xms1g"
