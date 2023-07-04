import "./NotFound.scss";
import React from "react";
import NotFoundLogo from "../icons/undraw_page_not_found_re_e9o6.svg";

const NotFound = () => (
    <div className={"not-found"}>
        <img src={NotFoundLogo} alt="logo"/>
    </div>
);
export default NotFound;