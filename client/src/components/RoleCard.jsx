import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Checkbox, Chip, ChipType} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {isEmpty} from "../utils/Utils";

export const RoleCard = ({role, index, invitationSelected, invitationSelectCallback, isNew = false}) => {
    const navigate = useNavigate();
    const application = role.isUserRole ? role.role.application : role.application;
    const logo = application.data.metaDataFields["logo:0:url"];

    const children =
        <div key={index} className="user-role">
            {!isEmpty(invitationSelected) &&
                <Checkbox name={`invitationSelected-${index}-${role.value}`}
                          value={invitationSelected}
                          onChange={e => invitationSelectCallback(e, role.value)}/>
            }
            <Logo src={logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{application.data.metaDataFields[`name:${I18n.locale}`]} ({application.data.metaDataFields[`OrganizationName:${I18n.locale}`]})</p>
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
        <div className={className}
             onClick={() => !isEmpty(invitationSelected) && invitationSelectCallback({target: {checked: !invitationSelected}}, role.value)}>
            {isNew &&
                <Chip label={I18n.t("proceed.new")} type={ChipType.Status_error}/>
            }
            <Card key={index} cardType={CardType.Big} children={children}/>
        </div>
    );
}