import React, {useState} from "react";
import "./ClipBoardCopy.scss";
import {CopyIcon as Duplicate} from "@phosphor-icons/react";
import I18n from "../locale/I18n";

export default function ClipBoardCopy({txt}) {
    const [copied, setCopied] = useState(false);

    return (
        <>
            <section
                className="copy-to-clipboard"
                onClick={e => {
                    navigator.clipboard.writeText(txt)
                        .then(() => {
                            const me = e.target;
                            me.classList.add("copied");
                            setCopied(true);
                            setTimeout(() => {
                                me.classList.remove("copied");
                                setCopied(false);
                            }, 1560);
                        })
                }}>
                <Duplicate/>
            </section>
            <div className={`copied ${copied ? "" : "is-hidden"}`}>{I18n.t("forms.copied")}</div>
        </>
    );

}