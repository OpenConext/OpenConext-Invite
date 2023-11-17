import {useNavigate} from "react-router-dom";
import {Button, ButtonType, Tooltip} from "@surfnet/sds";
import I18n from "../locale/I18n";
import React from "react";
import {highestAuthority} from "../utils/UserRole";

import "./Impersonating.scss";
import {ReactComponent as ImpersonateIcon} from "@surfnet/sds/icons/illustrative-icons/presentation-amphitheater.svg";
import DOMPurify from "dompurify";
import {useAppStore} from "../stores/AppStore";

export const Impersonating = () => {

    const {user: currentUser, setFlash, impersonator, stopImpersonation} = useAppStore(state => state);
    const navigate = useNavigate();
    const authority = highestAuthority(currentUser);
    const userRole = I18n.t(`access.${authority}`);
    return <div className="impersonator ">
        <Tooltip children={<ImpersonateIcon/>}
                 standalone={true}
                 tip={I18n.t("impersonate.impersonatorTooltip", {
                     currentUser: currentUser.name,
                     impersonator: impersonator.name
                 })}/>

        <p dangerouslySetInnerHTML={{
            __html: DOMPurify.sanitize(I18n.t("impersonate.impersonator", {
                name: currentUser.name,
                role: userRole
            }))
        }}/>
        <Button type={ButtonType.Secondary}
                onClick={() => {
                    stopImpersonation();
                    setFlash(I18n.t("impersonate.flash.clearedImpersonation"));
                    navigate("/");
                }}
                txt={I18n.t("impersonate.exit")}/>
    </div>
}
