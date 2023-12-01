import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Checkbox, Chip, ChipType} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {isEmpty, splitListSemantically} from "../utils/Utils";
import {roleName} from "../utils/Manage";
import {ReactComponent as MultipleIcon} from "../icons/multi-role.svg";

export const RoleCard = ({role, index, invitationSelected, invitationSelectCallback, isNew = false}) => {
    const navigate = useNavigate();

    const applications = role.isUserRole ? role.role.applicationMaps : role.applicationMaps;
    const multiApp = applications.length === 1;
    const application = applications[0];
    const logo = multiApp ? application.logo : <MultipleIcon/>
    const name = multiApp ? splitListSemantically(applications.map(app => roleName(app, I18n.locale)), I18n.t("forms.and")) :
        roleName(application, I18n.locale);

    const children =
        <div key={index} className="user-role" >
            {!isEmpty(invitationSelected) &&
                <Checkbox name={`invitationSelected-${index}-${role.value}`}
                          value={invitationSelected}
                          onChange={e => invitationSelectCallback(e, role.value)}/>
            }
            <Logo src={logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{name}</p>
                <h3>{role.name}</h3>
                <MoreLessText txt={role.description} cutOffNumber={80}/>
            </section>
            {isEmpty(invitationSelected) && <div className={"launch"}>
                <Button txt={I18n.t("inviter.details")} onClick={() => navigate(`/roles/${role.id}`)}/>
            </div>}

        </div>;
    const inviterCard = isEmpty(invitationSelected) ? "" : (invitationSelected ? "inviter-selected" : "inviter")
    const className = `card-container ${isNew ? "is-new" : ""} ${inviterCard}`;
    return (
        <div className={className}>
            {isNew &&
                <Chip label={I18n.t("proceed.new")} type={ChipType.Status_error}/>
            }
            {isEmpty(inviterCard) && <Card key={index} cardType={CardType.Big} children={children}/>}
            {!isEmpty(inviterCard) && <label htmlFor={`invitationSelected-${index}-${role.value}`}> <Card key={index} cardType={CardType.Big} children={children}/></label>}
        </div>
    );
}