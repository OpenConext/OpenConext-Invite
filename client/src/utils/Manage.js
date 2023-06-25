import {isEmpty} from "./Utils";

export const singleProviderToOption = provider => {

    const organisation = provider.data.metaDataFields["OrganizationName:en"];
    const organisationValue = isEmpty(organisation) ? "" : ` (${organisation})`;
    return {
        value: provider.id,
        label: `${provider.data.metaDataFields["name:en"]}${organisationValue}`,
        type: provider.type
    };
}

export const providersToOptions = providers => {
    return providers.map(provider => singleProviderToOption(provider));
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

