import React from "react";

import "./SelectField.scss";
import Select from "react-select";
import CreatableSelect from "react-select/creatable";
import {Tooltip, TooltipContent, TooltipTrigger} from "@surfnet/curve-react";
import {CheckIcon, InfoIcon, WarningIcon as AlertIcon} from "@phosphor-icons/react";
import I18n from "../locale/I18n.js";
import DOMPurify from "dompurify";

export default function SelectField({
                                        onChange, name, value, options, placeholder = "", disabled = false,
                                        toolTip = null, searchable = false, small = false,
                                        clearable = false, isMulti = false, creatable = false,
                                        onInputChange = null, required = false, info = null,
                                        className = "", isAlert = false, optional = false,
                                        infoUnderLabel = false, showCheck = false
                                    }) {
    // Options may carry a `description`, which is shown below the label in the menu only.
    // With `showCheck` the selected option is marked with a check in the menu.
    const formatOptionLabel = (option, {context, selectValue}) => {
        if (context !== "menu" || !(option.description || showCheck)) {
            return option.label;
        }
        const selected = showCheck && selectValue.some(selectedOption => selectedOption.value === option.value);
        return (
            <div className="option-with-description">
                <span className="option-text">
                    <span className="option-label">{option.label}</span>
                    {option.description && <span className="option-description">{option.description}</span>}
                </span>
                {selected && <CheckIcon/>}
            </div>
        );
    }

    const infoElement = info && <p className={`select-info ${infoUnderLabel ? "under-label" : ""}`}
                                   dangerouslySetInnerHTML={{
                                       __html: DOMPurify.sanitize(info
                                           , {ADD_ATTR: ["target"], ADD_TAGS: ["a", "rel"]})
                                   }}/>;
    return (
        <div className={`select-field ${className}`}>
            {name && <label htmlFor={name}>{name}{required && <sup className="required">*</sup>}
                {optional && <span className="optional">{I18n.t("forms.optional")}</span>}
                {toolTip && <Tooltip>
                    <TooltipTrigger render={<InfoIcon/>}/>
                    <TooltipContent><span dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(toolTip)}}/></TooltipContent>
                </Tooltip>}
                {isAlert && <Tooltip>
                    <TooltipTrigger render={<AlertIcon weight="fill" className="alert-triangle"/>}/>
                    <TooltipContent><span dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("forms.changeRequest"))}}/></TooltipContent>
                </Tooltip>}
            </label>}
            {infoUnderLabel && infoElement}
            {creatable &&
                <CreatableSelect
                    className={`input-select-inner creatable`}
                    classNamePrefix={"select-inner"}
                    value={value}
                    isMulti={true}
                    placeholder={placeholder}
                    isSearchable={true}
                    onInputChange={onInputChange}
                    isClearable={clearable}
                    isDisabled={disabled}
                    onChange={onChange}
                    options={options}
                    required={required}
                />}
            {!creatable && <Select
                className={`input-select-inner ${small ? " small" : ""}`}
                classNamePrefix={"select-inner"}
                value={value}
                placeholder={placeholder}
                isDisabled={disabled}
                onChange={onChange}
                isMulti={isMulti}
                options={options}
                isSearchable={searchable}
                isClearable={clearable}
                required={required}
                formatOptionLabel={formatOptionLabel}
            />}
            {!infoUnderLabel && infoElement}
        </div>
    );
}
