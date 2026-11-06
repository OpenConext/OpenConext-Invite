import React from "react";
import "./RoleCard.scss";
import Logo from "./Logo";
import I18n from "../locale/I18n";
import {Badge, Button} from "@surfnet/curve-react";
import {isEmpty, sanitizeURL} from "../utils/Utils";

export const RoleCard = ({application, isNew = false}) => {

    const launch = e => {
        const href = sanitizeURL(application.landingPage);
        if (e.metaKey || e.ctrlKey) {
            window.open(href, '_blank');
        } else {
            window.location.href = href;
        }
    }

    return (
        <div className="application-card">
            <Logo src={application.logo} alt={application.name} className="provider"/>
            <div className="application-info">
                <div className="application-title">
                    <h3>{application.name}</h3>
                    {isNew && <Badge variant="info">{I18n.t("proceed.new")}</Badge>}
                </div>
                <p className="organisation">{application.organisation}</p>
                <p className="description">{application.description}</p>
            </div>
            {!isEmpty(application.landingPage) &&
                <Button variant="outline" onClick={launch}>{I18n.t("proceed.launch")}</Button>}
        </div>
    );
}
