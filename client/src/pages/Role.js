import React, {useEffect, useState} from "react";
import {invitationsByRoleId, roleByID, userRolesByRoleId} from "../api";
import I18n from "../locale/I18n";
import "./Role.scss";
import {ButtonType, Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {ReactComponent as InvitationLogo} from "@surfnet/sds/icons/functional-icons/id-1.svg";
import {allowedToDeleteInvitation, allowedToEditRole, AUTHORITIES, isUserAllowed, urnFromRole} from "../utils/UserRole";
import {RoleMetaData} from "../components/RoleMetaData";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {UserRoles} from "../tabs/UserRoles";
import {Invitations} from "../tabs/Invitations";
import ClipBoardCopy from "../components/ClipBoardCopy";

export const Role = () => {
    const {id, tab = "users"} = useParams();
    const navigate = useNavigate();
    const {user, config, clearFlash} = useAppStore(state => state);
    const [role, setRole] = useState({});
    const [loading, setLoading] = useState(true);
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);

    useEffect(() => {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/roles", value: I18n.t("tabs.roles")},
                    {path: `/roles/${role.id}`, value: role.name},
                    {value: I18n.t(`tabs.${currentTab}`)}
                ]
            });
        },
        [currentTab, role])

    useEffect(() => {
        if (!isUserAllowed(AUTHORITIES.INVITER, user)) {
            navigate("/404");
            return;
        }
        Promise.all([roleByID(id, false), userRolesByRoleId(id), invitationsByRoleId(id)])
            .then(res => {
                setRole(res[0]);
                setLoading(false);
                const newTabs = [
                    <Page key="users"
                          name="users"
                          label={I18n.t("tabs.userRoles")}
                          Icon={UserLogo}>
                        <UserRoles role={res[0]} userRoles={res[1]}/>
                    </Page>,
                    <Page key="invitations"
                          name="invitations"
                          label={I18n.t("tabs.invitations")}
                          Icon={InvitationLogo}>
                        <Invitations role={res[0]}
                                     invitations={res[2].filter(invitation => allowedToDeleteInvitation(user, invitation))}/>
                    </Page>
                ];
                setTabs(newTabs);
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
        return actions;
    }

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/roles/${role.id}/${name}`);
    }

    if (loading) {
        return <Loader/>
    }

    const logo = role.application.data.metaDataFields["logo:0:url"];
    const urn = urnFromRole(config.groupUrnPrefix, role);
    return (<>
        <UnitHeader obj={({...role, logo: logo})}
                    displayDescription={true}
                    actions={getActions()}>
            <div className={"urn-container"}>
                <span>{urn}</span>
                <ClipBoardCopy txt={urn} transparentBackground={true}/>
            </div>
            <RoleMetaData role={role} user={user} provider={role.application}/>
        </UnitHeader>
        <div className="mod-role">
            <Tabs activeTab={currentTab}
                  tabChanged={tabChanged}>
                {tabs}
            </Tabs>
        </div>
    </>);
};
