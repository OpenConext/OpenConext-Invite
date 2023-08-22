import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Proceed.scss";
import "../styles/circle.scss";
import DOMPurify from "dompurify";
import {Button, Card, CardType, Loader, Toaster, ToasterType} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {isEmpty, splitListSemantically} from "../utils/Utils";
import {organisationName} from "../utils/Manage";
import Logo from "../components/Logo";
import {getParameterByName} from "../utils/QueryParameters";
import {invitationByHash} from "../api";
import {login} from "../utils/Login";
import {MoreLessText} from "../components/MoreLessText";


export const Proceed = () => {

    const {user, invitationMeta, config} = useAppStore(state => state);
    const [loading, setLoading] = useState(true);
    const [reloadedInvitationMeta, setReloadedInvitationMeta] = useState(null);

    useEffect(() => {
        if (isEmpty(user)) {
          login(config);
        } else if (isEmpty(invitationMeta)) {
            const hashParam = getParameterByName("hash", window.location.search);
            invitationByHash(hashParam)
                .then(res => {
                    setReloadedInvitationMeta(res);
                    setLoading(false);
                })
        } else {
            setReloadedInvitationMeta(invitationMeta);
            setLoading(false);
        }
    }, [invitationMeta, user, config]);

    const renderInvitationRole = (invitationRole, index) => {
        const role = invitationRole.role;
        const provider = user.providers.find(data => data.id === role.manageId) || {};
        const logo = provider.data.metaDataFields["logo:0:url"];
        const children =
            <div key={index} className={"user-role"}>
                <Logo src={logo} alt={"provider"} className={"provider"}/>
                <section className={"user-role-info"}>
                    <h3>{role.name}</h3>
                    <MoreLessText txt={role.description}/>
                    <p><a href={role.landingPage}>{role.landingPage}</a></p>
                </section>
                <div className={"launch"}>
                    <Button txt={I18n.t("proceed.launch")} onClick={() => {
                        window.href = invitationRole.landingPage;
                    }}/>
                </div>

            </div>;
        return (
            <Card key={index} cardType={CardType.Big} children={children}/>
        );
    }

    const renderProceedStep = () => {
        const {invitation, providers} = reloadedInvitationMeta;
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

    if (loading || isEmpty(reloadedInvitationMeta)) {
        return <Loader/>
    }

    return (
        <div className="mod-proceed">
            <div className="proceed-container">
                {renderProceedStep()}
            </div>
        </div>);
};
