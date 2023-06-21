import "./InviteOnly.scss";
import I18n from "../locale/I18n";
import {Toaster, ToasterType} from "@surfnet/sds";
import DOMPurify from "dompurify";
import React from "react";
import {Page} from "../components/Page";
import {stopEvent} from "../utils/Utils";
import {login} from "../utils/Login";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";

export const InviteOnly = () => {

    const config = useAppStore((state) => state.config);

    const doLogin = e => {
        stopEvent(e);
        logout().then(() => {
            login(config, true);
        });
    }
    return (
        <Page>
            <div className="invite-only">
                <h1>{I18n.t("inviteOnly.welcome")}</h1>
                <p className="roles">{I18n.t("inviteOnly.roles")}</p>
                <Toaster message={""}
                         large={true}
                         toasterType={ToasterType.Warning}
                         children={
                             <div className="warning">
                                 <p className={"info"}
                                    dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("inviteOnly.info"))}}/>

                                 <span>{I18n.t("inviteOnly.preLogin")}</span>
                                 <a href="/login" onClick={doLogin}>{I18n.t("inviteOnly.login")}</a>
                                 <span>{I18n.t("inviteOnly.postLogin")}</span>
                             </div>
                         }
                />

            </div>
        </Page>)
};
