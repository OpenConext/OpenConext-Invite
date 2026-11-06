import React from "react";
import {Tooltip, TooltipContent, TooltipTrigger} from "@surfnet/curve-react";
import {InfoIcon} from "@phosphor-icons/react";
import {sanitize} from "../utils/Utils";

/**
 * curve-react Tooltip with the sanitized (HTML) tip. Without children the info icon is the trigger,
 * otherwise the children are wrapped in the trigger.
 */
export const InfoTooltip = ({tip, children = null, className = ""}) => (
    <Tooltip>
        <TooltipTrigger render={children ? <span className={className}>{children}</span> :
            <InfoIcon className={`info-tooltip ${className}`}/>}/>
        <TooltipContent><span dangerouslySetInnerHTML={{__html: sanitize(tip)}}/></TooltipContent>
    </Tooltip>
);
