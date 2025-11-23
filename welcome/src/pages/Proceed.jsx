import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "./Profile.scss";
import "../styles/circle.scss";
import DOMPurify from "dompurify";
import {Loader, Modal, Toaster, ToasterType, Tooltip} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, stopEvent} from "../utils/Utils";
import {getParameterByName} from "../utils/QueryParameters";
import {invitationByHash, logout} from "../api";
import {login} from "../utils/Login";
import {RoleCard} from "../components/RoleCard";
import {User} from "../components/User";
import HighFive from "../icons/high-five.svg?url";
import {useNavigate} from "react-router-dom";
import {reduceApplicationFromUserRoles} from "../utils/Manage";
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
    const [showModal, setShowModal] = useState(isEmpty(urlSearchParams.get("isRedirect")));

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

    const doLogin = e => {
        stopEvent(e);
        logout().then(() => login(config, true));
    }

    const renderApplication = (index, application, isNew, skipLaunch = false) => {
        return (
            <RoleCard index={index}
                      application={application}
                      isNew={isNew}
                      skipLaunch={skipLaunch}/>
        );
    }

    const renderProceedStep = () => {
        const toasterChildren = <div>
            <span>{I18n.t("profile.toaster", {institution: user.schacHomeOrganization})}</span>
            <a href="/logout" onClick={doLogin}>{I18n.t("profile.changeThis")}</a>
            <span>)</span>
        </div>
        const applications = reduceApplicationFromUserRoles(reloadedInvitation.roles, I18n.locale);
        return (
            <>
                <div className="profile-container">
                    <div className="welcome-logo">
                        <img src={HighFive} alt={I18n.t("notFound.alt")}/>
                    </div>
                    <h2>{I18n.t("profile.welcome", {name: user.name})}</h2>
                    <div>
                    <span className={"info"}
                          dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("profile.info"))}}/>
                        <Tooltip tip={I18n.t("profile.tooltipApps")}/>
                    </div>

                    <Toaster toasterType={ToasterType.Info}
                             large={true}
                             children={toasterChildren}/>
                    {applications.map((application, index) => renderApplication(index, application, true))}
                    <User user={user} invitationRoles={reloadedInvitation.roles}/>
                </div>

            </>
        )
    }

    if (loading || isEmpty(reloadedInvitation)) {
        return <Loader/>
    }

    const confirmModal = () => {
        if (isEmpty(inviteRedeemUrl)) {
            setShowModal(false);
        } else {
            window.location.href = DOMPurify.sanitize(inviteRedeemUrl);
        }
    }

    const reloadedApplications = reduceApplicationFromUserRoles(reloadedInvitation.roles, I18n.locale);
    return (
        <div className="mod-proceed mod-profile">
            {showModal &&
                <Modal confirm={() => confirmModal()}
                       confirmationButtonLabel={I18n.t("invitationAccept.continue")}
                       full={true}
                       title={I18n.t("invitationAccept.access")}>
                    {inviteRedeemUrl && <p className="invite-feedback">{I18n.t("invitationAccept.inviteRedeemUrl")}</p>}
                    {errorResponse &&
                        <p className="invite-feedback">{I18n.t("invitationAccept.graphEmailViolation")}</p>}
                    {userWaitTime && <p className="invite-feedback">{I18n.t("invitationAccept.userWaitTime",
                        {
                            role: role,
                            waitTime: relativeUserWaitTime(userWaitTime)
                        })}</p>}
                    <p>{I18n.t(`invitationAccept.applicationInfo${reloadedApplications.length > 1 ? "Multiple" : ""}`)}</p>
                    {reloadedApplications.map((application, index) => renderApplication(index, application, false, true))}
                </Modal>}
            <div className="proceed-container">
                {renderProceedStep()}
            </div>
        </div>);
};
