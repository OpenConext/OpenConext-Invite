import React from "react";
import "./RoleMetaData.scss";
import {MetaDataList} from "@surfnet/sds";
import I18n from "../locale/I18n";
import {isEmpty} from "../utils/Utils";


export const RoleMetaData = ({role, provider, user}) => {
    if (isEmpty((user))) {
        return null;
    }
    const organisation = provider.data.metaDataFields["OrganizationName:en"] || "-";
    const items = [
        {
            label: I18n.t("users.organisation"),
            values: [<span>{organisation}</span>]
        },
        {
            label: I18n.t("users.expiryDays"),
            values: [
                <span>{role.defaultExpiryDays}</span>]
        }

    ]
    return (
        <MetaDataList items={items} cutOffNumber={999}/>
    );
}