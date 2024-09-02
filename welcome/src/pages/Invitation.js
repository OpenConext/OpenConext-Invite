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
import {splitListSemantically} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {organisationName} from "../utils/Manage";
import HighFive from "../icons/high-five.svg";

const MAY_ACCEPT = "mayAccept";
const HAS_LOGGED_IN_AGAIN = "hasLoggedInAgain"

let runOnce = false;

export const Invitation = ({authenticated}) => {

    const navigate = useNavigate();
    const {user, config} = useAppStore(state => state);

    const [invitation, setInvitation] = useState({});
    const [loading, setLoading] = useState(true);
    const [expired, setExpired] = useState(false);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
        const hashParam = getParameterByName("hash", window.location.search);
        if (runOnce) {
            return;
        }
        runOnce = true;
        invitationByHash(hashParam)
            .then(res => {
                    setInvitation(res);
                    useAppStore.setState(() => ({
                        invitation: res
                    }));
                    const mayAccept = localStorage.getItem(MAY_ACCEPT);
                    const hasLoggedInAgain = localStorage.getItem(HAS_LOGGED_IN_AGAIN);
                    if (mayAccept && config.name && hasLoggedInAgain) {
                        acceptInvitation(hashParam, res.id)
                            .then(res => {
                                localStorage.removeItem(MAY_ACCEPT);
                                localStorage.removeItem(HAS_LOGGED_IN_AGAIN);
                                me()
                                    .then(userWithRoles => {
                                        useAppStore.setState(() => ({
                                            user: userWithRoles,
                                            authenticated: true
                                        }));
                                        const inviteRedeemUrlQueryParam = res.inviteRedeemUrl && !res.errorResponse ? `&inviteRedeemUrl=${encodeURIComponent(res.inviteRedeemUrl)}` : "";
                                        const errorResponseQueryParam = res.errorResponse ? "&errorResponse=true" : "";
                                        localStorage.removeItem("location");
                                        navigate(`/proceed?hash=${hashParam}${inviteRedeemUrlQueryParam}${errorResponseQueryParam}`);
                                    })
                            })
                            .catch(e => {
                                    setLoading(false);
                                    if (e.response && e.response.status === 412) {
                                        setConfirmation({
                                            cancel: null,
                                            action: () => logout().then(() => login(config, true, hashParam)),
                                            warning: false,
                                            error: true,
                                            question: I18n.t("invitationAccept.emailMismatch", {
                                                email: res.email,
                                                userEmail: user.email
                                            }),
                                            confirmationHeader: I18n.t("confirmationDialog.error"),
                                            confirmationTxt: I18n.t("invitationAccept.login")
                                        });
                                        localStorage.setItem(MAY_ACCEPT, "true");
                                        setConfirmationOpen(true);
                                    } else {
                                        localStorage.removeItem(MAY_ACCEPT);
                                        handleError(e);
                                    }

                                }
                            )
                    } else {
                        localStorage.setItem(MAY_ACCEPT, "true");
                        setExpired(new Date() > new Date(res.expiryDate * 1000));
                        setLoading(false);
                    }
                }
            )
            .catch(e => {
                localStorage.removeItem(MAY_ACCEPT);
                const status = e.response?.status;
                if (status === 409) {
                    if (config.authenticated) {
                        navigate(`/profile`);
                    } else {
                        localStorage.removeItem("location");
                        login(config);
                    }
                } else {
                    navigate("/404");
                }
            })
        //Prevent in dev mode an accidental acceptance of an invitation
        return () => localStorage.removeItem(MAY_ACCEPT);
    }, [config]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleError = e => {
        e.response.json().then(j => {
            const reference = j.reference || 999;
            setConfirmation({
                cancel: null,
                action: () => setConfirmationOpen(false),
                warning: false,
                error: true,
                confirmationHeader: I18n.t("confirmationDialog.title"),
                question: I18n.t("forms.error", {reference: reference}),
                confirmationTxt: I18n.t("forms.ok")
            });
            setConfirmationOpen(true);
        })
    }

    const proceed = () => {
        setLoading(true);
        const hashParam = getParameterByName("hash", window.location.search);
        const direction = window.location.pathname + window.location.search;
        localStorage.setItem("location", direction);
        logout().then(() => {
            localStorage.setItem(HAS_LOGGED_IN_AGAIN, "true");
            login(config, true, hashParam)
        });
    }

    const renderLoginStep = () => {
        let html = DOMPurify.sanitize(I18n.t("invitationAccept.invited", {
            type: I18n.t("invitationAccept.role"),
            roles: splitListSemantically(invitation.roles
                .map(invitationRole => `<strong>${invitationRole.role.name}</strong>${organisationName(invitationRole.role.applicationMaps)}`), I18n.t("forms.and")),
            inviter: invitation.inviter.name,
            plural: invitation.roles.length === 1 ? I18n.t("invitationAccept.role") : I18n.t("invitationAccept.roles"),
            email: invitation.inviter.email
        }));
        if (invitation.enforceEmailEquality) {
            html += DOMPurify.sanitize(I18n.t("invitationAccept.enforceEmailEquality", {email: invitation.email}));
        }

        const expiryDate = DateTime.fromMillis(invitation.expiryDate * 1000).toLocaleString(DateTime.DATETIME_MED);
        const expiredMessage = I18n.t("invitationAccept.expired", {expiryDate: expiryDate});
        return (
            <>
                <div className="welcome-logo">
                    <img src={HighFive} alt={I18n.t("notFound.alt")}/>
                </div>
                {!expired && <h1>{I18n.t("invitationAccept.hi", {name: authenticated ? ` ${user.name}` : ""})}</h1>}
                {expired &&
                    <p className="expired"><ErrorIndicator msg={expiredMessage}/></p>}
                {expired &&
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("invitationAccept.expiredInfo", {email: invitation.email}))}}/>}
                {!expired && <>
                    <Toaster toasterType={ToasterType.Info} message={html}/>
                </>}
                {!expired &&
                    <section className="step-container">
                        <div className="step">
                            <div className="circle two-quarters">
                                <span>{I18n.t("invitationAccept.progress")}</span>
                            </div>
                            <div className="step-actions">
                                <h4>{I18n.t("invitationAccept.login")}</h4>
                                <span>{I18n.t("invitationAccept.nextStep")}</span>
                            </div>
                        </div>
                        <div className={"info-block"}>
                            {!authenticated &&
                                <span dangerouslySetInnerHTML={{__html: `${I18n.t("invitationAccept.info")} `}}/>}
                            {invitation.eduIDOnly && <span>{I18n.t("invitationAccept.infoLoginEduIDOnly")}</span>}
                            {!invitation.eduIDOnly &&
                                <span>{I18n.t(`invitationAccept.${authenticated ? "infoLoginAgain" : "infoLogin"}`)}</span>}
                        </div>
                        <Button onClick={proceed}
                                txt={I18n.t(`invitationAccept.${authenticated ? "login" : "loginWithSub"}`)}
                                centralize={true}/>
                    </section>}
            </>
        )
    }

    if (loading) {
        return <Loader/>
    }
    return (
        <div className="mod-user-invitation">
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     confirmationTxt={confirmation.confirmationTxt}
                                                     isWarning={confirmation.warning}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}
            <div className="invitation-container">
                {renderLoginStep()}
            </div>
        </div>);
};
