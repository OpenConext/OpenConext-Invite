import React from "react";
import I18n from "../locale/I18n";
import InfoIcon from "@surfnet/sds/icons/functional-icons/info.svg";
import "./AuthorityExplanation.scss";

const AUTHORITIES_EXPLAINED = ["guest", "inviter", "manager", "roleCreator"];

export const AuthorityExplanation = () => {
    return (
        <div className="authority-explanation">
            <InfoIcon/>
            <div className="content">
                <p className="title">{I18n.t("invitations.authorityExplanation.title")}</p>
                {AUTHORITIES_EXPLAINED.map(key => (
                    <div className="authority" key={key}>
                        <p className="name">{I18n.t(`invitations.authorityExplanation.${key}.name`)}</p>
                        <p>{I18n.t(`invitations.authorityExplanation.${key}.description`)}</p>
                    </div>
                ))}
            </div>
        </div>
    );
}
