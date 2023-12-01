import I18n from "../locale/I18n";
import {splitListSemantically} from "./Utils";

export const organisationName = apps => {
    if (apps.length === 1) {
        return ` (${apps[0]["OrganizationName:en"]})`;
    } else {
        const set = new Set(apps.map(app => app["OrganizationName:en"]).sort());
        const orgNames = splitListSemantically([...set], I18n.t("forms.and"));
        return ` (${orgNames})`;
    }
}

export const roleName = app => {
    const name = app[`name:${I18n.locale}`] || app["name:en"]
    const orgName = app[`OrganizationName:${I18n.locale}`] || app["OrganizationName:en"]
    return `${name} (${orgName})`;
}
