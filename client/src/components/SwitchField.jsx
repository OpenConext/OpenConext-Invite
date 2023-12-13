import React from "react";
import "./SwitchField.scss";
import {Switch} from "@surfnet/sds";

export default function SwitchField({name, value, onChange, label, info}) {
    return (
        <div className="switch-field">
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