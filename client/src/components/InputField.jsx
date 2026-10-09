import React from "react";
import {CaretRightIcon as ArrowRight} from "@phosphor-icons/react";

import {Field, FieldDescription, FieldLabel, Input, Textarea, Tooltip, TooltipContent, TooltipTrigger} from "@surfnet/curve-react";
import {InfoIcon, WarningIcon as AlertIcon} from "@phosphor-icons/react";
import "./InputField.scss";
import {isEmpty, sanitize} from "../utils/Utils";
import ClipBoardCopy from "./ClipBoardCopy";
import {validUrlRegExp} from "../validations/regExps";
import {Link} from "react-router";
import I18n from "../locale/I18n.js";

export default function InputField({
                                       onChange,
                                       name,
                                       value,
                                       placeholder = "",
                                       disabled = false,
                                       toolTip = null,
                                       onBlur = () => true,
                                       onEnter = null,
                                       onEscape = null,
                                       multiline = false,
                                       copyClipBoard = false,
                                       link = null,
                                       externalLink = false,
                                       large = false,
                                       small = false,
                                       noInput = false,
                                       error = false,
                                       cols = 5,
                                       maxLength = 255,
                                       onRef = null,
                                       displayLabel = true,
                                       button = null,
                                       isInteger = false,
                                       isUrl = false,
                                       customClassName = "",
                                       required = false,
                                       info = null,
                                        isAlert = false,
                                       optional = false
                                   }) {
    placeholder = disabled ? "" : placeholder;
    const validExternalLink = externalLink && !isEmpty(value) && validUrlRegExp.test(value);

    const onKeyDown = e => {
        if (onEnter && e.key === "Enter") {//enter
            onEnter(e);
        } else if (onEscape && e.key === "Escape") {//escape
            onEscape(e);
        }
    };

    return (
        <Field className={`input-field ${customClassName}`} data-invalid={error}>
            {(name && displayLabel) && <FieldLabel htmlFor={name}>{name}{required && <sup className="required">*</sup>}
                {optional && <span className="optional">{I18n.t("forms.optional")}</span>}
                {isAlert && <Tooltip>
                    <TooltipTrigger render={<AlertIcon weight="fill" className="alert-triangle"/>}/>
                    <TooltipContent><span dangerouslySetInnerHTML={{__html: sanitize(I18n.t("forms.changeRequest"))}}/></TooltipContent>
                </Tooltip>}
                {toolTip && <Tooltip>
                    <TooltipTrigger render={<InfoIcon/>}/>
                    <TooltipContent><span dangerouslySetInnerHTML={{__html: sanitize(toolTip)}}/></TooltipContent>
                </Tooltip>}
            </FieldLabel>}
            <div className="inner-input-field">
                {(!multiline && !noInput) &&
                    <Input type={isInteger ? "number" : isUrl ? "url" : "text"}
                           disabled={disabled}
                           value={value || ""}
                           onChange={onChange}
                           onBlur={onBlur}
                           id={name}
                           maxLength={maxLength}
                           max={isInteger ? maxLength : null}
                           min={0}
                           ref={onRef}
                           placeholder={placeholder}
                           required={required}
                           aria-invalid={error}
                           onKeyDown={onKeyDown}/>}
                {(multiline && !noInput) &&
                    <Textarea disabled={disabled}
                              value={value || ""}
                              onChange={onChange}
                              onBlur={onBlur}
                              id={name}
                              required={required}
                              aria-invalid={error}
                              className={`${large ? "large" : ""} ${small ? "small" : ""}`}
                              onKeyDown={e => {
                                  if (onEnter && e.keyCode === 13) {//enter
                                      onEnter(e);
                                  }
                              }}
                              placeholder={placeholder}
                              cols={cols}/>}
                {button && button}
                {copyClipBoard && <ClipBoardCopy txt={value}/>}
                {link && <Link to={link} className="input-field-link">
                    <ArrowRight/>
                </Link>}
                {validExternalLink &&
                    <div className={`input-field-link`}>
                        <a href={value} rel="noopener noreferrer" target="_blank">
                            <ArrowRight/>
                        </a>
                    </div>}
                {noInput && <span className="no-input">{value}</span>}
            </div>
            {info && <FieldDescription>{info}</FieldDescription>}
        </Field>
    );
}
