import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Cron.scss";
import {Chip, ChipType, Tooltip} from "@surfnet/sds";
import {Entities} from "../components/Entities";
import {ReactComponent as UserIcon} from "@surfnet/sds/icons/functional-icons/id-2.svg";
import "./Users.scss";
import {useAppStore} from "../stores/AppStore";
import {dateFromEpoch, futureDate} from "../utils/Date";
import {useNavigate} from "react-router-dom";
import {chipTypeForUserRole} from "../utils/Authority";
import {allowedToRenewUserRole} from "../utils/UserRole";
import {DateField} from "../components/DateField";
import ConfirmationDialog from "../components/ConfirmationDialog";
import {updateUserRoleEndData} from "../api";


export const Cron = () => {
    const [results, setResults] = useState({});

    return (
        <div className="mod-cron-container">
            <div className="mod-cron">TODO</div>

    </div>)

}
