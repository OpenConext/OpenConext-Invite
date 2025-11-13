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
import {Loader} from "@surfnet/sds";
import {Tokens} from "../tabs/Tokens";
import {ApplicationUsers} from "../tabs/ApplicationUsers";
import Applications from "../tabs/Applications";
import {InviteTabs, useUserTabs} from "../hooks/useUserTabs";

export const Home = () => {
    const navigate = useNavigate();

    const {tab = InviteTabs.ROLES} = useParams();
    const [winking, setWinking] = useState(false);
    const [loading, setLoading] = useState(true);
    const user = useAppStore((state) => state.user)
    const {userTabs} = useUserTabs();

    const allTabs = {
        [InviteTabs.ROLES]: (
            <Page key="roles" name="roles" label={I18n.t("tabs.roles")}>
                <Roles/>
            </Page>
        ),
        [InviteTabs.USERS]: (
            <Page key="users" name="users" label={I18n.t("tabs.users")}>
                <Users/>
            </Page>
        ),
        [InviteTabs.APPLICATIONS]: (
            <Page key="applications" name="applications" label={I18n.t("tabs.applications")}>
                <Applications/>
            </Page>
        ),
        [InviteTabs.APPLICATION_USERS]: (
            <Page key="applicationUsers" name="applicationUsers" label={I18n.t("tabs.applicationUsers")}>
                <ApplicationUsers/>
            </Page>
        ),
        [InviteTabs.TOKENS]: (
            <Page key="tokens" name="tokens" label={I18n.t("tabs.tokens")}>
                <Tokens/>
            </Page>
        )
    }

    useEffect(() => {
        if (user) {
            if (highestAuthority(user) === AUTHORITIES.INVITER) {
                navigate("/inviter");
                return;
            }
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {value: I18n.t(`tabs.${tab}`)}
                ]
            });
        }
        setLoading(false);
    }, [tab, user]);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        navigate(`/home/${name}`);
    }

    const winkOwl = () => {
        setWinking(true);
        setTimeout(() => setWinking(false), 850);
    }

    if (loading) {
        return <Loader/>
    }

    const visibleTabs = userTabs.map((tr) => allTabs[tr])

    return (
        <div className="home">
            <div className="mod-home-container">
                <UnitHeader obj={({name: I18n.t("home.access"), svg: Logo, style: winking ? "wink" : ""})}
                            svgClick={() => winkOwl()}>
                    <p>{I18n.t("header.subTitle")}</p>
                </UnitHeader>
                <Tabs activeTab={tab} tabChanged={tabChanged}>
                    {visibleTabs}
                </Tabs>
            </div>
        </div>
    );
}
