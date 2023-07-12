import React from "react";
import {ReactComponent as ArrowRight} from "@surfnet/sds/icons/functional-icons/arrow-right-2.svg";

import {Tooltip} from "@surfnet/sds";
import "./InputField.scss";
import {isEmpty} from "../utils/Utils";
import ClipBoardCopy from "./ClipBoardCopy";
import {validUrlRegExp} from "../validations/regExps";
import {useNavigate} from "react-router-dom";

export default function InputField({
                                       onChange,
                                       name,
                                       value,
                                       placeholder = "",
                                       disabled = false,
                                       toolTip = null,
                                       onBlur = () => true,
                                       onEnter = null,
                                       multiline = false,
                                       copyClipBoard = false,
                                       link = null,
                                       externalLink = false,
                                       large = false,
                                       noInput = false,
                                       error = false,
                                       cols = 5,
                                       maxLength = 255,
                                       onRef = null,
                                       displayLabel = true,
                                       button = null,
                                       isInteger = false,
                                       isUrl = false
                                   }) {
    const navigate = useNavigate();
    placeholder = disabled ? "" : placeholder;
    let className = "sds--text-field--input";
    if (error) {
        className += "error ";
    }
    const validExternalLink = externalLink && !isEmpty(value) && validUrlRegExp.test(value);
    return (
        <div className={`input-field sds--text-field ${error ? "sds--text-field--status-error" : ""}`}>
            {(name && displayLabel) && <label htmlFor={name}>{name}
                {toolTip && <Tooltip tip={toolTip}/>}
            </label>}
            <div className="inner-input-field">
                {(!multiline && !noInput) &&
                    <input type={isInteger ? "number" : isUrl ? "url" : "text"}
                           disabled={disabled}
                           value={value || ""}
                           onChange={onChange}
                           onBlur={onBlur}
                           id={name}
                           maxLength={maxLength}
                           min={0}
                           ref={onRef}
                           placeholder={placeholder}
                           className={`${className} sds--text-field--input`}
                           onKeyDown={e => {
                               if (onEnter && e.keyCode === 13) {//enter
                                   onEnter(e);
                               }
                           }}/>}
                {(multiline && !noInput) &&
                    <textarea disabled={disabled}
                              value={value}
                              onChange={onChange}
                              onBlur={onBlur}
                              id={name}
                              className={`${className} sds--text-area ${large ? "large" : ""}`}
                              onKeyDown={e => {
                                  if (onEnter && e.keyCode === 13) {//enter
                                      onEnter(e);
                                  }
                              }}
                              placeholder={placeholder} cols={cols}/>}
                {button && button}
                {copyClipBoard && <ClipBoardCopy txt={value} right={true} input={true}/>}
                {link && <div className="input-field-link" onClick={() => navigate(link)}>
                    <ArrowRight/>
                </div>}
                {validExternalLink &&
                    <div className={`input-field-link`}>
                        <a href={value} rel="noopener noreferrer" target="_blank">
                            <ArrowRight/>
                        </a>
                    </div>}
                {noInput && <span className="no-input">{value}</span>}
            </div>
        </div>
    );
}
