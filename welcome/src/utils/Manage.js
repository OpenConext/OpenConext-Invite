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
    //First we need the roleName, roleDescription and applicationName and for each userRole.role.applicationMaps
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
        .filter(applicationMap => !applicationMap.unknown);
    return applicationMaps.sort((app1, app2) =>
        app1.applicationName.localeCompare(app2.applicationName) || app1.roleName.localeCompare(app2.roleName)
    );
}
