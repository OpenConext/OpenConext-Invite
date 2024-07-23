import {mergeProvidersProvisioningsRoles} from "../../utils/Manage";
import {AUTHORITIES} from "../../utils/UserRole";

const applications = {
    "provisionings": [
        {
            "scim_user": "user",
            "scim_update_role_put_method": true,
            "entityid": "https://scim",
            "type": "provisioning",
            "scim_password": "secret",
            "name:en": "Scim",
            "id": "7",
            "scim_url": "http://localhost:8081/api/scim/v2",
            "provisioning_type": "scim",
            "_id": "7",
            "applications": [
                {
                    "id": "1",
                    "type": "saml20_sp"
                },
                {
                    "id": "5",
                    "type": "oidc10_rp"
                }
            ]
        },
        {
            "scim_user": "user",
            "scim_update_role_put_method": false,
            "entityid": "https://scim-patch",
            "type": "provisioning",
            "scim_password": "secret",
            "name:en": "Scim-Patch",
            "id": "8",
            "scim_url": "http://localhost:8081/api/scim/v2",
            "scim_user_identifier": "subject_id",
            "provisioning_type": "scim",
            "_id": "8",
            "applications": [
                {
                    "id": "4",
                    "type": "saml20_sp"
                }
            ]
        },
        {
            "graph_client_id": "client_id",
            "entityid": "https://graph",
            "type": "provisioning",
            "graph_tenant": "tenant",
            "name:en": "graph",
            "id": "9",
            "graph_url": "http://localhost:8081/graph/users",
            "provisioning_type": "graph",
            "graph_secret": "secret",
            "_id": "9",
            "applications": [
                {
                    "id": "2",
                    "type": "saml20_sp"
                },
                {
                    "id": "6",
                    "type": "oidc10_rp"
                }
            ]
        },
        {
            "entityid": "https://eva",
            "type": "provisioning",
            "name:en": "EVA",
            "id": "10",
            "eva_guest_account_duration": 30,
            "eva_token": "secret",
            "provisioning_type": "eva",
            "eva_url": "http://localhost:8081/eva",
            "_id": "10",
            "applications": [
                {
                    "id": "3",
                    "type": "saml20_sp"
                }
            ]
        }
    ],
    "providers": [
        {
            "entityid": "https://calendar",
            "type": "oidc10_rp",
            "url": "https://default-url-calendar.org",
            "name:nl": "Calendar NL",
            "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
            "OrganizationName:en": "SURF bv",
            "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
            "name:en": "Calendar EN",
            "id": "5",
            "_id": "5"
        },
        {
            "entityid": "https://cloud",
            "type": "oidc10_rp",
            "url": "https://default-url-cloud.org",
            "name:nl": "Cloud NL",
            "OrganizationName:en": "SURF bv",
            "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
            "name:en": "Cloud EN",
            "id": "6",
            "_id": "6"
        },
        {
            "entityid": "https://wiki",
            "type": "saml20_sp",
            "url": "https://default-url-wiki.org",
            "name:nl": "Wiki NL",
            "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
            "OrganizationName:en": "SURF bv",
            "name:en": "Wiki EN",
            "id": "1",
            "_id": "1"
        },
        {
            "entityid": "https://network",
            "type": "saml20_sp",
            "url": "https://default-url-network.org",
            "name:nl": "Network NL",
            "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
            "OrganizationName:en": "SURF bv",
            "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
            "name:en": "Network EN",
            "id": "2",
            "_id": "2"
        },
        {
            "entityid": "https://storage",
            "type": "saml20_sp",
            "url": "https://default-url-storage.org",
            "name:nl": "Storage NL",
            "OrganizationName:en": "SURF bv",
            "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
            "name:en": "Storage EN",
            "id": "3",
            "_id": "3"
        },
        {
            "entityid": "https://research",
            "type": "saml20_sp",
            "url": "https://default-url-research.org",
            "name:nl": "Research NL",
            "OrganizationName:en": "SURF bv",
            "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
            "name:en": "Research EN",
            "id": "4",
            "_id": "4"
        }
    ]
}

const roles = [
    {
        "id": 3915,
        "name": "Wiki",
        "shortName": "wiki",
        "description": "Wiki desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "3e267427-e2cd-4b9e-9a78-9248bede1bc4",
        "userRoleCount": 2,
        "applicationUsages": [
            {
                "id": 4574,
                "landingPage": "http://landingpage.com",
                "application": {
                    "id": 3901,
                    "manageId": "1",
                    "manageType": "SAML20_SP",
                    "landingPage": "http://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "http://landingpage.com",
                "entityid": "https://wiki",
                "type": "saml20_sp",
                "url": "https://default-url-wiki.org",
                "name:nl": "Wiki NL",
                "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
                "OrganizationName:en": "SURF bv",
                "name:en": "Wiki EN",
                "id": "1",
                "_id": "1"
            }
        ]
    },
    {
        "id": 3916,
        "name": "Network",
        "shortName": "network",
        "description": "Network desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "3cec7986-ca0c-4750-a6f3-86f2ff7fb82f",
        "userRoleCount": 0,
        "applicationUsages": [
            {
                "id": 4575,
                "landingPage": "http://landingpage.com",
                "application": {
                    "id": 3902,
                    "manageId": "2",
                    "manageType": "SAML20_SP",
                    "landingPage": "http://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "http://landingpage.com",
                "entityid": "https://network",
                "type": "saml20_sp",
                "url": "https://default-url-network.org",
                "name:nl": "Network NL",
                "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Network EN",
                "id": "2",
                "_id": "2"
            }
        ]
    },
    {
        "id": 3917,
        "name": "Storage",
        "shortName": "storage",
        "description": "Storage desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "9ac14a11-fd43-4f9c-abeb-8bf811f134df",
        "userRoleCount": 1,
        "applicationUsages": [
            {
                "id": 4576,
                "landingPage": "https://landingpage.com",
                "application": {
                    "id": 3903,
                    "manageId": "3",
                    "manageType": "SAML20_SP",
                    "landingPage": "https://landingpage.com"
                }
            },
            {
                "id": 4577,
                "landingPage": "https://landingpage.com",
                "application": {
                    "id": 3904,
                    "manageId": "6",
                    "manageType": "OIDC10_RP",
                    "landingPage": "https://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "https://landingpage.com",
                "entityid": "https://storage",
                "type": "saml20_sp",
                "url": "https://default-url-storage.org",
                "name:nl": "Storage NL",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Storage EN",
                "id": "3",
                "_id": "3"
            },
            {
                "landingPage": "https://landingpage.com",
                "entityid": "https://cloud",
                "type": "oidc10_rp",
                "url": "https://default-url-cloud.org",
                "name:nl": "Cloud NL",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Cloud EN",
                "id": "6",
                "_id": "6"
            }
        ]
    },
    {
        "id": 3918,
        "name": "Research",
        "shortName": "research",
        "description": "Research desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "f2c99710-0cd1-4ea2-a20c-14da698a866b",
        "userRoleCount": 1,
        "applicationUsages": [
            {
                "id": 4578,
                "landingPage": "http://landingpage.com",
                "application": {
                    "id": 3905,
                    "manageId": "4",
                    "manageType": "SAML20_SP",
                    "landingPage": "http://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "http://landingpage.com",
                "entityid": "https://research",
                "type": "saml20_sp",
                "url": "https://default-url-research.org",
                "name:nl": "Research NL",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Research EN",
                "id": "4",
                "_id": "4"
            }
        ]
    },
    {
        "id": 3919,
        "name": "Calendar",
        "shortName": "calendar",
        "description": "Calendar desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "75e4e7b6-5f98-45bc-9d10-9795316e6be0",
        "userRoleCount": 1,
        "applicationUsages": [
            {
                "id": 4579,
                "landingPage": "http://landingpage.com",
                "application": {
                    "id": 3906,
                    "manageId": "5",
                    "manageType": "OIDC10_RP",
                    "landingPage": "http://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "http://landingpage.com",
                "entityid": "https://calendar",
                "type": "oidc10_rp",
                "url": "https://default-url-calendar.org",
                "name:nl": "Calendar NL",
                "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Calendar EN",
                "id": "5",
                "_id": "5"
            }
        ]
    },
    {
        "id": 3920,
        "name": "Mail",
        "shortName": "mail",
        "description": "Mail desc",
        "defaultExpiryDays": 365,
        "enforceEmailEquality": false,
        "eduIDOnly": false,
        "blockExpiryDate": false,
        "overrideSettingsAllowed": false,
        "teamsOrigin": false,
        "identifier": "a2e7d57c-652c-430c-98e6-d5ac7050d979",
        "userRoleCount": 0,
        "applicationUsages": [
            {
                "id": 4580,
                "landingPage": "http://landingpage.com",
                "application": {
                    "id": 3906,
                    "manageId": "5",
                    "manageType": "OIDC10_RP",
                    "landingPage": "http://landingpage.com"
                }
            }
        ],
        "auditable": {
            "createdAt": 1721314647.000000000,
            "createdBy": "ResourceCleaner"
        },
        "applicationMaps": [
            {
                "landingPage": "http://landingpage.com",
                "entityid": "https://calendar",
                "type": "oidc10_rp",
                "url": "https://default-url-calendar.org",
                "name:nl": "Calendar NL",
                "institutionGuid": "ad93daef-0911-e511-80d0-005056956c1a",
                "OrganizationName:en": "SURF bv",
                "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
                "name:en": "Calendar EN",
                "id": "5",
                "_id": "5"
            }
        ]
    }
]

test("Test mergeProvidersProvisioningsRoles", () => {
    const results = mergeProvidersProvisioningsRoles(applications.providers, applications.provisionings, roles);
    expect(results.length).toEqual(6);
})
