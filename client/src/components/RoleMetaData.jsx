import React from "react";
import "./RoleMetaData.scss";
import {MetaDataList} from "@surfnet/sds";
import I18n from "../locale/I18n";
import {dateFromEpoch} from "../utils/Date";


export const RoleMetaData = ({role, provider, user}) => {

    const organisation = provider.data.metaDataFields["OrganizationName:en"] || "-";
    const userRole = user.userRoles.find(userRole => userRole.role.id === role.id) ||
        {authority: I18n.t("roles.noMember")};
    const items = [
        {
            label: I18n.t("users.access"),
            values: [<a href={role.landingPage} rel="noreferrer"
                        target="_blank">{I18n.t("users.landingPage")}</a>]
        },
        {
            label: I18n.t("users.organisation"),
            values: [<span>{organisation}</span>]
        },
        {
            label: I18n.t("users.authority"),
            values: [<span>{userRole.authority}</span>]
        },
        {
            label: I18n.t("users.endDate"),
            values: [
                <span>{userRole.endDate ? dateFromEpoch(userRole.endDate) : I18n.t("forms.none")}</span>]
        }

    ]
    return (
        <MetaDataList items={items} cutOffNumber={999}/>
    );
}