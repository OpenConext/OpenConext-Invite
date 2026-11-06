import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "./Profile.scss";
import DOMPurify from "dompurify";
import {Alert, AlertDescription, Button, Spinner} from "@surfnet/curve-react";
import {useAppStore} from "../stores/AppStore";
import {isEmpty} from "../utils/Utils";
import {getParameterByName} from "../utils/QueryParameters";
import {invitationByHash} from "../api";
import {login} from "../utils/Login";
import {User} from "../components/User";
import {Welcome} from "../components/Welcome";
import {useNavigate} from "react-router";
import {relativeUserWaitTime} from "../utils/Date";

export const Proceed = () => {

    const {user, invitation, config} = useAppStore(state => state);

    const navigate = useNavigate();

    const urlSearchParams = new URLSearchParams(window.location.search);
    const inviteRedeemUrlParam = urlSearchParams.get("inviteRedeemUrl");
    const inviteRedeemUrl = inviteRedeemUrlParam ? DOMPurify.sanitize(decodeURIComponent(inviteRedeemUrlParam)) : null;
    const errorResponse = urlSearchParams.get("errorResponse");
    const userWaitTime = urlSearchParams.get("userWaitTime");
    const role = urlSearchParams.get("role");

    const [loading, setLoading] = useState(isEmpty(invitation));
    const [reloadedInvitation, setReloadedInvitation] = useState(invitation);

    useEffect(() => {
        if (isEmpty(user)) {
            login(config);
        } else if (isEmpty(invitation)) {
            const hashParam = getParameterByName("hash", window.location.search);
            invitationByHash(hashParam)
                .then(res => {
                    setReloadedInvitation(res);
                    setLoading(false);
                })
                .catch(() => navigate("/profile"))
        }
    }, [invitation, user, config]); // eslint-disable-line react-hooks/exhaustive-deps

    if (loading || isEmpty(reloadedInvitation)) {
        return <div className="loading-container"><Spinner className="size-8"/></div>
    }

    return (
        <div className="mod-proceed mod-profile">
            <div className="profile-container">
                <Welcome user={user}/>
                {inviteRedeemUrl && <Alert variant="info">
                    <AlertDescription>
                        <p className="invite-feedback">{I18n.t("invitationAccept.inviteRedeemUrl")}</p>
                        <Button onClick={() => window.location.href = DOMPurify.sanitize(inviteRedeemUrl)}>
                            {I18n.t("invitationAccept.continue")}
                        </Button>
                    </AlertDescription>
                </Alert>}
                {errorResponse && <Alert variant="warning">
                    <AlertDescription>{I18n.t("invitationAccept.graphEmailViolation")}</AlertDescription>
                </Alert>}
                {userWaitTime && <Alert variant="info">
                    <AlertDescription>{I18n.t("invitationAccept.userWaitTime",
                        {role: role, waitTime: relativeUserWaitTime(userWaitTime)})}</AlertDescription>
                </Alert>}
                <User user={user} invitationRoles={reloadedInvitation.roles}/>
            </div>
        </div>);
};
