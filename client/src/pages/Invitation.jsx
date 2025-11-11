import React, {useEffect, useState} from "react";
import {acceptInvitation, invitationByHash, logout} from "../api";
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

const MAY_ACCEPT = "mayAccept";

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

    const organisationName = role => {
        if (role.applicationMaps.length === 1) {
            const name = role.applicationMaps[0][`OrganizationName:${I18n.locale}`] || role.applicationMaps[0]["OrganizationName:en"];
            return ` (${name})`;
        }
        const set = new Set(role.applicationMaps.map(app => app[`OrganizationName:${I18n.locale}`] || app["OrganizationName:en"]));
        return ` (${splitListSemantically([...set], I18n.t("forms.and"))})`
    }

    const renderLoginStep = () => {
        const translation = invitation.roles.length === 0 ? "invitedNoRoles" : "invited";
        const intentedAuthority = I18n.t(`access.${invitation.intendedAuthority}`);
        let html = DOMPurify.sanitize(I18n.t(`invitationAccept.${translation}`, {
            authority: `<strong>${intentedAuthority}</strong>`,
            plural: invitation.roles.length === 1 ? I18n.t("invitationAccept.role") : I18n.t("invitationAccept.roles"),
            roles: splitListSemantically(invitation.roles.map(role => `<strong>${role.role.name}</strong>${organisationName(role.role)}`), I18n.t("forms.and")),
            inviter: invitation.inviter.name,
            email: invitation.inviter.email
        }));
        if (invitation.enforceEmailEquality) {
            html += DOMPurify.sanitize(I18n.t("invitationAccept.enforceEmailEquality", {email: invitation.email}));
        }
        const expiryDate = DateTime.fromMillis(invitation.expiryDate * 1000).toLocaleString(DateTime.DATETIME_MED);
        const expiredMessage = I18n.t("invitationAccept.expired", {expiryDate: expiryDate});
        return (
            <>
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
                        {!authenticated && <p className="info"
                                              dangerouslySetInnerHTML={{__html: I18n.t("invitationAccept.info")}}/>}
                        <p className="info"
                           dangerouslySetInnerHTML={{__html: I18n.t(`invitationAccept.${authenticated ? "infoLoginAgain" : "infoLogin"}`)}}/>
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
                                                     confirmationHeader={confirmation.confirmationHeader}
                                                     isError={confirmation.error}
                                                     question={confirmation.question}/>}
            <div className="invitation-container">
                {renderLoginStep()}
            </div>
        </div>);
};
