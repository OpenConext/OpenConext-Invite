import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import React, {useEffect, useState} from "react";
import owl from "../icons/owl.wav";
import {ReactComponent as Logo} from "../icons/Owl_Emblem.svg";
import {ReactComponent as RoleLogo} from "@surfnet/sds/icons/illustrative-icons/hierarchy-2.svg";
import {ReactComponent as ApplicationLogo} from "@surfnet/sds/icons/illustrative-icons/database-refresh.svg";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import Tabs from "../components/Tabs";
import {UnitHeader} from "../components/UnitHeader";
import {useNavigate, useParams} from "react-router-dom";
import {Users} from "../tabs/Users";
import {Page} from "../components/Page";
import {Roles} from "../tabs/Roles";
import Applications from "../tabs/Applications";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";
import {Loader} from "@surfnet/sds";

export const Home = () => {
    const {tab = "roles"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);
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
        setTabs(newTabs);
        setLoading(false);
    }, [currentTab, user]);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/home/${name}`);
    }

    if (loading) {
        return <Loader/>
    }
    return (
        <div className="home">
            <div className="mod-home-container">
                <UnitHeader obj={({name: I18n.t("home.access"), svg: Logo})}
                            svgClick={() => new Audio(owl).play()}>
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
