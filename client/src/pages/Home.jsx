import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import React, {useEffect, useState} from "react";
import Logo from "../icons/Owl_Emblem.svg";
import Tabs from "../components/Tabs";
import "./Home.scss";
import {UnitHeader} from "../components/UnitHeader";
import {useNavigate, useParams} from "react-router-dom";
import {Users} from "../tabs/Users";
import {Page} from "../components/Page";
import {Roles} from "../tabs/Roles";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import {Tokens} from "../tabs/Tokens";
import {ApplicationUsers} from "../tabs/ApplicationUsers";
import Applications from "../tabs/Applications";
import {isEmpty} from "../utils/Utils";

export const Home = () => {
    const {tab = "roles"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);
    const [winking, setWinking] = useState(false);

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
        (user && !user.superUser && user.institutionAdmin && user.organizationGUID && !isEmpty(user.applications)) ?
            <Page key="applications"
                  name="applications"
                  label={I18n.t("tabs.applications")}>
                <Applications/>
            </Page> : null,
        (user && (user.superUser || (user.institutionAdmin && user.organizationGUID))) ?
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

    }, [user]);

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/home/${name}`);
        useAppStore.setState({
            breadcrumbPath: [
                {path: "/home", value: I18n.t("tabs.home")},
                {value: I18n.t(`tabs.${currentTab}`)}
            ]
        });
    }

    const winkOwl = () => {
        setWinking(true);
        setTimeout(() => setWinking(false), 850);
    }

    return (
        <div className="home">
            <div className="mod-home-container">
                <UnitHeader obj={({name: I18n.t("home.access"), svg: Logo, style: winking ? "wink" : ""})}
                            svgClick={() => winkOwl()}>
                    <p>{I18n.t("header.subTitle")}</p>
                </UnitHeader>
                <Tabs activeTab={currentTab}
                      tabChanged={tabChanged}>
                    {tabs}
                </Tabs>
            </div>
        </div>
    );
}
