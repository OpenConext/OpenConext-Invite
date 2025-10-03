# scimgateway

***LET OP!! We raden aan om SCIM berichten direct en near-realtime af te
handelen in een eigen IDM of IAM platform. !!**
Als dit niet mogelijk is, kan bijvoorbeeld deze [Scimgateway](https://github.com/jelhub/scimgateway)
gebruikt worden. **Deze software wordt niet gemaakt of ondersteund door SURF**.*

## Wat is scimgateway

[Scimgateway](https://github.com/jelhub/scimgateway) is een open source
applicatie die SCIM berichten over personen en groepen kan ontvangen en
verwerken naar endpoints. Scimgateway ondersteunt standaard verschillende 
soorten endpoints.

## SURFconext Invite

Voor SURFconext Invite kan deze applicatie gebruikt worden om:

- Te testen met rollenbeheer vanuit SURFconext Invite zonder een echte
applicatie te koppelen
- Een SCIM koppeling naar een applicatie te realiseren, door een eigen
endpoint te maken specifiek voor de applicatie. Dit kan door de API's van
de applicatie aan te roepen of rechtstreeks gebruikers in de applicatiedatabase
aan te maken.
- Een SCIM koppelinhg te maken voor een legacy Identity Management systeem.
Door de gebruikers in een standaard database of LDAP te schrijven, kan een
niet-realtime IdM deze database periodiek uitlezen en verwerken.

## Installatie en configuratie

Scimgateway biedt [instructies](https://github.com/jelhub/scimgateway?tab=readme-ov-file#installation) die ondersteunen bij
de installatie en [configuratie](https://github.com/jelhub/scimgateway?tab=readme-ov-file#configuration) van het product. SURF kan hier geen ondersteuning op bieden.
