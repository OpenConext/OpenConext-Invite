import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {allInvitations} from "../api";
import {useAppStore} from "../stores/AppStore";
import {INVITATION_STATUS} from "../utils/UserRole";
import {Page} from "../components/Page";
import Tabs from "../components/Tabs";
import {Invitations} from "../tabs/Invitations";

export const InvitationOverview = () => {
    const navigate = useNavigate();
    const {tab = "pending"} = useParams();
    const {user} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [currentTab, setCurrentTab] = useState(tab);
    const [tabs, setTabs] = useState([]);

    useEffect(() => {
        useAppStore.setState({
            breadcrumbPath: [
                {path: "/inviter", value: I18n.t("tabs.home")},
                {value: I18n.t("tabs.invitations")}
            ]
        });
        allInvitations().then(res => {
            const newTabs = [
                <Page key="pending"
                      name="pending"
                      label={I18n.t("tabs.pendingInvitations")}>
                    <Invitations
                        preloadedInvitations={res.filter(invitation => invitation.status !== INVITATION_STATUS.ACCEPTED)}
                        standAlone={false}
                        pending={true}
                        history={false}/>
                </Page>,
                <Page key="accepted"
                      name="accepted"
                      label={I18n.t("tabs.acceptedInvitations")}>
                    <Invitations
                        preloadedInvitations={res.filter(invitation => invitation.status === INVITATION_STATUS.ACCEPTED)}
                        standAlone={false}
                        pending={false}
                        history={false}/>
                </Page>,
            ];
            setTabs(newTabs.filter(tab => tab !== null));
            setLoading(false);
        })
    }, [user]) // eslint-disable-line react-hooks/exhaustive-deps

    const tabChanged = (name) => {
        setCurrentTab(name);
        navigate(`/invitations/${name}`);
    }

    if (loading) {
        return <Loader/>
    }
    return (
        <Tabs activeTab={currentTab}
              tabChanged={tabChanged}>
            {tabs}
        </Tabs>
    )

}
