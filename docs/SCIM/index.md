<!-- markdownlint-disable MD024 -->
# SCIM - System for Cross-domain Identity Management

De relevante gebruikers en hun groepslidmaatschappen worden via het SCIM
protocol doorgegeven aan de endpoints bij instellingen. De invite-applicatie
heeft de rol van SCIM client om informatie aan de verschillende Service
Providers te sturen.

## Begrippen

| Begrip | Omschrijving |
| --- | --- |
| Service Provider | Een applicatie bij de instelling, waar een gast-gebruiker toegang moet krijgen |
| SCIM client | De applicatie die de gebruikersinformatie naar de Service Providers stuurt; De Invite-applicatie backend |

## Authenticatie

De SCIM-endpoints kunnen per instelling op verschillende manieren worden beveiligd.
De invite-applicatie ondersteunt op dit moment de volgende authenticatiemethoden:

- **HTTP Basic Authentication**  
  De client stuurt een `Authorization: Basic ...` header met gebruikersnaam en
  wachtwoord.

- **Bearer token authenticatie**  
  De client stuurt een `Authorization: Bearer ...` header met een vooraf
  verstrekt token.

## Aanvragen

Stuur de gegevens voor het SCIM endpoint naar support@surfconext.nl. Vermeld in ieder geval:

- De omgeving waarop je aan wil sluiten: SURFconext productie of SURFconext test
- De volledige url van het SCIM endpoint
- Door welke instelling dit endpoint gebruikt gaat worden
- Welk SAML-attribuut of openid claim je als 'username' in het SAML bericht verwacht
- De username/wachtwoord voor basic auth of het bearer-token voor header authenticatie.
Voor productiekoppelingen het wachtwoord of token via een beveiligde dienst, zoals bijvoorbeeld SURFfilesender, verzenden.
- Voor welke (op SURFconext aangesloten) applicaties gebruiker en rollen moeten worden doorgegeven. Bij voorkeur het client-id of entity-id.
- Of het endpoint alleen gebruikers (zonder groepen) wil ontvangen.

## Acties

De endpoints bij de instellingen ondersteunen de volgende operaties:

- Create: POST `https://example.com/{v}/{resource}`
- Read: GET `https://example.com/{v}/{resource}/{id}`
- Replace: PUT `https://example.com/{v}/{resource}/{id}`
- Delete: DELETE `https://example.com/{v}/{resource}/{id}`
- Update: PATCH `https://example.com/{v}/{resource}/{id}`
- Search: GET `https://example.com/{v}/{resource}?filter={attribute}{op}{value}&sortBy={attributeName}&sortOrder={ascending|descending}`

De invite-applicatie roept alleen Create, Replace, Update en Delete aan; Read
en Search worden niet gebruikt.

PUT operaties leveren het complete object; PATCH operaties geven het verschil
met het huidige object door. [Zie rfc7644 section-3.5.2](https://datatracker.ietf.org/doc/html/rfc7644#section-3.5.2)

## Identifiers

Er zijn meerdere attributen die een gebruiker of groep identificeren:

- **id** : De identifier voor een gebruiker of groep bij de Service Provider
- **externalId** : De identifier binnen de gastenapplicatie
- **userName** : De identifier voor een gebruiker bij de Service Provider,
in het SCIM protocol de inlognaam voor de gebruiker als deze bij de Service
Provider in gaat loggen.

Voor de gebruikers die via de invite-applicatie beheerd worden, worden de
`userName` en `externalId` gevuld met een attribuut van de gebruiker, zodat ze
ook bij een SAML of oidc authenticatie herkend kunnen worden. Per endpoint kan
door SURFconext support met `scim_user_identifier` worden ingesteld welk attribuut gebruikt
wordt:

| `scim_user_identifier` | Attribuut |
| --- | --- |
| `eduperson_principal_name` (standaard) | eduPersonPrincipalName (eppn) |
| `subject_id` | subject_id (OIDC-claim) |
| `uids` | uids (SAML-attribuut) |
| `email` | e-mailadres van de gebruiker |
| `eduID` | het (institutionele) eduID-pseudoniem |

Als het gekozen attribuut leeg is, valt de invite-applicatie terug op de
eduPersonPrincipalName (eppn). Bij `scim_user_identifier` = `eduID`
provisioneert de invite-applicatie eerst het eduID van de gebruiker bij de
instelling en gebruikt de teruggekregen institutionele eduID-waarde als
`userName` en `externalId`. Voor gastgebruik met eduID heeft
[de eduID identifier](https://servicedesk.surf.nl/wiki/spaces/IAM/pages/128910006/Attributes+in+SURFconext#AttributesinSURFconext-eduIDeduID)
de voorkeur.

## Gebruikers

### Aanmaken gebruiker

Na het accepteren van de eerste uitnodiging van een instelling, moet de gebruiker aangemaakt
worden bij de instelling.

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
  "externalId":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "name":{
    "formatted":"Peter Havekes",
    "familyName":"Havekes",
    "givenName":"Peter"
  },
  "displayName": "Peter Havekes",
  "active": true,
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ],
  "phoneNumbers":[
    {
      "type":"other",
      "value":"+31600000000"
    }
  ]
}
```

Het `phoneNumbers`-attribuut bevat altijd een dummy-nummer
(`+31600000000`), omdat sommige systemen dit attribuut verplicht stellen.

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
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ]
}
```

De **{UserID at SP}** uit het antwoord wordt in de Invite-applicatie opgeslagen
bij de user, voor toekomstige updates van de gebruiker.

Als bij het aanmaken van de uitnodiging [via de API](https://invite.test.surfconext.nl/ui/swagger-ui/index.html#/invitation-controller/newInvitation)
gebruik is gemaakt van `invitesWithInternalPlaceholderIdentifiers`, dan zal de
waarde van `internalPlaceholderIdentifier` worden doorgegeven als
`"externalId": "{internalPlaceholderIdentifier}"` bij het POST bericht om een
gebruiker aan te maken.

### Update gebruiker

Als de gegevens van een gebruiker veranderd zijn (veranderde attributen tijdens de authenticatie),
dan sturen we een geupdate user-object naar alle service providers waar deze gebruiker bekend is.

#### Request

```curl
PUT /v1/Users/{UserID at SP}  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
  "externalId":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "name":{
    "formatted":"Peter Havekes-Nieuwenaam",
    "familyName":"Havekes-Nieuwenaam",
    "givenName":"Peter"
  },
  "id": "{UserID at SP}",
  "displayName": "Peter Havekes-Nieuwenaam",
  "active": true,
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ],
  "phoneNumbers":[
    {
      "type":"other",
      "value":"+31600000000"
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
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ]
}
```

### Verwijder gebruiker

Gebruikers worden na het verlopen van hun laatste rol bij een applicatie
verwijderd uit SURFconext Invite, en ook bij alle service providers waar de
gebruiker is aangemaakt. Eerst wordt de gebruiker uit alle groepen
verwijderd; het gebruiker-object zelf wordt alleen verwijderd bij de service
providers waar geen andere rollen meer van toepassing zijn.

#### Request

```curl
DELETE /v1/Users/{UserID at SP}  HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas":["urn:ietf:params:scim:schemas:core:2.0:User"],
  "externalId":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "userName":"c2cd7d6e-63fc-493a-8746-62fb2d3f8806",
  "name":{
    "formatted":"Peter Havekes",
    "familyName":"Havekes",
    "givenName":"Peter"
  },
  "id": "{UserID at SP}",
  "displayName": "Peter Havekes",
  "active": true,
  "emails":[
    {
      "type":"other",
      "value":"peter@gmail.com"
    }
  ],
  "phoneNumbers":[
    {
      "type":"other",
      "value":"+31600000000"
    }
  ]
}
```

#### Response

```curl
HTTP/1.1 200 OK
```

## Groepen

De rollen in de invite applicatie worden als groepen gepubliceerd naar de Service Provider.

### Aanmaken groep

Bij het aanmaken van een groep in de invite applicatie wordt deze direct verstuurd naar de instelling.

De `externalId` van een groep is de URN van de rol. Standaard wordt deze
opgebouwd uit het geconfigureerde URN-prefix, de identifier van de rol en de
naam van de rol (`{prefix}:{identifier}:{rolnaam}`). Voor rollen afkomstig
uit SURF Teams wordt de URN van de rol zelf gebruikt, en voor rollen uit het
CRM `urn:mace:surfnet.nl:surfnet.nl:sab:role:{rolnaam}`.

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
   ]
}
```

Bij het initieel aanmaken van een groep zal de lijst met `members` leeg zijn.

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
    ],
    "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
    "id": "{GroupID at SP}"
}
```

De **{GroupID at SP}** uit het antwoord wordt in de Invite-applicatie opgeslagen bij de user, voor toekomstige updates van de groep.

### Update groep (Gebruiker toevoegen/verwijderen of rolnaam wijzigen)

Als een gebruiker een uitnodiging accepteert, wordt de gebruiker eerst aangemaakt (met bovenstaand user bericht) als deze nog niet bestond.
Daarna wordt de gebruiker aan de bestaande groep toegevoegd door het hele groep-object (met alle leden) als update te sturen (PUT)
of door het verschil door te geven (PATCH). Ook wanneer de naam van de rol is
gewijzigd, wordt de groep bijgewerkt.

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
   "id": "{GroupID at SP}",
   "displayName":"WUR Brightspace gastdocent",
   "members":
   [
       {
          "value":"{UserID at SP}"
       },
       {
          "value":"{Other UserID at SP}"
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
  "Operations" : [ {
    "op" : "add",
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
  "Operations" : [ {
    "op" : "remove",
    "path" : "members",
    "value" : [ {
      "value" : "{UserID at SP}"
    } ]
  } ]
}
```

Als de naam van de rol is gewijzigd, wordt de nieuwe naam doorgegeven met een
`replace` operatie op `displayName`:

```curl
PATCH /v1/Groups/{GroupID at SP} HTTP/1.1
Accept: application/json
Authorization: Basic dXNlcjpwYXNzd29yZA==
Host: example.com
Content-Length: ...
Content-Type: application/json
{
  "schemas" : [ "urn:ietf:params:scim:api:messages:2.0:PatchOp" ],
  "Operations" : [ {
    "op" : "replace",
    "path" : "displayName",
    "value" : "Nieuwe rolnaam"
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
          "value":"{UserID at SP}"
       },
       {
          "value":"{Other UserID at SP}"
       }
    ],
    "externalId": "urn:collab:group:test.eduid.nl:wur.nl:brightspace:gastdocent",
    "id": "{GroupID at SP}"
}
```

### Verwijder groep

Als groepen worden verwijderd vanuit de invite applicatie wordt dit ook doorgegeven aan de service provider.

#### Request

```curl
DELETE /v1/Groups/{GroupID at SP}  HTTP/1.1
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
   "id": "{GroupID at SP}",
   "displayName":"WUR Brightspace gastdocent",
   "members":
   [
   ]
}
```

#### Response

```curl
HTTP/1.1 200 OK
```
