import React from "react";
import "./SwitchField.scss";
import {Switch, Tooltip, TooltipContent, TooltipTrigger} from "@surfnet/curve-react";
import {WarningIcon as AlertIcon} from "@phosphor-icons/react";
import I18n from "../locale/I18n";
import {sanitize} from "../utils/Utils";

export default function SwitchField({name,
                                        value,
                                        onChange,
                                        label,
                                        info,
                                        last = false,
                                        isAlert=false,
                                        className = ""}) {
    return (
        <div className={`switch-field ${last ? "last" : ""} ${className}`}>
            <div className={"inner-switch"}>
                <span className="switch-label">{label}{isAlert &&
                    <Tooltip>
                        <TooltipTrigger render={<AlertIcon weight="fill" className="alert-triangle"/>}/>
                        <TooltipContent><span dangerouslySetInnerHTML={{__html: sanitize(I18n.t("forms.changeRequest"))}}/></TooltipContent>
                    </Tooltip>
                    }</span>
                <span className="switch-info">{info}</span>
            </div>
            <Switch name={name}
                    checked={value}
                    onCheckedChange={onChange}/>
        </div>

    )
}
