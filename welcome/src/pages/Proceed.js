import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "./Profile.scss";
import "../styles/circle.scss";
import DOMPurify from "dompurify";
import {Loader, Modal, Toaster, ToasterType, Tooltip} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, sanitizeURL, stopEvent} from "../utils/Utils";
import {getParameterByName} from "../utils/QueryParameters";
import {invitationByHash, logout} from "../api";
import {login} from "../utils/Login";
import {RoleCard} from "../components/RoleCard";
import {User} from "../components/User";
import HighFive from "../icons/high-five.svg";
import {useNavigate} from "react-router-dom";

export const Proceed = () => {

    const {user, invitation, config} = useAppStore(state => state);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [reloadedInvitation, setReloadedInvitation] = useState(null);
    const [showModal, setShowModal] = useState(true);
    const [inviteRedeemUrl, setInviteRedeemUrl] = useState(null);
    const [errorResponse, setErrorResponse] = useState(null);

    function invariantParams() {
        const urlSearchParams = new URLSearchParams(window.location.search);
        const isRedirect = urlSearchParams.get("isRedirect");
        if (isRedirect) {
            setShowModal(false);
        }
        const inviteRedeemUrlParam = urlSearchParams.get("inviteRedeemUrl");
        if (inviteRedeemUrlParam) {
            setInviteRedeemUrl(decodeURIComponent(inviteRedeemUrlParam));
        }
        setErrorResponse(urlSearchParams.get("errorResponse"));
    }

    useEffect(() => {
        if (isEmpty(user)) {
            login(config);
        } else if (isEmpty(invitation)) {
            const hashParam = getParameterByName("hash", window.location.search);
            invariantParams();
            invitationByHash(hashParam)
                .then(res => {
                    setReloadedInvitation(res);
                    setLoading(false);
                })
                .catch(() => navigate("/profile"))
        } else {
            invariantParams();
            setReloadedInvitation(invitation);
            setLoading(false);
        }
    }, [invitation, user, config]); // eslint-disable-line react-hooks/exhaustive-deps

    const doLogin = e => {
        stopEvent(e);
        logout().then(() => login(config, true));
    }

    const renderInvitationRole = (invitationRole, index, isNew, skipLaunch = false) => {
        const role = invitationRole.role;
        return (
            <React.Fragment key={index}>
                {role.applicationMaps.map((applicationMap, i) =>
                    <RoleCard role={role}
                              key={i}
                              applicationMap={applicationMap}
                              index={i}
                              isNew={isNew}
                              skipLaunch={skipLaunch}/>)}
            </React.Fragment>
        );
    }

    const renderProceedStep = () => {
        const toasterChildren = <div>
            <span>{I18n.t("profile.toaster", {institution: user.schacHomeOrganization})}</span>
            <a href="/logout" onClick={doLogin}>{I18n.t("profile.changeThis")}</a>
            <span>)</span>
        </div>
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
                    {reloadedInvitation.roles.map((invitationRole, index) => renderInvitationRole(invitationRole, index, true))}
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
            window.location.href = sanitizeURL(inviteRedeemUrl);
        }
    }

    return (
        <div className="mod-proceed mod-profile">
            {showModal &&
                <Modal confirm={() => confirmModal()}
                       confirmationButtonLabel={I18n.t("invitationAccept.continue")}
                       full={true}
                       title={I18n.t("invitationAccept.access")}>
                    {reloadedInvitation.roles.map((invitationRole, index) => renderInvitationRole(invitationRole, index, false, true))}
                    <p>{I18n.t(`invitationAccept.applicationInfo${reloadedInvitation.roles.length > 1 ? "Multiple" : ""}`)}</p>
                    {inviteRedeemUrl && <p className="invite-url">{I18n.t("invitationAccept.inviteRedeemUrl")}</p>}
                    {errorResponse && <p className="invite-error">{I18n.t("invitationAccept.graphEmailViolation")}</p>}
                </Modal>}
            <div className="proceed-container">
                {renderProceedStep()}
            </div>
        </div>);
};
