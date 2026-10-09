import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import React, {useEffect, useState} from "react";
import Tabs from "../components/Tabs";
import "./Home.scss";
import {useNavigate, useParams} from "react-router";
import {Users} from "../tabs/Users";
import {Page} from "../components/Page";
import {Roles} from "../tabs/Roles";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import {Tokens} from "../tabs/Tokens";
import {ApplicationUsers} from "../tabs/ApplicationUsers";
import Applications from "../tabs/Applications";
import {isEmpty} from "../utils/Utils";
import {MineInvitations} from "../tabs/MineInvitations";
import {SearchGroupContext} from "../utils/SearchGroupContext";

export const Home = () => {
    const {tab = "roles"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);

    const user = useAppStore((state) => state.user)
    const navigate = useNavigate();

    const tabs = [
        <Page key="roles"
              name="roles"
              label={I18n.t("tabs.roles")}>
            <Roles/>
        </Page>,
        (user && user.superUser) ?
            <Page key="users"
                  name="users"
                  label={I18n.t("tabs.users")}>
                <Users/>
            </Page> : null,
        (user && user.superUser) ?
            <Page key="applications"
                  name="applications"
                  label={I18n.t("tabs.applications")}>
                <Applications/>
            </Page> : null,
        (user && !user.superUser && user.institutionAdmin && user.organizationGUID && !isEmpty(user.applications)) ?
            <Page key="applicationUsers"
                  name="applicationUsers"
                  label={I18n.t("tabs.applicationUsers")}>
                <ApplicationUsers/>
            </Page> : null,
        (user && !user.superUser && ((user.institutionAdmin && user.organizationGUID && !isEmpty(user.applications)) ||
         !isEmpty(user.userApplications))) ?
            <Page key="applications"
                  name="applications"
                  label={I18n.t("tabs.applications")}>
                <Applications/>
            </Page> : null,
        (user && !user.superUser && user.institutionAdmin && user.organizationGUID) ?
            <Page key="invitations"
                  name="invitations"
                  label={I18n.t("tabs.invitations")}>
                <MineInvitations/>
            </Page> : null,
        (user && user.superUser) ?
            <Page key="tokens"
                  name="tokens"
                  label={I18n.t("tabs.tokens")}>
                <Tokens/>
            </Page> : null
    ].filter(t => t !== null);

    useEffect(() => {
        if (highestAuthority(user) === AUTHORITIES.INVITER) {
            navigate("/inviter");
        }
        tabChanged(currentTab);
    }, []);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/home/${name}`);
        useAppStore.setState({
            breadcrumbPath: [
                {path: "/home", value: I18n.t("tabs.home")},
                {value: I18n.t(`tabs.${name}`)}
            ]
        });
    }

    const heading = currentTab === "roles" ? I18n.t("roles.heading") : I18n.t(`tabs.${currentTab}`);
    return (
        <div className="home">
            <div className="mod-home-container">
                <h1>{heading}</h1>
                {currentTab === "roles" && <p className="intro">{I18n.t("roles.intro")}</p>}
                <SearchGroupContext.Provider value="home">
                    <Tabs activeTab={currentTab}
                          tabChanged={tabChanged}>
                        {tabs}
                    </Tabs>
                </SearchGroupContext.Provider>
            </div>
        </div>
    );
}
