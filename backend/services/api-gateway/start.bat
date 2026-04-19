@echo off
echo ====================================
echo Starting API Gateway Service
echo ====================================
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
