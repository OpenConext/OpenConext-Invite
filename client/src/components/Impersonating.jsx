import {useNavigate} from "react-router";
import {Button} from "@surfnet/curve-react";
import {InfoTooltip} from "./InfoTooltip";
import {sanitize} from "../utils/Utils";
import I18n from "../locale/I18n";
import React from "react";
import {highestAuthority} from "../utils/UserRole";

import "./Impersonating.scss";
import {ChalkboardTeacherIcon as ImpersonateIcon} from "@phosphor-icons/react";
import DOMPurify from "dompurify";
import {useAppStore} from "../stores/AppStore";

export const Impersonating = () => {

    const {user: currentUser, setFlash, impersonator, stopImpersonation} = useAppStore(state => state);
    const navigate = useNavigate();
    const authority = highestAuthority(currentUser);
    const userRole = I18n.t(`access.${authority}`);
    return <div className="impersonator ">
        <InfoTooltip tip={I18n.t("impersonate.impersonatorTooltip", {
            currentUser: currentUser.name,
            impersonator: impersonator.name
        })}><ImpersonateIcon/></InfoTooltip>

        <p dangerouslySetInnerHTML={{
            __html: DOMPurify.sanitize(I18n.t("impersonate.impersonator", {
                name: currentUser.name,
                role: userRole
            }))
        }}/>
        <Button variant="secondary"
                onClick={() => {
                    stopImpersonation();
                    setFlash(I18n.t("impersonate.flash.clearedImpersonation"));
                    navigate("/");
                }}>
            <span dangerouslySetInnerHTML={{__html: sanitize(I18n.t("impersonate.exit"))}}/>
        </Button>
    </div>
}
