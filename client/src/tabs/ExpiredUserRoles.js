import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./ExpiredUserRoles.scss";
import {Chip, Loader} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {dateFromEpoch, shortDateFromEpoch} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";
import {expiryUserRoles} from "../api";
import {stopEvent} from "../utils/Utils";


export const ExpiredUserRoles = () => {
    const navigate = useNavigate();
    const [userRoles, setUserRoles] = useState({});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
            expiryUserRoles().then(res => {
                setUserRoles(res);
                setLoading(false);
            });

        },
        [])

    if (loading) {
        return <Loader/>
    }

    const openRole = (e, userRole) => {
        const path = `/roles/${userRole.role.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const displayEndDate = userRole => {
        return dateFromEpoch(userRole.endDate)
    }

    const columns = [
        {
            key: "name",
            header: I18n.t("users.name_email"),
            mapper: userRole => (
                <div className="user-name-email">
                    <span className="name">{userRole.userInfo.name}</span>
                    <span className="email">{userRole.userInfo.email}</span>
                </div>)
        },
        {
            key: "schacHomeOrganisation",
            header: I18n.t("users.schacHomeOrganization"),
            mapper: userRole => <span>{userRole.userInfo.schacHomeOrganization}</span>
        },
        {
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: userRole => <Chip type={chipTypeForUserRole(userRole.authority)}
                                      label={I18n.t(`access.${userRole.authority}`)}/>
        },
        {
            key: "createdAt",
            header: I18n.t("userRoles.createdAt"),
            mapper: userRole => shortDateFromEpoch(userRole.createdAt)
        },
        {
            key: "endDate",
            header: I18n.t("roles.endDate"),
            toolTip: I18n.t("tooltips.roleExpiryDateTooltip"),
            mapper: userRole => displayEndDate(userRole)
        }];

    return (
        <div className="mod-expired-user-roles">
            <Entities entities={userRoles}
                      modelName="userRoles"
                      defaultSort="name"
                      columns={columns}
                      showNew={false}
                      customNoEntities={I18n.t(`expiredUserRoles.noResults`)}
                      loading={false}
                      rowLinkMapper={openRole}
                      title={I18n.t("expiredUserRoles.title")}
                      searchAttributes={["name", "email", "schacHomeOrganization"]}
                      inputFocus={true}>
            </Entities>
        </div>)

}
