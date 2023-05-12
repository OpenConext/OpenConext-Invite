# Openconext-Access
# invite-server

[![Build Status](https://github.com/OpenConext/OpenConext-Access/actions/workflows/maven.yml/badge.svg)](https://github.com/SOpenConext/OpenConext-Access/actions/workflows/maven.yml/badge.svg)
[![codecov](https://codecov.io/gh/OpenConext/OpenConext-Access/branch/main/graph/badge.svg?token=HZ7ES3TLQ9)](https://codecov.io/gh/OpenConext/OpenConext-Access)

## [Getting started](#getting-started)

### [System Requirements](#system-requirements)

- Java 17
- Maven 3

First install Java 17 with a package manager
and then export the correct the `JAVA_HOME`. For example on macOS:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home/
```

Then create the MySQL database:

```sql
DROP DATABASE IF EXISTS access;
CREATE DATABASE access CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'access'@'localhost' IDENTIFIED BY 'secret';
GRANT ALL privileges ON `access`.* TO 'access'@'localhost';
```

### [Building and running](#building-and-running)

This project uses Spring Boot and Maven. To run locally, type:

```bash
mvn spring-boot:run
```

To build and deploy (the latter requires credentials in your maven settings):

```bash
mvn clean deploy
```

### [Swagger](#swagger)

http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs