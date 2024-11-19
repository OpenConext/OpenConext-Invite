import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./System.scss";
import {Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {Cron} from "../tabs/Cron";
import {RolesUnknownInManage} from "../tabs/RolesUnknownInManage";
import {Invitations} from "../tabs/Invitations";
import {ExpiredUserRoles} from "../tabs/ExpiredUserRoles";
import {PerformanceSeed} from "../tabs/PerformanceSeed";


export const System = () => {
    const {config} = useAppStore(state => state);
    const navigate = useNavigate();
    const {tab = "cron"} = useParams();
    const [loading, setLoading] = useState(false);
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);

    useEffect(() => {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/system", value: I18n.t("header.links.system")},
                    {value: I18n.t(`tabs.${currentTab}`)}
                ]
            });
            const newTabs = [
                <Page key="cron"
                      name="cron"
                      label={I18n.t("tabs.cron")}
                >
                    <Cron/>
                </Page>,
                <Page key="invitations"
                      name="invitations"
                      label={I18n.t("tabs.invitations")}
                >
                    <Invitations standAlone={true}/>
                </Page>,
                <Page key="unknownRoles"
                      name="unknownRoles"
                      label={I18n.t("tabs.unknownRoles")}
                >
                    <RolesUnknownInManage/>
                </Page>,
                <Page key="expiredUserRoles"
                      name="expiredUserRoles"
                      label={I18n.t("tabs.expiredUserRoles")}
                >
                    <ExpiredUserRoles/>
                </Page>,
                config.performanceSeedAllowed ?
                    <Page key="seed"
                          name="seed"
                          label={I18n.t("tabs.performanceSeed")}
                    >
                        <PerformanceSeed/>
                    </Page> : null
            ].filter(t => Boolean(t));
            setTabs(newTabs);
            setLoading(false);
        },
        [currentTab, config.performanceSeedAllowed])

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/system/${name}`);
    }

    if (loading) {
        return <Loader/>
    }

    return (<>
        <div className="mod-system">
            <Tabs activeTab={currentTab}
                  tabChanged={tabChanged}>
                {tabs}
            </Tabs>
        </div>
    </>);
};
