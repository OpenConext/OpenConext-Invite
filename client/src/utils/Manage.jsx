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
        receivesMemberships: provider.receivesMemberships,
        url: provider.url || provider.landingPage,
        landingPage: provider.landingPage || provider.url
    };
}

export const applicationName = (app, locale) => {
    const name = app[`name:${locale}`] || app["name:en"]
    const organizationName = app[`OrganizationName:${locale}`] || app["OrganizationName:en"];
    const organisationValue = isEmpty(organizationName) ? "" : ` (${organizationName})`;
    return `${name}${organisationValue}`;
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
            if (isEmpty(firstApplication) || firstApplication.unknown) {
                role.unknownInManage = true;
            } else {
                role.applicationName = firstApplication[`name:${locale}`] || firstApplication["name:en"];
                role.applicationNames = role.applicationName;
                role.applicationOrganizationName = firstApplication[`OrganizationName:${locale}`] || firstApplication["OrganizationName:en"];
                role.logo = firstApplication.logo;
            }
        } else {
            role.applicationName = multiple;
            if (applications.some(app => isEmpty(app) || app.unknown)) {
                role.unknownInManage = true;
            }
            const filteredApplications = applications.filter(app => !isEmpty(app));
            const appNames = new Set(filteredApplications
                .map(app => app[`name:${locale}`] || app["name:en"]));
            role.applicationNames = splitListSemantically([...appNames], separator);
            const orgNames = new Set(filteredApplications
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

export const mergeProvidersProvisioningsRoles = (providers, provisionings, locale = "en") => {
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
     *         "roleCount": 5,
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
        roleCount: provider.roleCount,
        provisionings: provisionings
            .filter(prov => prov.applications.some(app => app.id === provider.id))
            .map(prov => ({
                provisioningType: prov.provisioning_type,
                name: locale === "en" ? prov["name:en"] || prov["name:nl"] : prov["name:nl"] || prov["name:en"],
            }))
    }))
}

export const reduceApplicationFromUserRoles = (userRoles, locale) => {
    //First we need the id, name, description, authority and applicationName for each userRole.role.applicationMaps
    userRoles.forEach(userRole => userRole.role.applicationMaps
        .forEach(app => {
            app.applicationName = applicationName(app, locale);
            app.roleId = userRole.role.id;
            app.roleName = userRole.role.name;
            app.authority = userRole.authority;
            app.roleDescription = userRole.role.description;
        }));
    //Now get all applicationMaps flattened and return sorted
    const applicationMaps = userRoles
        .map(userRole => userRole.role.applicationMaps)
        .flat()
        //Applications that do not exist any longer in Manage, are marked as unknown server side
        .filter(applicationMap => !applicationMap.unknown);
    return applicationMaps.sort((app1, app2) =>
        app1.applicationName.localeCompare(app2.applicationName) || app1.roleName.localeCompare(app2.roleName)
    );
}


