import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./System.scss";
import {Loader} from "@surfnet/sds";
import {useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {ReactComponent as CronLogo} from "@surfnet/sds/icons/illustrative-icons/database-check.svg";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {Cron} from "../tabs/Cron";

export const System = () => {
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
                      Icon={CronLogo}>
                    <Cron/>
                </Page>
            ];
            setTabs(newTabs);
            setLoading(false);
        },
        [currentTab])

    const tabChanged = (name) => {
        setCurrentTab(name);
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
