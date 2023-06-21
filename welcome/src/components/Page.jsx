import React from "react";
import "./Page.scss";

export const Page = ({Icon, label, name, children}) => {

    return (
        <div className="page">
            {children}
        </div>
    );
}