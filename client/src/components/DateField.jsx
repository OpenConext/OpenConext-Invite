import React, {useRef} from "react";

import {Tooltip, TooltipContent, TooltipTrigger} from "@surfnet/curve-react";
import {InfoIcon, CalendarBlankIcon as CardIcon} from "@phosphor-icons/react";
import {sanitize} from "../utils/Utils";
import DatePicker from "react-datepicker";

import "react-datepicker/dist/react-datepicker.css";
import "./DateField.scss"
import {DateTime} from "luxon";
import {CaretLeftIcon, CaretRightIcon} from "@phosphor-icons/react";
import {futureDate} from "../utils/Date";

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
                const m = DateTime.fromFormat(value, "dd/MM/yyyy");
                const d = m.toJSDate();
                if (!m.isValid || (!pastDatesAllowed && d < minimalDate) || (maxDate && d > maxDate)) {
                    invalidValue(onChange);
                }
            } else if (!allowNull) {
                invalidValue(onChange);
            }
        }
    }

    // Month name and year selector in one header, so the year is not shown twice
    const renderHeader = ({date, changeYear, decreaseMonth, increaseMonth, prevMonthButtonDisabled, nextMonthButtonDisabled}) => {
        const currentYear = new Date().getFullYear();
        const minYear = minDate ? minDate.getFullYear() : currentYear - 10;
        const maxYear = maxDate ? maxDate.getFullYear() : currentYear + 10;
        const years = [];
        for (let y = maxYear; y >= Math.min(minYear, date.getFullYear()); y--) {
            years.push(y);
        }
        return (
            <div className="date-field-header">
                <button type="button" onClick={decreaseMonth} disabled={prevMonthButtonDisabled}
                        aria-label="Previous month"><CaretLeftIcon/></button>
                <span className="month-year">
                    <span className="month">{date.toLocaleDateString("en-GB", {month: "long"})}</span>
                    <select value={date.getFullYear()} onChange={e => changeYear(Number(e.target.value))}>
                        {years.map(y => <option key={y} value={y}>{y}</option>)}
                    </select>
                </span>
                <button type="button" onClick={increaseMonth} disabled={nextMonthButtonDisabled}
                        aria-label="Next month"><CaretRightIcon/></button>
            </div>
        );
    };

    const minimalDate = minDate || futureDate(1);
    const selectedDate = value || (allowNull ? null : futureDate(16));
    return (
        <div className="date-field">
            {name && <label className="date-field-label" htmlFor={name}>{name}
                {toolTip && <Tooltip>
                    <TooltipTrigger render={<InfoIcon/>}/>
                    <TooltipContent><span dangerouslySetInnerHTML={{__html: sanitize(toolTip)}}/></TooltipContent>
                </Tooltip>}
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
                    renderCustomHeader={showYearDropdown ? renderHeader : undefined}
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
