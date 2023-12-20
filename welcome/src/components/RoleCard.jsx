import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Chip, ChipType} from "@surfnet/sds";
import {isEmpty, sanitizeURL} from "../utils/Utils";
import {roleName} from "../utils/Manage";

export const RoleCard = ({role, index, applicationMap, isNew = false, skipLaunch = false}) => {

    const children =
        <div key={index} className="user-role">
            <Logo src={applicationMap.logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{role.name}</p>
                <h3>{roleName(applicationMap)}</h3>
                <MoreLessText txt={role.description} cutOffNumber={120}/>
            </section>
            {(!skipLaunch && !isEmpty(applicationMap.landingPage)) && <div className={"launch"}>
                <Button txt={I18n.t("proceed.launch")} onClick={() => {
                    window.location.href = sanitizeURL(applicationMap.landingPage);
                }}/>
            </div>}
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