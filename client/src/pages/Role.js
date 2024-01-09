import React, {useEffect, useState} from "react";
import {invitationsByRoleId, roleByID, userRolesByRoleId} from "../api";
import I18n from "../locale/I18n";
import "./Role.scss";
import {ButtonType, Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {ReactComponent as AlertLogo} from "@surfnet/sds/icons/functional-icons/alert-circle.svg";
import {ReactComponent as WebsiteIcon} from "../icons/network-information.svg";
import {ReactComponent as PersonIcon} from "../icons/persons.svg";
import {ReactComponent as GuestLogo} from "@surfnet/sds/icons/illustrative-icons/hr.svg";
import {ReactComponent as InvitationLogo} from "@surfnet/sds/icons/functional-icons/id-1.svg";
import {allowedToEditRole, AUTHORITIES, highestAuthority, isUserAllowed, urnFromRole} from "../utils/UserRole";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {UserRoles} from "../tabs/UserRoles";
import {Invitations} from "../tabs/Invitations";
import ClipBoardCopy from "../components/ClipBoardCopy";
import {deriveApplicationAttributes} from "../utils/Manage";
import DOMPurify from "dompurify";
import {isEmpty} from "../utils/Utils";
import {UnitHeaderInviter} from "../components/UnitHeaderInviter";

export const Role = () => {
    const {id, tab = "users"} = useParams();
    const navigate = useNavigate();
    const {user, config, clearFlash} = useAppStore(state => state);
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
        Promise.all([roleByID(id, false), userRolesByRoleId(id), invitationsByRoleId(id)])
            .then(res => {
                deriveApplicationAttributes(res[0], I18n.locale, I18n.t("roles.multiple"), I18n.t("forms.and"))
                setRole(res[0]);
                setUserRole(res[1].find(userRole => userRole.role.id === res[0].id && userRole.userInfo.id === user.id));
                const newTabs = [
                    <Page key="guests"
                          name="guests"
                          label={I18n.t("tabs.guestRoles")}
                          Icon={GuestLogo}>
                        <UserRoles role={res[0]}
                                   guests={true}
                                   userRoles={res[1].filter(userRole => userRole.authority === AUTHORITIES.GUEST ||
                                                            userRole.guestRoleIncluded)}/>
                    </Page>,
                    <Page key="invitations"
                          name="invitations"
                          label={I18n.t("tabs.invitations")}
                          Icon={InvitationLogo}>
                        <Invitations role={res[0]}
                                     preloadedInvitations={res[2]}/>
                    </Page>,
                    <Page key="maintainers"
                          name="maintainers"
                          label={I18n.t("tabs.userRoles")}
                          Icon={UserLogo}>
                        <UserRoles role={res[0]}
                                   guests={false}
                                   userRoles={res[1].filter(userRole => userRole.authority !== AUTHORITIES.GUEST)}/>
                    </Page>
                ];
                setTabs(newTabs.filter(tab => tab !== null));
                let managers = res[1].filter(userRole => userRole.authority === AUTHORITIES.MANAGER)
                    .map(userRole => userRole.userInfo.email);
                if (isEmpty((managers))) {
                    managers = res[1].filter(userRole => userRole.authority === AUTHORITIES.INSTITUTION_ADMIN)
                        .map(userRole => userRole.userInfo.email);
                }
                setManagerEmails(managers)
                setLoading(false);

            })
            .catch(() => navigate("/"))

    }, [id]);// eslint-disable-line react-hooks/exhaustive-deps

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
                <UnitHeaderInviter role={role} userRole={userRole} managerEmails={managerEmails}/> }
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
                            valid: role.defaultExpiryDays
                        }))
                    }}/>
                </div>
                {!role.unknownInManage &&
                <div className={"meta-data-row"}>
                    <WebsiteIcon/>
                    <a href={role.applicationUsages[0].landingPage}
                       rel="noreferrer"
                       target="_blank">
                        <span className={"application-name"}>{`${role.applicationNames}`}</span>
                    </a>{role.applicationOrganizationName && <span>{` (${role.applicationOrganizationName})`}</span>}
                </div>}
                {role.unknownInManage &&
                    <div className="meta-data-row unknown-in-manage">
                        <AlertLogo/>
                        <span className="unknown-in-manage">{I18n.t("roles.unknownInManage")}</span>
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
