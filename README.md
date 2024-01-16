# Openconext-Invite

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

### [Endpoints](#endpoints)

<https://invite.test.surfconext.nl/ui/swagger-ui/index.html>

<https://mock.test.surfconext.nl/>

<https://welcome.test.surfconext.nl/>

<https://invite.test.surfconext.nl/>

### [Mock](#mock)

If you want to use the mock-provisioning, add the following metadata in Manage.

SCIM:

```json
"provisioning_type": "scim",
"scim_url": "https://mock.test.surfconext.nl/api/scim/v2",
"scim_user": "user",
"scim_password": "secret",
"scim_update_role_put_method": true
```

eVA

```json
"provisioning_type": "eva",
"eva_token": "secret",
"eva_guest_account_duration": 30
"eva_url": "https://mock.test.surfconext.nl/eva",
```

Graph

```json
"provisioning_type": "graph",
"graph_url": "https://mock.test.surfconext.nl/graph/users",
"graph_client_id" : "client_id",
"graph_domain" : "hartingcollege.onmicrosoft.com",
"graph_secret" : "secret",
"graph_tenant": "tenant"
```

### [Local endpoints](#local-endpoints)

Login with Mujina IdP and user `admin` to become super-user in the local environment

<http://localhost:8080/ui/swagger-ui/index.html>

<http://localhost:8081/>

<http://localhost:4000>

<http://localhost:3000>

### [Institution Admin](#institution-admin)

To become an institution admin in invite, add the following values as `eduPersonEntitlements` using Mujina:

- urn:mace:surfnet.nl:surfnet.nl:sab:organizationGUID:ad93daef-0911-e511-80d0-005056956c1a
- urn:mace:surfnet.nl:surfnet.nl:sab:role:SURFconextverantwoordelijke

### [Technical documentation](#technical-documentation)

<https://openconext.github.io/OpenConext-Invite/>

### Provisioning Secrets

The secrets (passwords / API-keys) used in provisionings are encrypted in OpenConext-Manage using keypairs.

#### Create private / public keypair

```bash
openssl genrsa -traditional -out private_key.pem 2048
openssl rsa -pubout -in private_key.pem -out public_key.pem
```

#### Convert private key to pkcs8 format in order to import it from Java

```bash
openssl pkcs8 -topk8 -in private_key.pem -inform pem -out private_key_pkcs8.pem -outform pem -nocrypt
```
