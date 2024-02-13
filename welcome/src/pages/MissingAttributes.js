import "./MissingAttributes.scss";
import I18n from "../locale/I18n";
import {Toaster, ToasterType} from "@surfnet/sds";
import DOMPurify from "dompurify";
import React from "react";
import {Page} from "../components/Page";
import {stopEvent} from "../utils/Utils";
import {login} from "../utils/Login";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";

export const MissingAttributes = () => {

    const {config} = useAppStore((state) => state);

    const doLogin = e => {
        stopEvent(e);
        logout().then(() => {
            login(config, true);
        });
    }
    return (
        <Page>
            <div className="missing-attributes">
                <h1>{I18n.t("missingAttributes.welcome")}</h1>
                <p className="roles">{I18n.t("missingAttributes.attributes")}</p>
                <ul className={"missing-attributes-list"}>
                    {config.missingAttributes.map(attr =>
                        <li key={attr}>{I18n.t(`missingAttributes.${attr}`) }</li>)}
                </ul>
                <Toaster message={""}
                         large={true}
                         toasterType={ToasterType.Warning}
                         children={
                             <div className="warning">
                                 <p className={"info"}
                                    dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("missingAttributes.info"))}}/>

                                 <span>{I18n.t("missingAttributes.preLogin")}</span>
                                 <a href="/login" onClick={doLogin}>{I18n.t("missingAttributes.login")}</a>
                                 <span>{I18n.t("missingAttributes.postLogin")}</span>
                             </div>
                         }
                />

            </div>
        </Page>)
};
