import I18n from "../locale/I18n";
import {isEmpty, splitListSemantically} from "./Utils";

export const organisationName = apps => {
    if (apps.length === 1) {
        return ` (${apps[0]["OrganizationName:en"]})`;
    } else {
        const set = new Set(apps.map(app => app["OrganizationName:en"]).sort());
        const orgNames = splitListSemantically([...set], I18n.t("forms.and"));
        return ` (${orgNames})`;
    }
}

export const applicationName = (app, locale) => {
    const name = app[`name:${locale}`] || app["name:en"]
    const organizationName = app[`OrganizationName:${locale}`] || app["OrganizationName:en"];
    const organisationValue = isEmpty(organizationName) ? "" : ` (${organizationName})`;
    return `${name}${organisationValue}`;
}

export const reduceApplicationFromUserRoles = (userRoles, locale) => {
    //First we need the roleName, roleDescription and applicationName for each userRole.role.applicationMaps
    userRoles.forEach(userRole => userRole.role.applicationMaps
        .forEach(app => {
            app.applicationName = applicationName(app, locale);
            app.roleName = userRole.role.name;
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

export const initials = name => {
    const parts = (name || "").trim().split(/\s+/).filter(p => p.length > 0);
    if (parts.length === 0) {
        return "";
    }
    const first = parts[0].charAt(0);
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : "";
    return (first + last).toUpperCase();
}

//One entry per role with the applications of that role
export const groupApplicationsByRole = (userRoles, locale) => {
    const roles = [];
    userRoles.forEach(userRole => {
        const applications = (userRole.role.applicationMaps || [])
            //Applications that do not exist any longer in Manage, are marked as unknown server side
            .filter(app => !app.unknown)
            .map(app => ({
                logo: app.logo,
                landingPage: app.landingPage,
                name: app[`name:${locale}`] || app["name:en"],
                organisation: app[`OrganizationName:${locale}`] || app["OrganizationName:en"],
                description: userRole.role.description
            }))
            .sort((a, b) => (a.name || "").localeCompare(b.name || ""));
        if (applications.length > 0) {
            roles.push({id: userRole.role.id, roleName: userRole.role.name, applications});
        }
    });
    return roles.sort((a, b) => a.roleName.localeCompare(b.roleName));
}
