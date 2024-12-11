import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Checkbox, Chip, ChipType} from "@surfnet/sds";
import {isEmpty, splitListSemantically} from "../utils/Utils";
import {roleName} from "../utils/Manage";
import {ReactComponent as MultipleIcon} from "../icons/multi-role.svg";
import {useNavigate} from "react-router-dom";

export const InvitationRoleCard = ({
                                       role,
                                       applicationMaps,
                                       index,
                                       invitationSelected,
                                       invitationSelectCallback,
                                       isNew = false
                                   }) => {
    const navigate = useNavigate();

    const multiApp = applicationMaps.length > 1;
    const application = applicationMaps[0];
    const logo = multiApp ? <MultipleIcon/> : application.logo;
    const name = multiApp ? splitListSemantically(applicationMaps.map(app => roleName(app, I18n.locale)), I18n.t("forms.and")) :
        roleName(application, I18n.locale);

    const children =
        <div key={index} className="user-role">
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

    const inviterCard = invitationSelected ? "inviter-selected" : "inviter"
    const className = `card-container ${isNew ? "is-new" : ""} ${inviterCard} ${invitationSelectCallback ? "pointer" : ""}`;
    return (
        <div className={className}>
            {isNew &&
                <Chip label={I18n.t("proceed.new")} type={ChipType.Status_error}/>
            }
                <label htmlFor={`invitationSelected-${index}-${role.value}`}>
                    <Card key={index} cardType={CardType.Big}  children={children}/>
                </label>
        </div>
    );
}