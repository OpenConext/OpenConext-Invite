import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "../styles/circle.scss";
import DOMPurify from "dompurify";
import {Loader, Toaster, ToasterType} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, splitListSemantically} from "../utils/Utils";
import {organisationName} from "../utils/Manage";
import {getParameterByName} from "../utils/QueryParameters";
import {invitationByHash} from "../api";
import {login} from "../utils/Login";
import {RoleCard} from "../components/RoleCard";
import {User} from "../components/User";


export const Proceed = () => {

    const {user, invitation, config} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [reloadedInvitation, setReloadedInvitation] = useState(null);

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
        } else {
            setReloadedInvitation(invitation);
            setLoading(false);
        }
    }, [invitation, user, config]);

    const renderInvitationRole = (invitationRole, index, isNew) => {
        const role = invitationRole.role;
        return (
            <RoleCard role={role} index={index} isNew={isNew}/>
        );
    }

    const renderProceedStep = () => {
        const html = DOMPurify.sanitize(I18n.t("proceed.info", {
            plural: reloadedInvitation.roles.length === 1 ? I18n.t("invitationAccept.role") : I18n.t("invitationAccept.roles"),
            roles: splitListSemantically(reloadedInvitation.roles.map(invitationRole => `<strong>${invitationRole.role.name}</strong>${organisationName(invitationRole)}`), I18n.t("forms.and"))
        }));
        return (
            <>
                <h1>{I18n.t("invitationAccept.hi", {name: ` ${user.name}`})}</h1>
                <>
                    <Toaster toasterType={ToasterType.Info} message={html}/>
                </>
                <section className="step-container">
                    <div className="step">
                        <div className="circle full">
                            <span>{I18n.t("proceed.progress")}</span>
                        </div>
                        <div className="step-actions">
                            <h4>{I18n.t("proceed.goto")}</h4>
                            <span>{I18n.t("proceed.nextStep")}</span>
                        </div>
                    </div>
                    {reloadedInvitation.roles.map((invitationRole, index) => renderInvitationRole(invitationRole, index, true))}
                    <User user={user} invitationRoles={invitation.roles}/>
                </section>
            </>
        )
    }

    if (loading || isEmpty(reloadedInvitation)) {
        return <Loader/>
    }

    return (
        <div className="mod-proceed">
            <div className="proceed-container">
                {renderProceedStep()}
            </div>
        </div>);
};
