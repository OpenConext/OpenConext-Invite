import {Button, Spinner} from "@surfnet/curve-react";
import React, {useEffect, useState} from "react";
import {acceptInvitation, invitationByHash, logout} from "../api";
import I18n from "../locale/I18n";
import "./Invitation.scss";
import {login} from "../utils/Login";
import ErrorIndicator from "../components/ErrorIndicator";
import DOMPurify from "dompurify";
import {getParameterByName} from "../utils/QueryParameters";
import {DateTime, Duration} from "luxon";
import {useNavigate} from "react-router";
import {useAppStore} from "../stores/AppStore";
import {initials, isEmpty, splitListSemantically} from "../utils/Utils";
import ConfirmationDialog from "../components/ConfirmationDialog";

const MAY_ACCEPT = "mayAccept";

export const Invitation = ({authenticated}) => {

    const navigate = useNavigate();
    const config = useAppStore(state => state.config);

    const [invitation, setInvitation] = useState({});
    const [loading, setLoading] = useState(true);
    const [expired, setExpired] = useState(false);
    const [confirmation, setConfirmation] = useState({});
    const [confirmationOpen, setConfirmationOpen] = useState(false);

    useEffect(() => {
        const hashParam = getParameterByName("hash", window.location.search);
        invitationByHash(hashParam)
            .then(res => {
                setInvitation(res);
                const reloaded = performance.getEntriesByType("navigation").map(entry => entry.type).includes("reload");
                const mayAccept = localStorage.getItem(MAY_ACCEPT);
                if (mayAccept && config.name && !reloaded) {
                    acceptInvitation(hashParam, res.id)
                        .then(() => {
                            localStorage.removeItem(MAY_ACCEPT);
                            localStorage.removeItem("location");
                            useAppStore.setState(() => ({reload: true}));
                            navigate("/home");
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
                                        email: res.email
                                    }),
                                    confirmationHeader: I18n.t("confirmationDialog.error"),
                                    confirmationTxt: I18n.t("invitationAccept.login")
                                });
                                localStorage.setItem(MAY_ACCEPT, "true");
                                setConfirmationOpen(true);
                            } else if (e.response && e.response.status === 406) {
                                e.response.json().then(j => {
                                    setConfirmation({
                                        cancel: null,
                                        action: () => setConfirmationOpen(false),
                                        warning: false,
                                        error: true,
                                        question: I18n.t("invitationAccept.crmOrganisationMismatch", {
                                            message: j.message
                                        }),
                                        confirmationHeader: I18n.t("confirmationDialog.error"),
                                        confirmationTxt: I18n.t("forms.ok")
                                    });
                                    localStorage.setItem(MAY_ACCEPT, "true");
                                    setConfirmationOpen(true);
                                })
                            } else {
                                localStorage.removeItem(MAY_ACCEPT);
                                handleError(e);
                            }

                        })
                } else {
                    localStorage.setItem(MAY_ACCEPT, "true");
                    setExpired(DateTime.now().toJSDate() > new Date(res.expiryDate * 1000));
                    setLoading(false);
                }
            })
            .catch(e => {
                localStorage.removeItem(MAY_ACCEPT);
                const status = e.response?.status;
                if (status === 409) {
                    if (config.authenticated) {
                        navigate(`/`);
                    } else {
                        //Prevent endless loop for already accepted / expired invitations
                        localStorage.removeItem("location");
                        login(config);
                    }
                } else {
                    navigate("/404");
                }
            })
        //Prevent in dev mode an accidental acceptance of an invitation
        return () => localStorage.removeItem(MAY_ACCEPT);
    }, [config]);// eslint-disable-line react-hooks/exhaustive-deps

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
            login(config, true, hashParam)
        });
    }

    const validity = () => {
        if (isEmpty(invitation.roleExpiryDate)) {
            return null;
        }
        const days = Math.round(DateTime.fromSeconds(invitation.roleExpiryDate).diffNow("days").days);
        const object = days < 60 ? {days: days} : days < 730 ? {months: Math.round(days / 30.44)} : {years: Math.round(days / 365.25)};
        const duration = Duration.fromObject(object).reconfigure({locale: I18n.locale}).toHuman();
        return I18n.t("invitationAccept.validity", {duration: duration});
    }

    const applicationNames = role => {
        const names = (role.applicationMaps || [])
            .filter(app => !app.unknown)
            .map(app => app[`name:${I18n.locale}`] || app["name:en"]);
        return [...new Set(names)].sort((a, b) => a.localeCompare(b)).join(", ");
    }

    const renderLoginStep = () => {
        const noRoles = invitation.roles.length === 0;
        const isGuest = invitation.intendedAuthority === "GUEST";
        const authority = I18n.t(`access.${invitation.intendedAuthority}`);
        const roleNames = splitListSemantically(invitation.roles.map(role => role.role.name), I18n.t("forms.and"));
        const plural = invitation.roles.length === 1 ? I18n.t("invitationAccept.role") : I18n.t("invitationAccept.roles");
        const title = noRoles ?
            I18n.t("invitationAccept.invitedNoRolesTitle", {authority: authority.toLowerCase()}) :
            I18n.t("invitationAccept.invitedTitle", {plural: plural, roles: roleNames});
        const hasNoAccess = ["INVITER", "MANAGER"].includes(invitation.intendedAuthority);
        const expiryDate = DateTime.fromMillis(invitation.expiryDate * 1000).toLocaleString(DateTime.DATETIME_MED);
        const expiredMessage = I18n.t("invitationAccept.expired", {expiryDate: expiryDate});
        return (
            <>
                <h1>{expired ? I18n.t("invitationAccept.expiredTitle") : title}</h1>
                {expired &&
                    <p className="expired"><ErrorIndicator msg={expiredMessage}/></p>}
                {expired &&
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("invitationAccept.expiredInfo", {email: invitation.email}))}}/>}
                {!expired && invitation.roles.map(invitationRole =>
                    <div className="role-card" key={invitationRole.id}>
                        <div className="role-card-header">
                            <h2>{invitationRole.role.name}</h2>
                            {hasNoAccess && <p>{I18n.t("invitationAccept.noAccessInviter", {authority: authority.toLowerCase()})}</p>}
                            {validity() && <p>{validity()}</p>}
                        </div>
                        <div className="role-card-applications">
                            <p className="muted">{I18n.t(`invitationAccept.applicationsHeader${isGuest ? "Guest" : ""}`)}</p>
                            <p>{applicationNames(invitationRole.role)}</p>
                        </div>
                    </div>)}
                {(!expired && noRoles && hasNoAccess) &&
                    <p>{I18n.t("invitationAccept.noAccessInviter", {authority: authority.toLowerCase()})}</p>}
                {(!expired && invitation.enforceEmailEquality) &&
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("invitationAccept.enforceEmailEquality", {email: invitation.email}))}}/>}
                {!expired && <>
                    <p className="accept-info">{I18n.t(`invitationAccept.acceptInfo${noRoles ? "NoRoles" : ""}`)}</p>
                    <div className="accept-actions">
                        <Button onClick={proceed}>{I18n.t("invitationAccept.loginToAccept")}</Button>
                        <p className="hint">
                            {I18n.t(`invitationAccept.${authenticated ? "infoLoginAgain" : "infoLogin"}`)}
                        </p>
                    </div>
                    <div className="inviter">
                        <p>{I18n.t("invitationAccept.receivedFrom")}</p>
                        <div className="inviter-identity">
                            <div className="avatar" aria-hidden="true">{initials(invitation.inviter.name)}</div>
                            <div className="inviter-name">
                                <span className="name">{invitation.inviter.name}</span>
                                <span className="email">{invitation.inviter.email}</span>
                            </div>
                        </div>
                    </div>
                </>}
            </>
        )
    }

    if (loading) {
        return <div className="loading-container"><Spinner className="size-8"/></div>
    }
    return (
        <div className="mod-user-invitation">
            {confirmationOpen && <ConfirmationDialog isOpen={confirmationOpen}
                                                     cancel={confirmation.cancel}
                                                     confirm={confirmation.action}
                                                     confirmationTxt={confirmation.confirmationTxt}
                                                     confirmationHeader={confirmation.confirmationHeader}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}
            <div className="invitation-container">
                {renderLoginStep()}
            </div>
        </div>);
};
