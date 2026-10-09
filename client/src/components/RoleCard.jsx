import {Badge, Button, Card, CardContent} from "@surfnet/curve-react";
import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {useNavigate} from "react-router";

export const RoleCard = ({
                             application,
                             index,
                             isNew = false
                         }) => {
    const navigate = useNavigate();

    const children =
        <div key={index} className="user-role">
            <Logo src={application.logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{application.roleName} {application.authority &&
                    <span>- {I18n.t(`access.${application.authority}`)}</span>}</p>
                <h3>{application.applicationName}</h3>
                <MoreLessText txt={application.roleDescription} cutOffNumber={80}/>
            </section>
            <div className="launch">
                <Button onClick={() => navigate(`/roles/${application.roleId}`)}>{I18n.t("inviter.details")}</Button>
            </div>

        </div>;

    const className = `card-container ${isNew ? "is-new" : ""}`;
    return (
        <div className={className}>
            {isNew &&
                <Badge variant="danger">{I18n.t("proceed.new")}</Badge>
            }
            <Card key={index}><CardContent>{children}</CardContent></Card>
        </div>
    );
}