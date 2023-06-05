import React, {useRef} from "react";

import {ReactComponent as CardIcon} from "@surfnet/sds/icons/functional-icons/card-view.svg";
import {Tooltip} from "@surfnet/sds";
import DatePicker from "react-datepicker";

import "react-datepicker/dist/react-datepicker.css";
import "./DateField.scss"
import {stopEvent} from "../utils/Utils";
import {DateTime} from "luxon";

export const DateField = ({
                              onChange,
                              name,
                              value,
                              disabled = false,
                              maxDate = null,
                              minDate = null,
                              toolTip = null,
                              allowNull = false,
                              showYearDropdown = false,
                              pastDatesAllowed = false
                          }) => {
    const inputRef = useRef(null);

    const toggle = () => inputRef.current.setOpen(true);

    const invalidValue = (onChange) => {
        setTimeout(() => onChange(DateTime.now().plus({days: 16}).toJSDate()), 250);
    }

    const validateOnBlur = e => {
        if (e && e.target) {
            const minimalDate = minDate || DateTime.now().plus({days: 1}).toJSDate();
            minimalDate.setHours(0, 0, 0, 0);
            const value = e.target.value;
            if (value) {
                const m = DateTime.fromFormat(value, "DD/MM/YYYY");
                const d = m.toJSDate();
                if (!m.isValid || (!pastDatesAllowed && d < minimalDate) || (maxDate && d > maxDate)) {
                    invalidValue(onChange);
                }
            } else if (!allowNull) {
                invalidValue(onChange);
            }
        }
    }

    const minimalDate = minDate || DateTime.now().plus({days: 1}).toJSDate();
    const selectedDate = value || (allowNull ? null : DateTime.now().plus({days: 16}).toJSDate());
    return (
        <div className="date-field">
            {name && <label className="date-field-label" htmlFor={name}>{name}
                {toolTip && <Tooltip tip={toolTip}/>}
            </label>}
            <label className={"date-picker-container"} htmlFor={name}>
                <DatePicker
                    ref={inputRef}
                    name={name}
                    id={name}
                    selected={selectedDate}
                    preventOpenOnFocus
                    dateFormat={"dd/MM/yyyy"}
                    onChange={onChange}
                    showWeekNumbers
                    isClearable={allowNull}
                    showYearDropdown={showYearDropdown}
                    onBlur={validateOnBlur}
                    weekLabel="Week"
                    disabled={disabled}
                    todayButton={null}
                    maxDate={maxDate}
                    minDate={pastDatesAllowed ? null : minimalDate}
                />
                <div className={"calendar-icon"} onClick={toggle}><CardIcon/></div>
            </label>
        </div>
    );
}
