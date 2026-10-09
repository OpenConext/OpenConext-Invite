import {Button, DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from "@surfnet/curve-react";
import {CaretDownIcon} from "@phosphor-icons/react";
import React from "react";
import "./UnitHeader.scss";

import Logo from "./Logo";
import {isEmpty} from "../utils/Utils";

import {useNavigate} from "react-router";
import I18n from "../locale/I18n";
import {MoreLessText} from "./MoreLessText";

export const UnitHeader = ({
                               obj,
                               history,
                               auditLogPath,
                               name,
                               breadcrumbName,
                               svgClick,
                               firstTime,
                               actions,
                               children,
                               customAction,
                               displayDescription
                           }) => {

    const navigate = useNavigate();

    const queryParam = `name=${encodeURIComponent(breadcrumbName || name)}&back=${encodeURIComponent(window.location.pathname)}`;
    const showChevronAction = (history && auditLogPath) || firstTime;
    return (
        <div className="unit-header-container">
            <div className="unit-header">
                <div className={`image ${obj.style || ""}`}>
                    {obj.logo && <Logo src={obj.logo}/>}
                    {obj.svg && <obj.svg onClick={() => svgClick && svgClick()}/>}
                    {obj.icon && obj.icon}
                </div>
                <div className="obj-name">
                    {obj.name && <h1>{obj.name}</h1>}
                    {(obj.description && displayDescription) &&
                        <MoreLessText txt={obj.description} type={"compact"}/>
                    }
                    {children}
                </div>
                {!isEmpty(actions) &&
                    <div className="action-menu-container">
                        {(actions || []).map((action, index) =>
                            <Button key={index}
                                    variant={action.secondary ? "outline" : "default"}
                                    onClick={() => !action.disabled && action.perform()}>{action.name}</Button>)
                        }
                        {showChevronAction &&
                            <DropdownMenu>
                                <DropdownMenuTrigger render={
                                    <Button variant="outline">
                                        <span>{I18n.t("home.otherOptions")}</span>
                                        <CaretDownIcon/>
                                    </Button>
                                }/>
                                <DropdownMenuContent align="end" className="action-menu-content">
                                    {(history && auditLogPath) &&
                                        <DropdownMenuItem
                                            onClick={() => navigate(`/audit-logs/${auditLogPath}?${queryParam}`)}>
                                            {I18n.t("home.history")}
                                        </DropdownMenuItem>}
                                    {firstTime &&
                                        <DropdownMenuItem onClick={() => firstTime()}>
                                            {I18n.t("home.firstTime")}
                                        </DropdownMenuItem>}
                                </DropdownMenuContent>
                            </DropdownMenu>}
                    </div>}
                {customAction && customAction}
            </div>
        </div>
    )
}
