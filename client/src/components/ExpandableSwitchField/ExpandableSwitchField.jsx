import React, {useState} from "react";
import SwitchField from "../SwitchField";

export const ExpandableSwitchField = ({
                                          name,
                                          label,
                                          info,
                                          defaultValue = false,
                                          onChange,
                                          disabled = false,
                                          children
                                      }) => {
    const [expanded, setExpanded] = useState(defaultValue);

    const toggle = () => {
        const next = !expanded;
        setExpanded(next);
        if (onChange) {
            onChange(next);
        }
    };

    return (
        <>
            <SwitchField name={name}
                         value={expanded}
                         onChange={toggle}
                         label={label}
                         info={info}
                         last={expanded}
                         disabled={disabled}/>
            {expanded && children}
        </>
    );
};
