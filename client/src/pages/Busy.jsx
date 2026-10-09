import {Spinner} from "@surfnet/curve-react";
import "./NotFound.scss";
import React from "react";

export const Busy = () => {

    return (
        <div className="loading-container"><Spinner className="size-8"/></div>
    );
}
