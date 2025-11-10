import React from "react";
import "./WarningIndicator.scss";
import WarningIco from "../icons/warning.svg";
import DOMPurify from "dompurify";

export default function WarningIndicator({msg, standalone = false, decode = true, adjustMargin = false}) {
    const className = `warning-indication ${standalone ? "standalone" : ""} ${adjustMargin ? "adjust-margin" : ""}`;
    msg = msg.replaceAll("?", "");
    return decode ? <span className={className}><WarningIco/>{msg}</span> :
        <span className={className}>
            <WarningIco/>
            <span className={"warning-message"}
                  dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(msg, {ADD_ATTR: ['target']})}}/>
        </span>
}