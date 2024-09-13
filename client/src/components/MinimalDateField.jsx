import React, {useRef} from "react";

import DatePicker from "react-datepicker";
import {ReactComponent as ResetIcon} from "@surfnet/sds/icons/functional-icons/close.svg";
import {ReactComponent as EditIcon} from "@surfnet/sds/icons/functional-icons/edit.svg";

import "react-datepicker/dist/react-datepicker.css";
import "./MinimalDateField.scss"
import {dateFromEpoch, futureDate} from "../utils/Date";
import {isEmpty} from "../utils/Utils";
import I18n from "../locale/I18n";
import {Chip, ChipType} from "@surfnet/sds";

export const MinimalDateField = ({
                                     onChange,
                                     name,
                                     value,
                                     disabled = false,
                                     maxDate = null,
                                     minDate = null,
                                     allowNull = false,
                                     showYearDropdown = false,
                                     pastDatesAllowed = false
                                 }) => {
    const inputRef = useRef(null);

    const toggle = () => inputRef.current.setOpen(true);
    const minimalDate = minDate || futureDate(1);
    const selectedDate = value || (allowNull ? null : futureDate(16));
    let expired = false;
    if (!isEmpty(value)) {
        const now = new Date();
        const endDate = new Date(value * 1000);
        expired = now > endDate;
    }
    return (
        <div className="minimal-date-field">
            {expired && <Chip
                type={ChipType.Status_error}
                label={I18n.t("invitations.statuses.expired")}/>}
            {!expired && <span className="value">
                {!isEmpty(value) ? dateFromEpoch(value) : I18n.t("roles.noEndDate")}
            </span>}
            <DatePicker
                ref={inputRef}
                name={name}
                id={name}
                customInput={<div className={"dummy"}/>}
                selected={!isEmpty(selectedDate) ? new Date(selectedDate * 1000) : null}
                preventOpenOnFocus={true}
                dateFormat={"dd/MM/yyyy"}
                onChange={onChange}
                showWeekNumbers
                isClearable={false}
                showYearDropdown={showYearDropdown}
                weekLabel="Week"
                disabled={disabled}
                todayButton={null}
                maxDate={maxDate}
                minDate={pastDatesAllowed ? null : minimalDate}
            />
            {!isEmpty(value) && <div className="icon reset-icon left" onClick={() => onChange(null)}><ResetIcon/></div>}
            <div className={`icon edit-icon ${isEmpty(value) ? "left" : ""}`} onClick={toggle}><EditIcon/></div>
        </div>
    );
}