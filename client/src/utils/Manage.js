import {isEmpty} from "./Utils";

export const singleProviderToOption = provider => {
    const organisation = provider["OrganizationName:en"];
    const organisationValue = isEmpty(organisation) ? "" : ` (${organisation})`;
    return {
        value: provider.id,
        label: `${provider["name:en"]}${organisationValue}`,
        type: provider.type
    };
}

export const providersToOptions = providers => {
    return providers.map(provider => singleProviderToOption(provider));
}

export const deriveApplicationAttributes = (role, locale) => {
    const application = role.application;
    if (!isEmpty(application)) {
        role.applicationName = application[`name:${locale}`] || application["name:en"]
        role.applicationOrganizationName = application[`OrganizationName:${locale}`] || application["OrganizationName:en"];
        role.logo = application.logo;
    }
}

export const deriveRemoteApplicationAttributes = (application, locale) => {
    if (!isEmpty(application)) {
        application.name = application[`name:${locale}`] || application["name:en"]
        application.organizationName = application[`OrganizationName:${locale}`] || application["OrganizationName:en"];
    }
}

export const providerInfo = provider => {
    if (isEmpty(provider)) {
        return {
            "OrganizationName:en": "",
            provisioning_type: "",
            "name:en": "Unknown in Manage"
        }
    }
    return provider;
}

