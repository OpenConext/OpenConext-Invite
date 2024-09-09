import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Chip, ChipType} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";

export const RoleCard = ({
                             application,
                             index,
                             isNew = false
                         }) => {
    const navigate = useNavigate();

    const children =
        <div key={index} className="user-role" >
            <Logo src={application.logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{application.roleName} {application.authority &&
                    <span>- {I18n.t(`access.${application.authority}`)}</span>}</p>
                <h3>{application.applicationName}</h3>
                <MoreLessText txt={application.roleDescription} cutOffNumber={80}/>
            </section>
            <div className="launch">
                <Button txt={I18n.t("inviter.details")} onClick={() => navigate(`/roles/${application.roleId}`)}/>
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