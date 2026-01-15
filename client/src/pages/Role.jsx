import React, {useEffect, useState} from "react";
import {managersByRoleId, roleByID} from "../api";
import I18n from "../locale/I18n";
import "./Role.scss";
import {ButtonType, Loader, Tooltip} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import WebsiteIcon from "../icons/network-information.svg";
import PersonIcon from "../icons/persons.svg";
import {allowedToEditRole, AUTHORITIES, highestAuthority, isUserAllowed, urnFromRole} from "../utils/UserRole";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {UserRoles} from "../tabs/UserRoles";
import {Invitations} from "../tabs/Invitations";
import ClipBoardCopy from "../components/ClipBoardCopy";
import {deriveApplicationAttributes} from "../utils/Manage";
import DOMPurify from "dompurify";
import {UnitHeaderInviter} from "../components/UnitHeaderInviter";
import {isEmpty} from "../utils/Utils";
import {displayExpiryDate, futureDate} from "../utils/Date";

export const Role = () => {
    const {id, tab = "users"} = useParams();
    const navigate = useNavigate();
    const {user, config, clearFlash, setFlash} = useAppStore(state => state);
    const [role, setRole] = useState({});
    const [userRole, setUserRole] = useState({});
    const [loading, setLoading] = useState(true);
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);
    const [managerEmails, setManagerEmails] = useState([]);

    useEffect(() => {
            const isInviter = highestAuthority(user) === AUTHORITIES.INVITER;
            const paths = isInviter ? [
                {path: "/inviter", value: I18n.t("tabs.home")},
                {path: `/roles/${role.id}`, value: role.name},
                {value: I18n.t(`tabs.${currentTab}`)}
            ] : [
                {path: "/home", value: I18n.t("tabs.home")},
                {path: "/home/roles", value: I18n.t("tabs.roles")},
                {path: `/roles/${role.id}`, value: role.name},
                {value: I18n.t(`tabs.${currentTab}`)}
            ]
            useAppStore.setState({
                breadcrumbPath: paths
            });
        },
        [user, currentTab, role])

    useEffect(() => {
        if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
            navigate("/404");
            return;
        }
        roleByID(id, false)
            .then(res => {
                deriveApplicationAttributes(res, I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"))
                setRole(res);
                const isInviter = highestAuthority(user) === AUTHORITIES.INVITER;
                if (isInviter) {
                    managersByRoleId(id).then(emails => setManagerEmails(emails));
                }
                const newTabs = [
                    <Page key="guests"
                          name="guests"
                          label={I18n.t("tabs.guestRoles")}>
                        <UserRoles role={res}
                                   guests={true}
                        />
                    </Page>,
                    <Page key="invitations"
                          name="invitations"
                          label={I18n.t("tabs.allPendingInvitations")}>
                        <Invitations role={res}/>
                    </Page>,
                    <Page key="maintainers"
                          name="maintainers"
                          label={I18n.t("tabs.userRoles")}>
                        <UserRoles role={res}
                                   guests={false}
                        />
                    </Page>
                ];
                setTabs(newTabs.filter(tab => tab !== null));
                if (res.unknownInManage) {
                    setFlash(I18n.t("roles.unknownInManageDisabled"), "error");
                }
                setLoading(false);
                setUserRole((user.userRoles || []).find(ur => ur.role.id === res.id));

            })
            .catch(() => navigate("/"))

    }, [id]);// eslint-disable-line react-hooks/exhaustive-deps

    const separator = (role, index) => {
        const l = role.applicationMaps.length;
        if (index === (l - 1)) {
            return "";
        }
        if (index === (l - 2)) {
            return ` ${I18n.t("forms.and")} `
        }
        return ", ";
    }

    const landingPages = role => {
        return role.applicationMaps
            .filter(m => !isEmpty(m) && !m.unknown)
            .map((m, index) => {
                const name = m[`name:${I18n.locale}`] || m["name:en"];
                const orgName = m[`OrganizationName:${I18n.locale}`] || m["OrganizationName:en"];
                const landingPage = role.applicationUsages.find(au => au.application.manageId === m.id).landingPage;
                return (
                    <span key={index}>
                        <a href={landingPage} target="_blank" rel="noreferrer">{`${name}`}</a>
                        {`${orgName ? " (" + orgName + ")" : ""}`}
                        {separator(role, index)}
                    </span>
                );
            })
    }

    const getActions = () => {
        const actions = [];
        if (allowedToEditRole(user, role)) {
            actions.push({
                buttonType: ButtonType.Primary,
                name: I18n.t("forms.edit"),
                perform: () => {
                    clearFlash();
                    navigate(`/role/${role.id}`)
                }
            });
        }
        if (highestAuthority(user) === AUTHORITIES.INVITER) {
            actions.push({
                buttonType: ButtonType.Primary,
                name: I18n.t("invitations.newGuest"),
                perform: () => navigate(`/invitation/new?maintainer=false`, {state: role.id})
            });
        }
        return actions;
    }

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/roles/${role.id}/${name}`);
    }

    if (loading) {
        return <Loader/>
    }

    const logo = role.logo;
    const urn = urnFromRole(config.groupUrnPrefix, role);
    const isInviter = highestAuthority(user) === AUTHORITIES.INVITER;

    return (
        <div className="mod-role">
            {isInviter &&
                <UnitHeaderInviter role={role}
                                   userRole={userRole}
                                   managerEmails={managerEmails}/>}
            {!isInviter &&
                <UnitHeader obj={({...role, logo: logo})}
                            displayDescription={true}
                            actions={getActions()}>
                    <div className={"urn-container"}>
                        <span>{I18n.t("role.copyUrn")}</span>
                        <ClipBoardCopy txt={urn} transparentBackground={true}/>
                    </div>
                    <div className={"meta-data"}>
                        <div className={"meta-data-row"}>
                            <PersonIcon/>
                            <span dangerouslySetInnerHTML={{
                                __html: DOMPurify.sanitize(I18n.t("role.userInfo", {
                                    nbr: role.userRoleCount,
                                    period: displayExpiryDate(
                                        !isEmpty(role.defaultExpiryDate) ? new Date(role.defaultExpiryDate * 1000) :
                                        futureDate(role.defaultExpiryDays))
                                }))
                            }}/>
                        </div>
                        {!role.unknownInManage &&
                            <div className={"meta-data-row"}>
                                <WebsiteIcon/>
                                <div>{landingPages(role)}</div>
                            </div>}
                        {role.unknownInManage &&
                            <div className="meta-data-row unknown-in-manage">
                                <span className="unknown-in-manage">{I18n.t("roles.unknownInManage")} </span>
                                <Tooltip tip={I18n.t("roles.unknownInManageToolTip")} standalone={true}
                                         clickable={true}/>
                            </div>}
                    </div>
                </UnitHeader>}
            <div className="mod-role">
                <Tabs activeTab={currentTab}
                      tabChanged={tabChanged}>
                    {tabs}
                </Tabs>
            </div>
        </div>);
};
