import React from "react";
import "./Page.scss";

export const Page = ({children}) => {

    return (
        <div className="page">
            {children}
        </div>
    );
}