import React from "react";
import "./SwitchField.scss";
import {Switch} from "@surfnet/sds";

export default function SwitchField({name, value, onChange, label, info, last = false}) {
    return (
        <div className={`switch-field ${last ? "last" : ""}`}>
            <div className={"inner-switch"}>
                <span className="switch-label">{label}</span>
                <span className="switch-info">{info}</span>
            </div>
            <Switch name={name}
                    value={value}
                    onChange={onChange}/>
        </div>

    )
}