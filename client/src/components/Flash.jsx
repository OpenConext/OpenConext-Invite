import React, {useEffect} from "react";
import {isEmpty, sanitize} from "../utils/Utils";
import {Alert, AlertAction, AlertDescription} from "@surfnet/curve-react";
import {WarningIcon, XIcon} from "@phosphor-icons/react";
import {toast} from "sonner";
import {useAppStore} from "../stores/AppStore"
import "./Flash.scss";

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
            <div className="flash-container">
                <Alert variant={flash.type === "error" ? "danger" : "default"}>
                    {flash.type === "error" ? <WarningIcon/> : null}
                    <AlertDescription dangerouslySetInnerHTML={{__html: sanitize(flash.msg)}}/>
                    {flash.action && <button type="button" className="alert-action"
                                             onClick={flash.action}>{flash.actionLabel}</button>}
                    <AlertAction>
                        <button type="button" onClick={clearFlash}><XIcon/></button>
                    </AlertAction>
                </Alert>
            </div>
        );
    }
    return null;
}

