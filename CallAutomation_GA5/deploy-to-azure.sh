#!/bin/bash

# =========================
# CONFIGURATION VARIABLES
# =========================
RESOURCE_GROUP="JavaContosoGA5ResourceGroup"
APP_SERVICE_PLAN="JavaContosoGA5AppServicePlan"
APP_NAME="javaContosoGA5App"
LOCATION="eastus"
JAR_NAME="CallAutomation_GA5-1.0-SNAPSHOT.jar"
JAVA_VERSION="JAVA|17-java17"

# =========================
# BUILD THE SPRING BOOT APP
# =========================
echo "Building the Spring Boot application..."
mvn clean package || { echo "Maven build failed"; exit 1; }

# =========================
# AZURE LOGIN
# =========================
echo "Logging in to Azure..."
az login

# =========================
# CREATE RESOURCE GROUP
# =========================
echo "Creating resource group..."
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"

# =========================
# CREATE APP SERVICE PLAN
# =========================
echo "Creating App Service plan..."
az appservice plan create --name "$APP_SERVICE_PLAN" --resource-group "$RESOURCE_GROUP" --sku B1 --is-linux

# =========================
# CREATE WEB APP
# =========================
echo "Creating Web App..."
az webapp create --resource-group "$RESOURCE_GROUP" --plan "$APP_SERVICE_PLAN" --name "$APP_NAME" --runtime "$JAVA_VERSION"

# =========================
# DEPLOY THE JAR FILE
# =========================
echo "Deploying the JAR file to Azure App Service..."
az webapp deploy --resource-group "$RESOURCE_GROUP" --name "$APP_NAME" --src-path "./target/$JAR_NAME" --type jar

# =========================
# OUTPUT APP URL
# =========================
echo "Deployment complete!"
echo "Visit your Swagger UI at: https://$APP_NAME.azurewebsites.net/swagger-ui/index.html"
