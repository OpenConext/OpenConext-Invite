# Openconext-Access
# invite-server

[![Build Status](https://github.com/OpenConext/OpenConext-Access/actions/workflows/actions.yml/badge.svg)](https://github.com/SOpenConext/OpenConext-Access/actions/workflows/actions.yml/badge.svg)
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

### [Endpoints](#endpoint)

https://access.test2.surfconext.nl/ui/swagger-ui/index.html

https://mock.test2.surfconext.nl/

https://welcome.test2.surfconext.nl/

https://access.test2.surfconext.nl/

### [Mock](#mock)

If you want to use the mock-provisioning, add the following metadata in Manage.

SCIM:
```
"provisioning_type": "scim",
"scim_url": "https://mock.test2.surfconext.nl/api/scim/v2",
"scim_user": "user",
"scim_password": "secret",
"scim_update_role_put_method": true
```
eVA
```
"provisioning_type": "eva",
"eva_token": "secret",
"eva_guest_account_duration": 30
"eva_url": "https://mock.test2.surfconext.nl/eva",
```
Graph
```
"provisioning_type": "graph",
"graph_url": "https://mock.test2.surfconext.nl/graph/users",
"graph_client_id" : "client_id",
"graph_domain" : "hartingcollege.onmicrosoft.com",
"graph_secret" : "secret",
"graph_tenant": "tenant"
```

### [Local endpoints](#local-endpoint)

Login with Mujina IdP and user `admin` to become super-user in the local environment

http://localhost:8080/ui/swagger-ui/index.html#/system-controller/cron

http://localhost:8081/

http://localhost:4000/profile

http://localhost:3000
