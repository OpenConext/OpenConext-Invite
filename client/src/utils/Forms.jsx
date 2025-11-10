import {ReactComponent as ArrowDown} from "@surfnet/sds/icons/functional-icons/arrow-down-2.svg";
import {ReactComponent as ArrowUp} from "@surfnet/sds/icons/functional-icons/arrow-up-2.svg";
import React from "react";

export function headerIcon(column, sorted, reverse) {
    if (column.nonSortable) {
        return null;
    }
    if (column.key === sorted) {
        return reverse ? <ArrowDown/> : <ArrowUp/>
    }
    return null;
}
