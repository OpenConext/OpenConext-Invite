import React from "react";
import {AlertType, Modal,} from "@surfnet/sds";
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
            alertType={isError ? AlertType.Error : isWarning ? AlertType.Warning : AlertType.Info}
            question={question}
            children={children}
            title={confirmationHeader || isError ? I18n.t("confirmationDialog.error") : I18n.t("confirmationDialog.title")}
            cancelButtonLabel={I18n.t("confirmationDialog.cancel")}
            confirmationButtonLabel={confirmationTxt}
            confirmDisabled={disabledConfirm}
            subTitle={isError ? I18n.t("confirmationDialog.subTitleError") : I18n.t("confirmationDialog.subTitle")}
            full={largeWidth}/>
    );

}

