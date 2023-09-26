import React from "react";
import "./ClipBoardCopy.scss";
import {ReactComponent as Duplicate} from "@surfnet/sds//icons/functional-icons/duplicate.svg";
import {CopyToClipboard} from "react-copy-to-clipboard";

export default function ClipBoardCopy({txt,  right = false, transparentBackground = false, input = false}) {
    return (
        <CopyToClipboard text={txt}>
            <section
                className={`copy-to-clipboard ${right ? "right" : ""} ${transparentBackground ? "transparent" : ""} ${input ? "input" : ""}`}
                onClick={e => {
                    const me = e.target;
                    me.classList.add("copied");
                    setTimeout(() => me.classList.remove("copied"), 1250);
                }}>
                <Duplicate/>
            </section>
        </CopyToClipboard>
    );

}