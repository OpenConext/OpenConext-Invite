import React, {useState} from "react";
import {Modal,} from "@surfnet/sds";
import I18n from "../locale/I18n";
import {isEmpty} from "../utils/Utils";


export default function ConfirmationDialog({
                                               isOpen = false,
                                               cancel,
                                               confirm,
                                               question = "",
                                               isError = false,
                                               disabledConfirm = false,
                                               children = null,
                                               confirmationTxt = I18n.t("confirmationDialog.confirm"),
                                               largeWidth = false,
                                               confirmationHeader = I18n.t("confirmationDialog.title")
                                           }) {
    const [busy, setBusy] = useState(false);

    if (!isOpen) {
        return null;
    }
    return (
        <Modal
            confirm={() => {
                setBusy(true);
                confirm();
            }}
            cancel={cancel}
            alertType={null}
            question={question}
            isError={isError}
            children={children}
            title={confirmationHeader}
            cancelButtonLabel={I18n.t("confirmationDialog.cancel")}
            confirmationButtonLabel={confirmationTxt}
            confirmDisabled={disabledConfirm || (busy && cancel)}
            subTitle={null}
            focusConfirm={!disabledConfirm && isEmpty(children)}
            full={largeWidth}/>
    );

}

