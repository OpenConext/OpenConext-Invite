# scimgateway

*Het heeft de voorkeur om SCIM berichten direct en near-realtime af te handelen in 
een eigen IDM of IAM platform. Als dit niet mogelijk is, kan bijvoorbeeld deze
[Scimgateway](https://github.com/jelhub/scimgateway) gebruikt worden. Deze 
software wordt niet gemaakt of ondersteund door SURF.*

## Wat is scimgateway

[Scimgateway](https://github.com/jelhub/scimgateway) is een open source
applicatie die SCIM berichten over personen en groepen kan ontvangen en
verwerken naar endpoints. Standaard ondersteunde endpoint zijn:

- Loki (NoSQL Document-Oriented Database)
- MongoDB (NoSQL Document-Oriented Database)
- SCIM (REST Webservice)
- Soap (SOAP Webservice)
- MSSQL (MSSQL Database)
- SAP HANA (SAP HANA Database)
- Entra ID (REST Webservices)
- LDAP (Directory)
- API (REST Webservices)

Nieuwe endpoints zijn relatief makkelijk te ontwikkelen.

## SURFconext Invite

Voor SURFconect Invite kan deze applicatie gebruikt worden om:

- Te testen met rollenbeheer vanuit SURFConext Invite zonder een echte
applicatie te koppelen
- Eenvoudig een SCIM koppeling naar een applicatie te realiseren, door een
eigen endpoint te maken specifiek voor de applicatie. Dit kan door de API's van
de applicatie aan te roepen of rechtstreeks gebruikers in de applicatiedatabase
aan te maken.
- Een SCIM koppelinhg te maken voor een legacy Identity Management systeem.
Door de gebruikers in een standaard database of LDAP te schrijven, kan een
niet-realtime IdM deze database periodiek uitlezen en verwerken.

## Installatie

### Docker

```bash
git clone https://github.com/jelhub/scimgateway.git
cd scimgateway/config/docker/
docker compose up
```

### Linux

Zorg dat node (18 of later) en npm geinstaleerd zijn op de server. [Zie de node.js website](https://nodejs.org/en/download/package-manager)

```bash
mkdir my-scimgateway
cd my-scimgateway
npm init -y
npm install scimgateway
node ./
```

### Windows

[Zie hier voor details](https://github.com/jelhub/scimgateway?tab=readme-ov-file#installation)

## Configuratie

In `index.js` staan alle beschikbare endpoints. Standaard staat alleen het loki
endpoint ingeschakeld, en is de rest uitgeschakeld met "//". Schakel de gewenste
 optie in door de "//" re verwijderen, bijvoorbeeld voor Microsoft SQL:

```javascript
// const loki = require('./lib/plugin-loki')
// const mongodb = require('./lib/plugin-mongodb')
// const scim = require('./lib/plugin-scim')
// const soap = require('./lib/plugin-soap') // prereq: npm install soap
const mssql = require('./lib/plugin-mssql')
// const saphana = require('./lib/plugin-saphana') // prereq: npm install hdb
// const entra = require('./lib/plugin-entra-id')
// const ldap = require('./lib/plugin-ldap')
// const api = require('./lib/plugin-api')
```

Daarna moet de config van het endpoint worden aangepast. Daarvoor vind je in de
config directory een bestand met dezelfde naam als de plugin. In het geval van
bijvoorbeeld Microsoft SQL is dat `config\plugin-mssql.json`.

Deze configuratie bestaat uit twee delen. Het eerste deel (scimgateway) gaat
over het SCIM endpoint. Hier zijn belangrijk:

- port: De http-poort waarop de service luistert
- auth.basic : de inlognaam en wachtwoord die moet worden gebruikt om berichten
aan de scim-gateway te sturen
- certificate :  de gegevens van het ssl-certificaat om de service beveiligd te
laten communiceren (https)

Het tweede deel (endpoint) gaat over het endpoint wat de gebruikers en groepen
gaat ontvangen. De configuratie verschilt per endpoint. Voor bijvoorbeeld het
Microsoft SQL endpoint moeten de hostname van de server en de
authenticatiegegevens worden ingevuld.

[Zie hier voor details](https://github.com/jelhub/scimgateway?tab=readme-ov-file#configuration)
