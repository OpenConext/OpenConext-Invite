import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Chip, ChipType} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {roleName} from "../utils/Manage";

export const RoleCard = ({
                             role,
                             applicationMap,
                             index,
                             isNew = false,
                             userRole = null,
                         }) => {
    const navigate = useNavigate();

    const children =
        <div key={index} className="user-role" >
            <Logo src={applicationMap.logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{role.name} {userRole && <span>- {I18n.t(`access.${userRole.authority}`)}</span>}</p>
                <h3>{roleName(applicationMap, I18n.locale)}</h3>
                <MoreLessText txt={role.description} cutOffNumber={80}/>
            </section>
            <div className="launch">
                <Button txt={I18n.t("inviter.details")} onClick={() => navigate(`/roles/${role.id}`)}/>
            </div>

        </div>;

    const className = `card-container ${isNew ? "is-new" : ""}`;
    return (
        <div className={className}>
            {isNew &&
                <Chip label={I18n.t("proceed.new")} type={ChipType.Status_error}/>
            }
            <Card key={index} cardType={CardType.Big} children={children}/>
        </div>
    );
}