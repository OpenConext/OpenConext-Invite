import React from "react";
import {Modal,} from "@surfnet/sds";
import I18n from "../locale/I18n";


export default function ConfirmationDialog({
                                               isOpen = false,
                                               cancel,
                                               confirm,
                                               question = "",
                                               isError = false,
                                               isWarning = false,
                                               disabledConfirm = false,
                                               children = null,
                                               confirmationTxt = I18n.t("confirmationDialog.confirm"),
                                               largeWidth = false,
                                               confirmationHeader = I18n.t("confirmationDialog.title")
                                           }) {
    if (!isOpen) {
        return null;
    }
    return (
        <Modal
            confirm={confirm}
            cancel={cancel}
            alertType={null}
            question={question}
            children={children}
            title={confirmationHeader}
            cancelButtonLabel={I18n.t("confirmationDialog.cancel")}
            confirmationButtonLabel={confirmationTxt}
            confirmDisabled={disabledConfirm}
            subTitle={null}
            full={largeWidth}/>
    );

}

