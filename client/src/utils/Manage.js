import {isEmpty, splitListSemantically} from "./Utils";
import {ReactComponent as MultipleIcon} from "../icons/multi-role.svg";

export const singleProviderToOption = (provider, locale) => {
    const manageType = provider.type ? provider.type.toUpperCase() : provider.manageType;
    const manageId = provider.id || provider.manageId;
    return {
        value: manageId,
        label: roleName(provider, locale),
        type: manageType,
        manageType: manageType,
        manageId: manageId,
        url: provider.url || provider.landingPage,
        landingPage: provider.landingPage || provider.url
    };
}

export const roleName = (app, locale) => {
    const name = app[`name:${locale}`] || app["name:en"]
    const organizationName = app[`OrganizationName:${locale}`] || app["OrganizationName:en"];
    const organisationValue = isEmpty(organizationName) ? "" : ` (${organizationName})`;
    return `${name}${organisationValue}`;
}

export const providersToOptions = (providers, locale) => {
    return providers
        .map(provider => singleProviderToOption(provider, locale))
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
            "name:en": "Unknown in Manage",
            unknownInManage: true
        }
    }
    return provider;
}

export const mergeProvidersProvisioningsRoles = (providers, provisionings, roles, locale = "en") => {
    /**
     * We want the following structure for the providers, provisionings and roles:
     *
     *     const app = {
     *         "id": "2",
     *         "logo": "https://static.surfconext.nl/media/idp/surfconext.png",
     *         "name": "Calendar EN",
     *         "type": "oidc10_rp",
     *         "organization": "SURF bv",
     *         "url": "https://default-url-calendar.org",
     *         "roles": [
     *              "id": 3920,
     *              "name": "Mail",
     *              "landingPage": "https://landingpage.com"
     *         ],
     *         "provisionings": [
     *             "name": "SCIM Hardewijk"
     *             "provisioningType": "scim",
     *         ]
     *     }
     */
    return providers.map(provider => ({
        id: provider.id,
        logo: provider.logo,
        name: locale === "en" ? provider["name:en"] || provider["name:nl"] : provider["name:nl"] || provider["name:en"],
        type: provider.type,
        organization: locale === "en" ? provider["OrganizationName:en"] || provider["OrganizationName:nl"] : provider["OrganizationName:nl"] || provider["OrganizationName:en"],
        url: provider.url,
        roles: roles
            .filter(role => role.applicationUsages.some(appUsage => appUsage.application.manageId === provider.id))
            .map(role => ({
                id: role.id,
                name: role.name,
                landingPage: role.applicationUsages.find(appUsage => appUsage.application.manageId === provider.id).landingPage
            })),
        provisionings: provisionings
            .filter(prov => prov.applications.some(app => app.id === provider.id))
            .map(prov => ({
                provisioningType: prov.provisioning_type,
                name: locale === "en" ? prov["name:en"] || prov["name:nl"] : prov["name:nl"] || prov["name:en"],
            }))
    }))
}

