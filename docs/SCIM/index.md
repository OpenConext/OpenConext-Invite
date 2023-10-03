<!-- markdownlint-disable MD024 -->
# SCIM - System for Cross-domain Identity Management

De relevante gebruikers en hun groepslidmaatschappen worden via het SCIM protocol doorgegeven aan de endpoints bij instellingen.
De invite-applicatie heeft de rol van SCIM client om informatie aan de verschillende Service Providers te sturen.

## Begrippen

|   |   |
|---|---|
| Service Provider | Een applicatie bij de instelling, waar een gast-gebruiker toegang moet krijgen |
| SCIM client  | De applicatie die de gebruikersinformatie naar de Serice Providers stuurt; De Invite-applicatie backend |

## Akties

De endpoints bij de instellingen ondersteunen de volgende operaties:

- Create: POST `https://example.com/{v}/{resource}`
- Read: GET `https://example.com/{v}/{resource}/{id}`
- Replace: PUT `https://example.com/{v}/{resource}/{id}`
- Delete: DELETE `https://example.com/{v}/{resource}/{id}`
- Update: PATCH `https://example.com/{v}/{resource}/{id}`
- Search: GET `https://example.com/{v}/{resource}?ﬁlter={attribute}{op}{value}&sortBy={attributeName}&sortOrder={ascending|descending}`

PUT operaties leveren het complete object; PATCH operaties geven het verschil met het huidige object door. [Zie rfc7644 section-3.5.2](https://datatracker.ietf.org/doc/html/rfc7644#section-3.5.2)

## Identifiers

Er zijn meerdere attributen die een gebruiker of groep identificeren:

- **id** : De identifier voor een gebruiker of groep bij de Service Provider
- **externalId** : De identifier binnen de gastenapplicatie
- **userName** : De identifier voor een gebruiker bij de Service Provider,
in het SCIM protocol de inlognaam voor de gebruiker als deze bij de Service Provider in gaat loggen.

Voor de gebruikers die via de invite-applicatie beheerd worden, gebruiken we de eduPersonPrincipalName (eppn) voor de indentifiers,
zodat ze ook bij een SAML of oidc authenticatie herkend kunnen worden.

## Gebruikers

### Aanmaken gebruiker

Na het accepteren van de eerste uitnodiging van een instelling, moet de gebruiker aangemaakt worden bij de instelling.

#### Request

```curl
POST /v1/Users  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
  "externalId":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "name":{
    "familyName":"Havekes",
    "givenName":"Peter"
  },
  "displayName": "Peter Havekes",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmnail.com"
    }
  ]
}
```

#### Response

```curl
HTTP/1.1 201 Created
Content-Type: application/scim+json
Location: https://example.com/v1/Users/{UserID at SP}
{
  "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:User",
      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  ],  
  "displayName": "Peter Havekes",
  "meta": {
    "created": "2021-12-22T12:34:56Z",
    "location": "https://example.com/v1/Users/{UserID at SP}",
    "lastModified": "2021-12-22T12:34:56Z",
    "resourceType": "User"
  },
  "name":{
    "familyName":"Havekes",
    "givenName":"Peter"
  },
  "id": "{UserID at SP}",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmnail.com"
    }
  ]
}
```

De **{UserID at SP}** uit het antwoord wordt in de Invite-applicatie opgeslagen bij de user, voor toekomstige updates van de gebruiker.

### Update gebruiker

Als de gegevens van een gebruiker veranderd zijn (veranderde attributen tijdens de authenticatie),
dan sturen we een geupdate user-object naar alle service providers waar deze gebruiker bekend is.

#### Request

```curl
PUT /v1/users/{UserID at SP}  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
  "externalId":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "name":{
    "familyName":"Havekes-Nieuwenaam",
    "givenName":"Peter"
  },
  "id": "{UserID at SP}",
  "displayName": "Peter Havekes-Nieuwenaam",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmnail.com"
    }
  ]
}
```

#### Response

```curl
HTTP/1.1 200 OK
Content-Type: application/scim+json
Location: https://example.com/v1/Users/{UserID at SP}
{
  "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:User",
      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  ],  
  "displayName": "Peter Havekes-Nieuwenaam",
  "name":{
    "familyName":"Havekes-Nieuwenaam",
    "givenName":"Peter"
  },
  "meta": {
    "created": "2021-12-22T12:34:56Z",
    "location": "https://example.com/v1/Users/{UserID at SP}",
    "lastModified": "2021-12-22T20:34:56Z",
    "resourceType": "User"
  },
  "id": "{UserID at SP}",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806@eduid.nl",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ]
}
```

### Verwijder gebruiker

Niet actieve gebruikers worden na X dagen verwijderd uit de invite applicatie, en ook bij alle service providers waar de gebruiker is aangemaakt.

#### Request

```curl
DELETE /users/{UserID at SP}  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
```

#### Response

```curl
HTTP/1.1 200 OK
```

## Groepen

De rollen in de invite applicatie worden als rollen gepubliceerd naar de Service Provider.

### Aanmaken groep

Bij het aanmaken van een groep in de invite applicatie wordt deze direct verstuurd naar de instelling.

#### Request

```curl
POST /v1/Groups  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
   "schemas":
   [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
   ],
   "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
   "displayName":"WUR Brightspace gastdocent",
   "members":
   [
      {
         "value":"{UserID at SP}"
      }
   ]
}
```

Bij het aanmaken van een groep zal de lijst met `members` leeg zijn.

#### Response

```curl
HTTP/1.1 201 Created
Content-Type: application/json
Location: https://example.com/v1/Groups/{GroupID at SP}
{
    "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "displayName":"WUR Brightspace gastdocent",
    "meta": {
        "created": "2021-12-23T10:00:00Z",
        "location": "https://example.com/v1/Groups/{GroupID at SP}",
        "lastModified": "2021-12-23T10:00:00Z",
        "resourceType": "Group"
    },
    "members":
    [
       {
          "value":"{UserID at SP}"
       }
    ]
    "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
    "id": "{GroupID at SP}"
}
```

De **{GroupID at SP}** uit het antwoord wordt in de Invite-applicatie opgeslagen bij de user, voor toekomstige updates van de groep.

### Update groep (Gebruiker toevoegen/verwijderen)

Als een gebruiker een uitnodiging accepteert, wordt de gebruiker eerst aangemaakt (met bovenstaand user bericht) als deze nog niet bestond.
Daarna wordt de gebruiker aan de bestaande groep toegevoegd door het hele groep-object (met alle leden) als update te sturen (PUT)
of door het verschil door te geven (PATCH).

Per applicatie is in te stellen of groep-updates als PUT of PATCH verstuurd worden:

PUT operaties leveren het complete object; PATCH operaties geven het verschil met het huidige object door. [Zie rfc7644 section-3.5.2](https://datatracker.ietf.org/doc/html/rfc7644#section-3.5.2)

#### Request PUT

```curl
PUT /v1/Groups/{GroupID at SP} HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
   "schemas":
   [
      "urn:ietf:params:scim:schemas:core:2.0:Group"
   ],
   "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
   "id": "{GroupID at SP}"
   "displayName":"WUR Brightspace gastdocent",
   "members":
   [
      {
         "value":"{UserID at SP}",
         "externalId": "{Internal UserID at Invite-application}"
      },
      {
         "value":"{Other UserID at SP}",
         "externalId": "{Other Internal UserID at Invite-application}"
      }
   ]
}
```

#### Request PATCH

```curl
PATCH /v1/Groups/{GroupID at SP} HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
  "externalId" : "test.eduid.nl.uva.canvas.guest",
  "id" : "{GroupID at SP}",
  "Operations" : [ {
    "op" : "Add",
    "path" : "members",
    "value" : [ {
      "value" : "{UserID at SP}"
    } ]
  } ]
}
```

```curl
PATCH /v1/Groups/{GroupID at SP} HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
  "externalId" : "test.eduid.nl.uva.canvas.guest",
  "id" : "{GroupID at SP}",
  "Operations" : [ {
    "op" : "Remove",
    "path" : "members",
    "value" : [ {
      "value" : "{UserID at SP}"
    } ]
  } ]
}
```

#### Response

```curl
HTTP/1.1 200 OK
Content-Type: application/json
Location: https://example.com/v1/Groups/{GroupID at SP}
{
    "schemas": [
        "urn:ietf:params:scim:schemas:core:2.0:Group"
    ],
    "displayName":"WUR Brightspace gastdocent",
    "meta": {
        "created": "2021-12-23T10:00:00Z",
        "location": "https://example.com/v1/Groups/{GroupID at SP}",
        "lastModified": "2021-12-23T14:01:00Z",
        "resourceType": "Group"
    },
    "members":
    [
       {
          "value":"{UserID at SP}",
          "externalId": "{Internal UserID at Invite-application}"
       },
       {
          "value":"{Other UserID at SP}",
          "externalId": "{Other Internal UserID at Invite-application}"
       }
    ]
    "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
    "id": "{GroupID at SP}"
}
```

### Verwijder groep

Als groepen worden verwijderd vanuit de invite applicatie wordt dit ook doorgegeven aan de service provider.

#### Request

```curl
DELETE /groups/{GroupID at SP}  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
```

#### Response

```curl
HTTP/1.1 200 OK
```
