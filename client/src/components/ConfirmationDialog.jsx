import React, {useState} from "react";
import {Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle} from "@surfnet/curve-react";
import I18n from "../locale/I18n";
import {sanitize} from "../utils/Utils";
import "./ConfirmationDialog.scss";


export default function ConfirmationDialog({
                                               isOpen = true,
                                               cancel,
                                               confirm,
                                               question = "",
                                               isError = false,
                                               isDeleteAction = false,
                                               disabledConfirm = false,
                                               children = null,
                                               confirmationTxt = I18n.t("confirmationDialog.confirm"),
                                               largeWidth = false,
                                               confirmationHeader = I18n.t("confirmationDialog.title"),
                                               className = ""
                                           }) {
    const [busy, setBusy] = useState(false);

    if (!isOpen) {
        return null;
    }
    return (
        <Dialog open={true} onOpenChange={open => !open && cancel && cancel()}>
            <DialogContent showCloseButton={!!(cancel)}
                            className={`confirmation-dialog ${largeWidth ? "large-width" : ""} ${className}`}>
                {confirmationHeader && <DialogHeader>
                    <DialogTitle className={isError ? "error" : ""}>{confirmationHeader}</DialogTitle>
                </DialogHeader>}
                {question && <p className="confirmation-question" dangerouslySetInnerHTML={{__html: sanitize(question)}}/>}
                {children}
                {(cancel || confirm) && <DialogFooter>
                    {cancel && <Button variant="outline" onClick={cancel}>
                        <span dangerouslySetInnerHTML={{__html: sanitize(I18n.t("confirmationDialog.cancel"))}}/>
                    </Button>}
                    {confirm && <Button variant={isDeleteAction ? "destructive" : "default"}
                                        disabled={disabledConfirm || (busy && cancel)}
                                        onClick={() => {
                                            setBusy(true);
                                            confirm();
                                        }}>
                        <span dangerouslySetInnerHTML={{__html: sanitize(confirmationTxt)}}/>
                    </Button>}
                </DialogFooter>}
            </DialogContent>
        </Dialog>
    );

}

