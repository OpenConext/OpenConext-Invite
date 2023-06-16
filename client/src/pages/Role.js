import React, {useEffect, useState} from "react";
import {invitationsByRoleId, roleByID, userRoleByRoleId} from "../api";
import I18n from "../locale/I18n";
import "./Profile.scss";
import {Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {ReactComponent as InvitationLogo} from "@surfnet/sds/icons/functional-icons/id-1.svg";
import {AUTHORITIES, isUserAllowed} from "../utils/UserRole";
import {RoleMetaData} from "../components/RoleMetaData";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {UserRoles} from "../tabs/UserRoles";
import {Invitations} from "../tabs/Invitations";

export const Role = () => {
    const {id, tab = "users"} = useParams();
    const navigate = useNavigate();
    const {user} = useAppStore(state => state);
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
        Promise.all([roleByID(id), userRoleByRoleId(id), invitationsByRoleId(id)])
            .then(res => {
                setRole(res[0]);
                setLoading(false);
                const newTabs = [
                    <Page key="users"
                          name="users"
                          label={I18n.t("tabs.userRoles")}
                          Icon={UserLogo}>
                        <UserRoles userRoles={res[1]}/>
                    </Page>,
                    <Page key="invitations"
                          name="invitations"
                          label={I18n.t("tabs.invitations")}
                          Icon={InvitationLogo}>
                        <Invitations invitations={res[2]}/>
                    </Page>
                ];
                setTabs(newTabs);
            })

    }, [id]);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/roles/${role.id}/${name}`);
    }

    if (loading) {
        return <Loader/>
    }

    const logo = role.application.data.metaDataFields["logo:0:url"];
    return (
        <div className="mod-role">
            <UnitHeader obj={({name: role.name, logo: logo})}
                        displayDescription={true}>
                <RoleMetaData role={role} user={user} provider={role.application}/>
            </UnitHeader>
            <Tabs activeTab={currentTab}
                  tabChanged={tabChanged}>
                {tabs}
            </Tabs>
        </div>);
};
