import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";
import {Button, Card, CardType, Chip, ChipType} from "@surfnet/sds";
import {isEmpty, sanitizeURL, splitListSemantically} from "../utils/Utils";
import {ReactComponent as MultipleIcon} from "../icons/multi-role.svg";
import {roleName} from "../utils/Manage";

export const RoleCard = ({role, index, isNew = false, skipLaunch= false}) => {

    const applications = role.applicationMaps;
    const multiApp = applications.length === 1;
    const application = applications[0];
    const logo = multiApp ? application.logo : <MultipleIcon/>
    const name = multiApp ? splitListSemantically(applications.map(app => roleName(app)), I18n.t("forms.and")) :
        roleName(application);

    const children =
        <div key={index} className="user-role">
            <Logo src={logo} alt={"provider"} className={"provider"}/>
            <section className={"user-role-info"}>
                <p>{name}</p>
                <h3>{role.name}</h3>
                <MoreLessText txt={role.description} cutOffNumber={120}/>
            </section>
            {(!skipLaunch && !isEmpty(role.landingPage)) && <div className={"launch"}>
                <Button txt={I18n.t("proceed.launch")} onClick={() => {
                    window.location.href = sanitizeURL(role.landingPage);
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