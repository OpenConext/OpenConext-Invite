import React, {useState} from "react";
import "./MoreLessText.scss";
import I18n from "../locale/I18n";
import {isEmpty, stopEvent} from "../utils/Utils";

export const MoreLessText = ({txt, type = "full"}) => {

    const [showMore, setShowMore] = useState(!isEmpty(txt) && txt.length > 190
        && txt.substring(190).indexOf(" ") > -1);
    const [showLess, setShowLess] = useState(false);

    const toggleShowMore = e => {
        stopEvent(e);
        const isShowingMore = showMore;
        setShowMore(!isShowingMore);
        setShowLess(isShowingMore);
    }

    const txtToDisplay = isEmpty(txt) ? txt : txt.substring(0, 190 + txt.substring(190).indexOf(" "));

    return (
        <span className={`more-less-txt description ${type}`}>
            {showMore ? txtToDisplay : txt}
            {showMore && <a className={"show-more"} href="/more" onClick={toggleShowMore}>
                {I18n.t("forms.more")}
            </a>}
            {showLess &&
                <a className={"show-more"} href="/less" onClick={toggleShowMore}>
                    {I18n.t("forms.less")}
                </a>}
        </span>
    )
}