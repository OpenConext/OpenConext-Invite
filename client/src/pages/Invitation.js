import React, {useEffect, useState} from "react";
import {acceptInvitation, invitationByHash, logout, me} from "../api";
import I18n from "../locale/I18n";
import "./Invitation.scss";
import "../styles/circle.scss";
import {login} from "../utils/Login";
import ErrorIndicator from "../components/ErrorIndicator";
import DOMPurify from "dompurify";
import {Button, Loader, Toaster, ToasterType} from "@surfnet/sds";
import {getParameterByName} from "../utils/QueryParameters";
import {DateTime} from "luxon";
import {useNavigate} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";

const MAY_ACCEPT = "mayAccept";

export const Invitation = ({authenticated}) => {

    const navigate = useNavigate();
    const user = useAppStore((state) => state.user);
    const config = useAppStore((state) => state.config);

    const [invitationMeta, setInvitationMeta] = useState({});
    const [loading, setLoading] = useState(true);
    const [isExpired, setIsExpired] = useState(false);

    useEffect(() => {
        const hashParam = getParameterByName("hash", window.location.search);
        invitationByHash(hashParam)
            .then(res => {
                setInvitationMeta(res);
                const mayAccept = localStorage.getItem(MAY_ACCEPT);
                if (mayAccept && config.name) {
                    setLoading(true);
                    const {invitation} = res;
                    acceptInvitation(hashParam, invitation.id).then(() => {
                        localStorage.removeItem(MAY_ACCEPT);
                        me().then(userWithRoles => {
                            useAppStore.setState(() => ({user: userWithRoles}));
                            navigate("/home");
                        })
                    })
                } else {
                    localStorage.setItem(MAY_ACCEPT, "true");
                }
                setLoading(false);
                setIsExpired(DateTime.now().toJSDate() < new Date(res["invitation"].expiryDate));
            }).catch(e => {
            debugger;
            localStorage.removeItem(MAY_ACCEPT);
            navigate(e.response?.status === 404 ? "/404" : "/expired-invitation");
        })
        //Prevent in dev mode an accidental acceptance of an invitation
        return () => localStorage.removeItem(MAY_ACCEPT);
    }, [navigate, config.name]);

    const proceed = () => {
        setLoading(true);
        const hashParam = getParameterByName("hash", window.location.search);
        logout().then(() => login(config, true, hashParam));
    }

    const renderLoginStep = () => {
        const nextStep = I18n.t("invitations.steps.invite");
        return (
            <>
                {!isExpired && <h1>{I18n.t("welcomeDialog.hi")}</h1>}
                {isExpired &&
                    <p className="expired"><ErrorIndicator msg={expiredMessage}/></p>}
                {!isExpired && <>
                    <Toaster toasterType={ToasterType.Info} message={html}/>
                </>}
                <section className="step-container">
                    <div className="step">
                        <div className="circle two-quarters">
                            <span>{I18n.t("invitations.steps.progress", {now: "1", total: "2"})}</span>
                        </div>
                        <div className="step-actions">
                            <h1>{I18n.t("invitations.steps.login")}</h1>
                            <span>{I18n.t("invitations.steps.next", {step: nextStep})}</span>
                        </div>
                    </div>
                    <p className="info"
                       dangerouslySetInnerHTML={{__html: I18n.t("invitations.followingSteps")}}/>
                    <Button onClick={proceed}
                            txt={I18n.t("invitations.loginWithSub")}
                            centralize={true}/>
                </section>
            </>
        )
    }

    const renderAcceptInvitation = () => {
        return (
            <section>TODO {JSON.stringify(user)}</section>

        )
    }

    if (loading) {
        return <Loader/>
    }
    const {invitation, providers} = invitationMeta;
    const expiryDate = DateTime.fromMillis(invitation.expiryDate * 1000).toLocaleString(DateTime.DATETIME_MED);
    const expiredMessage = I18n.t("invitations.expired", {expiryDate: expiryDate});
    const html = DOMPurify.sanitize(I18n.t("invitations.invited", {
        type: I18n.t("welcomeDialog.role"),
        collaboration: invitation.roles.map(role => role.role.name).join(", "),
        inviter: invitation.inviter.name,
        email: invitation.inviter.email
    }));
    return (
        <div className="mod-user-invitation">
            <div className="invitation-container">
                <p>Authenticated {authenticated.toString()}</p>
                <p>Name {config.name}</p>
                {authenticated && renderAcceptInvitation()}
                {!authenticated && renderLoginStep()}
            </div>
        </div>);
};
