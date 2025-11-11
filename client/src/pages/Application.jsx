import React, {useEffect, useState} from "react";
import {rolesPerApplicationManageId} from "../api";
import I18n from "../locale/I18n";
import "./Application.scss";
import WebsiteIcon from "../icons/network-information.svg";
import {Chip} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {deriveApplicationAttributes} from "../utils/Manage";
import {isEmpty, stopEvent} from "../utils/Utils";
import {Entities} from "../components/Entities";
import {chipTypeForUserRole} from "../utils/Authority";
import AlertLogo from "@surfnet/sds/icons/functional-icons/alert-circle.svg";

export const Application = () => {
    const {manageId} = useParams();
    const navigate = useNavigate();
    const {user} = useAppStore(state => state);
    const [roles, setRoles] = useState([]);
    const [application, setApplication] = useState({});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
            if (!isUserAllowed(AUTHORITIES.INSTITUTION_ADMIN, user)) {
                navigate("/404");
                return;
            }
            rolesPerApplicationManageId(manageId)
                .then(res => {
                    res.forEach(role =>
                        deriveApplicationAttributes(role, I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"))
                    );
                    setRoles(res);
                    const app = res
                        .filter(role => role.applicationMaps.some(app => app.id === manageId))
                        .map(role => role.applicationMaps.filter(app => app.id === manageId))
                        .flat()[0];
                    app.name = I18n.locale === "en" ? app["name:en"] || app["name:nl"] : app["name:nl"] || app["name:en"];
                    app.description = I18n.locale === "en" ? app["OrganizationName:en"] || app["OrganizationName:nl"] : app["OrganizationName:nl"] || app["OrganizationName:en"];
                    setApplication(app);
                    const paths = [
                        {path: "/home", value: I18n.t("tabs.home")},
                        {path: "/home/applications", value: I18n.t("tabs.applications")},
                        {value: app.name}
                    ]
                    useAppStore.setState({
                        breadcrumbPath: paths
                    });
                    // setTimeout(() => setLoading(false), 40);
                    setLoading(false);
                })
                .catch(() => navigate("/"))
        },
        [user]); // eslint-disable-line react-hooks/exhaustive-deps

    const openRole = (e, role) => {
        const path = `/roles/${role.id}`
        if (e.metaKey || e.ctrlKey) {
            window.open(path, '_blank');
        } else {
            stopEvent(e);
            navigate(path);
        }
    };

    const columns = [
        {
            nonSortable: true,
            key: "logo",

            header: "",
            mapper: role => role.unknownInManage ? <div className="role-icon unknown-in-manage"><AlertLogo/></div> :
                <div className="role-icon">
                    {typeof role.logo === "string" ? <img src={role.logo} alt="logo"/> : role.logo}
                </div>
        },
        {
            key: "name",
            header: I18n.t("applications.accessRole"),
            mapper: role => <span>{role.name}</span>
        },
        {
            key: "description",
            header: I18n.t("roles.description"),
            mapper: role => <span className={"cut-of-lines"}>{role.description}</span>
        },
        {
            key: "authority",
            header: I18n.t("roles.authority"),
            mapper: role => <Chip type={chipTypeForUserRole(role.authority)}
                                  label={role.isUserRole ? I18n.t(`access.${role.authority}`) :
                                      I18n.t("roles.noMember")}/>
        },
        {
            key: "userRoleCount",
            header: I18n.t("roles.userRoleCount"),
            mapper: role => role.userRoleCount
        }

    ];
    return (
        <div className="mod-application-container">
            <UnitHeader obj={application}
                        displayDescription={true}
                        actions={[]}>
                <div className={"meta-data"}>
                    {(!isEmpty(application.url) || !isEmpty(application.landingPage)) &&
                        <div className={"meta-data-row"}>
                            <WebsiteIcon/>
                            <a href={application.url || application.landingPage}
                               rel="noreferrer"
                               target="_blank">
                                <span className={"application-name"}>{application.url || application.landingPage}</span>
                            </a>
                        </div>}
                </div>
            </UnitHeader>
            <div className="mod-application">
                <Entities
                    entities={roles}
                    modelName="applicationRoles"
                    title={I18n.t("applications.title", {nbr: roles.length})}
                    showNew={true}
                    busy={loading}
                    loading={false}
                    newLabel={I18n.t("applications.new")}
                    newEntityFunc={() => navigate("/role/new", {state: application.id})}
                    defaultSort="name"
                    columns={columns}
                    searchAttributes={["name", "description"]}
                    inputFocus={true}
                    rowLinkMapper={openRole}/>

            </div>
        </div>);
};
