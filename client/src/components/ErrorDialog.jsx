import React from "react";
import I18n from "../locale/I18n";
import ConfirmationDialog from "./ConfirmationDialog";

export default function ErrorDialog({isOpen = false, close}) {

    if (!isOpen) {
        return null;
    }

    return (
        <ConfirmationDialog confirm={close}
                            isError={true}
                            question={I18n.t("error_dialog.body")}
                            confirmationHeader={I18n.t("error_dialog.title")}
                            confirmationTxt={I18n.t("error_dialog.ok")}/>
    );

}
