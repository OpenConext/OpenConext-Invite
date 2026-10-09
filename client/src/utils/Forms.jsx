import {CaretDownIcon as ArrowDown, CaretUpDownIcon, CaretUpIcon as ArrowUp} from "@phosphor-icons/react";
import React from "react";

export function headerIcon(column, sorted, reverse) {
    if (column.nonSortable) {
        return null;
    }
    if (column.key === sorted) {
        return reverse ? <ArrowDown/> : <ArrowUp/>
    }
    return <CaretUpDownIcon/>;
}
