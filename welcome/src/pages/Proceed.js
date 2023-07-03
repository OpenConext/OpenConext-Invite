import React from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "../styles/circle.scss";
import DOMPurify from "dompurify";
import {Card, CardType, Toaster, ToasterType} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {splitListSemantically} from "../utils/Utils";
import {organisationName} from "../utils/Manage";
import Logo from "../components/Logo";


export const Proceed = () => {

    const {user, invitationMeta, config} = useAppStore(state => state);

    const renderInvitationRole = (invitationRole, index) => {
        const role = invitationRole.role;
        const provider = user.providers.find(data => data.id === role.manageId) || {};
        const logo = provider.data.metaDataFields["logo:0:url"];
        const children =
            <div key={index} className={"user-role"}>
                <Logo src={logo} alt={"provider"} className={"provider"}/>
                <section className={"user-role-info"}>
                    <h3>{role.name}</h3>
                    <p>{role.description}</p>
                    <a href={role.landingPage}>{role.landingPage}</a>
                </section>
            </div>;
        return (
            <Card cardType={CardType.Big} children={children}/>
        );
    }

    const renderProceedStep = () => {
        const {invitation, providers} = invitationMeta;
        const html = DOMPurify.sanitize(I18n.t("proceed.info", {
            plural: invitation.roles.length === 1 ? I18n.t("invitationAccept.role") : I18n.t("invitationAccept.roles"),
            roles: splitListSemantically(invitation.roles.map(role => `<strong>${role.role.name}</strong>${organisationName(role, providers)}`), I18n.t("forms.and"))
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
                    {invitation.roles.map((invitationRole, index) => renderInvitationRole(invitationRole, index))}
                </section>
            </>
        )
    }
    return (
        <div className="mod-proceed">
            <div className="proceed-container">
                {renderProceedStep()}
            </div>
        </div>);
};
