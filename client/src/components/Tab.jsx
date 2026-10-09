import React from "react";
import {WarningCircleIcon as AlertIcon} from "@phosphor-icons/react";
import "./Tab.scss";
import {Badge} from "@surfnet/curve-react";
import {stopEvent} from "../utils/Utils";

export default function Tab({
                                name,
                                onClick,
                                activeTab,
                                className = "",
                                label,
                                Icon,
                                notifier,
                                readOnly,
                                busy,
                            }) {

    const onClickInner = e => {
        stopEvent(e);
        if (!readOnly) {
            onClick(name);
        }
    };

    let chipCount = null;
    if (label && label.indexOf("(") > -1) {
        const count = label.substring(label.indexOf("(") + 1, label.indexOf(")"));
        label = label.substring(0, label.indexOf("(") - 1);
        chipCount = <Badge variant="secondary" className="tab-count">{count}</Badge>
    }

    const classes = [className, "tab", name, activeTab === name ? "active" : "", readOnly ? "disabled" : "", busy ? "busy" : ""]
        .filter(c => c).join(" ");
    return (
        <a href={`/${name}`} className={classes} onClick={onClickInner}
           aria-current={activeTab === name ? "page" : undefined}>
            {Icon && <Icon/>}{label}{chipCount}
            {notifier && <span className="notifier"><AlertIcon/></span>}
        </a>
    );
}
