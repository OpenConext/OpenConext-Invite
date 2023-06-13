import {useAppStore} from "../stores/AppStore";
import I18n from "../locale/I18n";
import React, {useEffect, useState} from "react";
import owl from "../icons/owl.wav";
import {ReactComponent as Logo} from "../icons/Owl_Emblem.svg";
import {ReactComponent as ApplicationLogo} from "@surfnet/sds/icons/illustrative-icons/hierarchy-2.svg";
import {ReactComponent as UserLogo} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import Tabs from "../components/Tabs";
import {UnitHeader} from "../components/UnitHeader";
import {useNavigate, useParams} from "react-router-dom";
import Applications from "../tabs/Applications";
import {Users} from "../tabs/Users";
import {Page} from "../components/Page";

export const Home = () => {

    const {tab = "applications"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab );
    const [tabs, setTabs] = useState([]);
    const user = useAppStore((state) => state.user)
    const navigate = useNavigate();

    useEffect(() => {
        useAppStore.setState({breadcrumbPath: [
            {path: "/home", value: I18n.t("tabs.home")},
            {path: `/home/${currentTab}`, value: I18n.t(`tabs.${currentTab}`)}
        ]});
        if (user.superUser) {
            setTabs([
                <Page key="applications"
                     name="applications"
                     label={I18n.t("tabs.applications")}
                     Icon={ApplicationLogo}>
                    <Applications/>
                </Page>,
                <div key="users"
                     name="users"
                     label={I18n.t("tabs.users")}
                     Icon={UserLogo}>
                        <Users />
                    </div>
            ])
        }
    }, [currentTab, user.superUser]);

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/home/${name}`);
    }


    return (
        <div className="home">
            <div className="mod-home-container">
                <UnitHeader obj={({name: I18n.t("home.access"), svg: Logo})}
                             svgClick={() => new Audio(owl).play()}>
                    <p>{JSON.stringify(user)}</p>
                </UnitHeader>
                <Tabs activeTab={currentTab}
                      tabChanged={tabChanged}>
                    {tabs}
                </Tabs>
            </div>
            );

        </div>
    );
}
