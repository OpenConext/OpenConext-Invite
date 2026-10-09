import React, {useEffect} from "react";
import {isEmpty, sanitize} from "../utils/Utils";
import {Alert, AlertAction, AlertDescription} from "@surfnet/curve-react";
import {WarningIcon, XCircleIcon, XIcon} from "@phosphor-icons/react";
import {toast} from "sonner";
import {useAppStore} from "../stores/AppStore"

export const Flash = () => {

    const flash = useAppStore(state => state.flash);
    const clearFlash = useAppStore(state => state.clearFlash);

    const isAlert = !isEmpty(flash) && (flash.type === "error" || flash.type === "warning");

    useEffect(() => {
        if (!isEmpty(flash) && !isAlert && !isEmpty(flash.msg)) {
            toast.success(<span dangerouslySetInnerHTML={{__html: sanitize(flash.msg)}}/>);
            clearFlash();
        }
    }, [flash]); // eslint-disable-line react-hooks/exhaustive-deps

    if (isAlert) {
        return (
            <Alert variant={flash.type === "error" ? "destructive" : "default"}>
                {flash.type === "error" ? <XCircleIcon/> : <WarningIcon/>}
                <AlertDescription dangerouslySetInnerHTML={{__html: sanitize(flash.msg)}}/>
                {flash.action && <button type="button" className="alert-action"
                                          onClick={flash.action}>{flash.actionLabel}</button>}
                <AlertAction>
                    <button type="button" onClick={clearFlash}><XIcon/></button>
                </AlertAction>
            </Alert>
        );
    }
    return null;
}

