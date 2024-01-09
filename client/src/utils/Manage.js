import {isEmpty, splitListSemantically} from "./Utils";
import {ReactComponent as MultipleIcon} from "../icons/multi-role.svg";

export const singleProviderToOption = provider => {
    const organisation = provider["OrganizationName:en"];
    const organisationValue = isEmpty(organisation) ? "" : ` (${organisation})`;
    const manageType = provider.type ? provider.type.toUpperCase() : provider.manageType;
    const manageId = provider.id || provider.manageId;
    return {
        value: manageId,
        label: `${provider["name:en"]}${organisationValue}`,
        type: manageType,
        manageType: manageType,
        manageId: manageId,
        url: provider.url,
        landingPage: provider.landingPage
    };
}

export const roleName = (app, locale) => {
    const name = app[`name:${locale}`] || app["name:en"]
    const orgName = app[`OrganizationName:${locale}`] || app["OrganizationName:en"]
    return `${name} (${orgName})`;
}

export const providersToOptions = providers => {
    return providers
        .map(provider => singleProviderToOption(provider))
        .sort((r1, r2) => r1.label.toLowerCase().localeCompare(r2.label.toLowerCase()));
}

export const deriveApplicationAttributes = (role, locale, multiple, separator) => {
    const applications = role.applicationMaps;
    if (!isEmpty(applications)) {
        if (applications.length === 1) {
            const firstApplication = applications[0];
            if (firstApplication.unknown) {
                role.unknownInManage = true;
            }
            role.applicationName = firstApplication[`name:${locale}`] || firstApplication["name:en"];
            role.applicationNames = role.applicationName;
            role.applicationOrganizationName = firstApplication[`OrganizationName:${locale}`] || firstApplication["OrganizationName:en"];
            role.logo = firstApplication.logo;
        } else {
            role.applicationName = multiple;
            if (applications.every(app => app.unknown)) {
                role.unknownInManage = true;
            }
            const appNames = new Set(applications
                .map(app => app[`name:${locale}`] || app["name:en"]));
            role.applicationNames = splitListSemantically([...appNames], separator);
            const orgNames = new Set(applications
                .map(app => app[`OrganizationName:${locale}`] || app["OrganizationName:en"]));
            role.applicationOrganizationName = splitListSemantically([...orgNames], separator);
            role.logo = <MultipleIcon/>;
        }
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

