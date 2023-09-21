import {isEmpty} from "./Utils";
import I18n from "../locale/I18n";

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

export const deriveApplicationAttributes = role => {
    const application = role.application;
    if (!isEmpty(application)) {
        const metaData = application.data.metaDataFields;
        role.applicationName = metaData[`name:${I18n.locale}`] || metaData["name:en"]
        role.applicationOrganizationName = metaData[`OrganizationName:${I18n.locale}`] || metaData["OrganizationName:en"];
        role.logo = metaData["logo:0:url"] ;
    }
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

