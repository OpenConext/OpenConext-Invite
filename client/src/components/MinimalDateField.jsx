import React, {useRef} from "react";

import DatePicker from "react-datepicker";
import ResetIcon from "@surfnet/sds/icons/functional-icons/close.svg";
import EditIcon from "@surfnet/sds/icons/functional-icons/edit.svg";
import "react-datepicker/dist/react-datepicker.css";
import "./MinimalDateField.scss"
import {futureDate, shortDateFromEpoch} from "../utils/Date";
import {isEmpty} from "../utils/Utils";
import I18n from "../locale/I18n";
import {Chip, ChipType, Tooltip} from "@surfnet/sds";

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
    const selectedDate = isEmpty(value) ? futureDate(16) : new Date(value);

    let expired = false;
    if (!isEmpty(value)) {
        const now = new Date();
        const endDate = new Date(value);
        expired = now > endDate;
    }
    return (
        <div className="minimal-date-field">
            {expired && <Chip
                type={ChipType.Status_error}
                label={I18n.t("invitations.statuses.expired")}/>}
            {!expired && <span className="value">
                {!isEmpty(value) ? shortDateFromEpoch(value, false) : I18n.t("roles.noEndDate")}
            </span>}
            <DatePicker
                ref={inputRef}
                name={name}
                id={name}
                customInput={<div className={"dummy"}/>}
                selected={selectedDate}
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
            {(!isEmpty(value) && allowNull) &&
                <div className="icon reset-icon left"
                     onClick={() => onChange(null)}>
                    <Tooltip standalone={true}
                             children={<ResetIcon/>}
                             tip={I18n.t("forms.reset")}/>

                </div>}
            <div className={`icon edit-icon ${isEmpty(value) || !allowNull ? "left" : ""}`}
                 onClick={toggle}>
                <Tooltip standalone={true}
                         children={<EditIcon/>}
                         tip={I18n.t("forms.edit")}/>

            </div>
        </div>
    );
}
