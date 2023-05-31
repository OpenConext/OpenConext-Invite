import React from "react";

import Tab from "./Tab";
import "./Tabs.scss";

export default function Tabs({children, busy, currentTab, className = ""}) {

    const onClickTabItem = tab => {
        const {tabChanged} = this.props;
        tabChanged(tab);
    }

    let activeTab = currentTab || children[0].props.name;
    const filteredChildren = children.filter(child => child);
    if (!filteredChildren.some((child => child.props && child.props.name === activeTab))) {
        activeTab = (filteredChildren[0] || {props: {name: activeTab}}).props.name
    }
    return (
        <>
            <div className="tabs-container">
                {<div className={`tabs ${className}`}>

                    {filteredChildren.map(child => {
                        const {label, name, icon, notifier, readOnly} = child.props;

                        return (
                            <Tab
                                activeTab={activeTab}
                                icon={icon}
                                readOnly={readOnly}
                                key={name}
                                name={name}
                                busy={busy}
                                notifier={notifier}
                                label={label}
                                onClick={onClickTabItem}
                                className={className}
                            />
                        );
                    })}
                </div>}
            </div>
            {filteredChildren.map(child => {
                if (child.props.name !== activeTab) {
                    return undefined;
                }
                return child.props.children;
            })}

        </>
    );
}
