import React, {useEffect, useMemo, useState} from "react";
import I18n from "../locale/I18n";
import "./System.scss";
import {useNavigate, useParams} from "react-router";
import {useAppStore} from "../stores/AppStore";
import Tabs from "../components/Tabs";
import {Page} from "../components/Page";
import {Cron} from "../tabs/Cron";
import {RolesUnknownInManage} from "../tabs/RolesUnknownInManage";
import {Invitations} from "../tabs/Invitations";
import {ExpiredUserRoles} from "../tabs/ExpiredUserRoles";
import {PerformanceSeed} from "../tabs/PerformanceSeed";
import {SearchGroupContext} from "../utils/SearchGroupContext";


export const System = () => {
    const {config} = useAppStore(state => state);
    const navigate = useNavigate();
    const {tab = "cron"} = useParams();
    const [currentTab, setCurrentTab] = useState(tab);

    const tabs = useMemo(() => {
        return [
            <Page key="cron" name="cron" label={I18n.t("tabs.cron")}>
                <Cron />
            </Page>,
            <Page key="invitations" name="invitations" label={I18n.t("tabs.invitations")}>
                <Invitations systemView={true} />
            </Page>,
            <Page key="unknownRoles" name="unknownRoles" label={I18n.t("tabs.unknownRoles")}>
                <RolesUnknownInManage />
            </Page>,
            <Page key="expiredUserRoles" name="expiredUserRoles" label={I18n.t("tabs.expiredUserRoles")}>
                <ExpiredUserRoles />
            </Page>,
            config.performanceSeedAllowed && (
                <Page key="seed" name="seed" label={I18n.t("tabs.performanceSeed")}>
                    <PerformanceSeed />
                </Page>
            ),
        ].filter(Boolean);
    }, [config.performanceSeedAllowed]);


    useEffect(() => {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/system", value: I18n.t("header.links.system")},
                    {value: I18n.t(`tabs.${currentTab}`)}
                ]
            });
        },
        [currentTab])

    //Ensure the URL always reflects the actually active tab - even the default one - so a tab
    //switch always results in a distinct, stable URL (see Home.jsx for the same pattern)
    useEffect(() => {
            navigate(`/system/${currentTab}`, {replace: true});
        },
        []);// eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/system/${name}`);
    }

    return (<>
        <div className="mod-system">
            <SearchGroupContext.Provider value="system">
                <Tabs activeTab={currentTab}
                      tabChanged={tabChanged}>
                    {tabs}
                </Tabs>
            </SearchGroupContext.Provider>
        </div>
    </>);
};
