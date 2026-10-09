import {Badge, Button, Card, CardContent, Checkbox} from "@surfnet/curve-react";
import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {isEmpty, splitListSemantically} from "../utils/Utils";
import {roleName} from "../utils/Manage";
import MultipleIcon from "../icons/multi-role.svg";
import {useNavigate} from "react-router";

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
                <Checkbox id={`invitationSelected-${index}-${role.value}`}
                          checked={invitationSelected}
                          onCheckedChange={checked => invitationSelectCallback(checked, role.value)}/>
            }

            <Logo src={logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{name}</p>
                <h3>{role.name}</h3>
                <MoreLessText txt={role.description} cutOffNumber={80}/>
            </section>
            {isEmpty(invitationSelected) && <div className={"launch"}>
                <Button onClick={() => navigate(`/roles/${role.id}`)}>{I18n.t("inviter.details")}</Button>
            </div>}

        </div>;

    const inviterCard = invitationSelected ? "inviter-selected" : "inviter"
    const className = `card-container ${isNew ? "is-new" : ""} ${inviterCard} ${invitationSelectCallback ? "pointer" : ""}`;
    return (
        <div className={className}>
            {isNew &&
                <Badge variant="danger">{I18n.t("proceed.new")}</Badge>
            }
                <label htmlFor={`invitationSelected-${index}-${role.value}`}>
                    <Card key={index}><CardContent>{children}</CardContent></Card>
                </label>
        </div>
    );
}