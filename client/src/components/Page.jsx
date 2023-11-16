import React from "react";
import "./Page.scss";

export const Page = ({children, className="page"}) => {

    return (
        <div className={className}>
            {children}
        </div>
    );
}