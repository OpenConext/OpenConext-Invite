import React from "react";
import DOMPurify from "dompurify";
import I18n from "../locale/I18n";
import AccessDeniedLogo from "../icons/landing/undraw_access_denied_re_awnf.svg";
import AuthenticationLogo from "../icons/landing/undraw_authentication_re_svpt.svg";
import EnterLogo from "../icons/landing/undraw_enter_uhqk.svg";
import SCIMLogo from "../icons/landing/undraw_subscriptions_re_k7jj.svg";
import "./LandingInfo.scss";
import {Chip, ChipType} from "@surfnet/sds"

export const LandingInfo = () => {

    const infoBlock = (info, Logo, index) => {
        const reversed = index % 2 === 0 ? "reversed" : "";
        return (
            <div key={index} className={`mod-login info ${reversed}`}>
                <div className="header-left info">
                    <div className={"info-title"}>
                        <h2>{info[0]}</h2>
                        {info[2] && <div className={"admin-function-container"}>
                            <Chip label={I18n.t("landing.adminFunction")} type={ChipType.Main_400}/>
                        </div>}
                    </div>
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(info[1])}}/>
                </div>
                <div className="header-right info">
                    <img src={Logo} alt="logo" className={`${reversed}`}/>
                </div>
            </div>
        );
    }


    const logos = [AccessDeniedLogo, AuthenticationLogo, EnterLogo, SCIMLogo];
    return (
        <div className="mod-login-container bottom">
            <div className="mod-login bottom">
                <h1>{I18n.t("landing.works")}</h1>
                {I18n.translations[I18n.locale].landing.info.map((info, index) =>
                    infoBlock(info, logos[index], index)
                )}
                <div className={"landing-footer"}>
                    <p dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t(`landing.footer`))}}/>
                </div>
            </div>
        </div>
    );
}