import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Chip, ChipType} from "@surfnet/sds";
import {isEmpty, sanitizeURL} from "../utils/Utils";

export const RoleCard = ({index, application, isNew = false, skipLaunch = false}) => {

    const children =
        <div key={index} className="user-role">
            <Logo src={application.logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{application.roleName}</p>
                <h3>{application.applicationName}</h3>
                <MoreLessText txt={application.roleDescription} cutOffNumber={120}/>
            </section>
            {(!skipLaunch && !isEmpty(application.landingPage)) &&
                <div className={"launch"}>
                    <Button txt={I18n.t("proceed.launch")} onClick={() => {
                        window.location.href = sanitizeURL(application.landingPage);
                    }}/>
                </div>
            }
        </div>;
    return (
        <div className={`card-container  ${isNew ? "is-new" : ""}`}>
            {isNew &&
                <Chip label={I18n.t("proceed.new")} type={ChipType.Status_error}/>
            }
            <Card key={index} cardType={CardType.Big} children={children}/>
        </div>
    );
}