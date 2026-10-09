import React from "react";

import Tab from "./Tab";
import "./Tabs.scss";

const Tabs = ({children, className, activeTab, tabChanged}) => {

    const filteredChildren = children.filter(child => child);
    if (!children.some((child => child.props && child.props.name === activeTab))) {
        activeTab = (children[0] || {props: {name: activeTab}}).props.name
    }

    return (
        <>
            {filteredChildren.length > 1 && <div className="tabs-menu">
                {filteredChildren.map(child => {
                    const {label, name, notifier, readOnly} = child.props;
                    return (
                        <Tab activeTab={activeTab}
                             readOnly={readOnly}
                             key={name}
                             name={name}
                             notifier={notifier}
                             label={label}
                             onClick={tab => tabChanged(tab)}
                             className={className}/>
                    );
                })}
            </div>}
            {filteredChildren.map(child => {
                if (child.props.name !== activeTab) {
                    return undefined;
                }
                return child.props.children;
            })}

        </>
    );
}

export default Tabs;
