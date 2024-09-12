import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import React, {useEffect, useState} from "react";
import {ReactComponent as Logo} from "../icons/Owl_Emblem.svg";
import {ReactComponent as RoleLogo} from "@surfnet/sds/icons/illustrative-icons/hierarchy-2.svg";
import {ReactComponent as ApplicationLogo} from "@surfnet/sds/icons/illustrative-icons/database-refresh.svg";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import {ReactComponent as TokenLogo} from "@surfnet/sds/icons/illustrative-icons/database-hand.svg";
import Tabs from "../components/Tabs";
import "./Home.scss";
import {UnitHeader} from "../components/UnitHeader";
import {useNavigate, useParams} from "react-router-dom";
import {Users} from "../tabs/Users";
import {Page} from "../components/Page";
import {Roles} from "../tabs/Roles";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import {Loader} from "@surfnet/sds";
import {Tokens} from "../tabs/Tokens";
import {ApplicationUsers} from "../tabs/ApplicationUsers";
import Applications from "../tabs/Applications";
import {isEmpty} from "../utils/Utils";

export const Home = () => {
    const {tab = "roles"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);
    const [winking, setWinking] = useState(false);
    const [loading, setLoading] = useState(true);

    const user = useAppStore((state) => state.user)
    const navigate = useNavigate();

    useEffect(() => {
        if (user) {
            if (highestAuthority(user) === AUTHORITIES.INVITER) {
                navigate("/inviter");
                return;
            }
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {value: I18n.t(`tabs.${currentTab}`)}
                ]
            });
        }
        const newTabs = [
            <Page key="roles"
                  name="roles"
                  label={I18n.t("tabs.roles")}
                  Icon={RoleLogo}>
                <Roles/>
            </Page>
        ];
        if (user && user.superUser) {
            newTabs.push(
                <Page key="users"
                      name="users"
                      label={I18n.t("tabs.users")}
                      Icon={UserLogo}>
                    <Users/>
                </Page>);
            newTabs.push(
                <Page key="applications"
                      name="applications"
                      label={I18n.t("tabs.applications")}
                      Icon={ApplicationLogo}>
                    <Applications/>
                </Page>
            );
        }
        if (user && user.institutionAdmin && user.organizationGUID && !isEmpty(user.applications)) {
            newTabs.push(
                <Page key="applications"
                      name="applications"
                      label={I18n.t("tabs.applications")}
                      Icon={ApplicationLogo}>
                    <Applications/>
                </Page>
            );
            newTabs.push(
                <Page key="applicationUsers"
                      name="applicationUsers"
                      label={I18n.t("tabs.applicationUsers")}
                      Icon={UserLogo}>
                    <ApplicationUsers/>
                </Page>);
        }
        if (user && (user.superUser || (user.institutionAdmin && user.organizationGUID))) {
            newTabs.push(
                <Page key="tokens"
                      name="tokens"
                      label={I18n.t("tabs.tokens")}
                      Icon={TokenLogo}>
                    <Tokens/>
                </Page>);
        }
        setTabs(newTabs);
        setLoading(false);
    }, [currentTab, user]);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/home/${name}`);
    }

    const winkOwl = () => {
        setWinking(true);
        setTimeout(() => setWinking(false), 850);
    }

    if (loading) {
        return <Loader/>
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
