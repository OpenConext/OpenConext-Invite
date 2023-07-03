import {isEmpty} from "./Utils";

export const organisationName = (role, providers) => {
    const provider = providers.find(prov => prov.id === role.role.manageId);
    return provider ? ` (${provider.data.metaDataFields["OrganizationName:en"]})` : "";
}


export const providerInfo = provider => {
    if (isEmpty(provider)) {
        return {
            data: {
                metaDataFields: {
                    "OrganizationName:en": "",
                    provisioning_type: "",
                    "name:en": "Unknown in Manage"
                }
            }
        }
    }
    return provider;
}

