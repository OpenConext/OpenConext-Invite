import React from "react";
import {isEmpty} from "../utils/Utils";
import "./Flash.scss";
import {Alert, AlertType, Toaster, ToasterContainer, ToasterType} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore"

export const Flash = () => {

    const flash = useAppStore(state => state.flash);
    const clearFlash = useAppStore(state => state.clearFlash);

    if (!isEmpty(flash) && (flash.type === "error" || flash.type === "warning")) {
        return (
            <Alert message={flash.message}
                   action={flash.action}
                   actionLabel={flash.actionLabel}
                   alertType={flash.type === "warning" ? AlertType.Warning : AlertType.Error}
                   close={clearFlash}/>
        );
    }
    if (!isEmpty(flash) && !isEmpty(flash.message)) {
        return (
            <ToasterContainer>
                <Toaster message={flash.message}
                         toasterType={ToasterType.Success}/>
            </ToasterContainer>);
    }
    return null;
}

