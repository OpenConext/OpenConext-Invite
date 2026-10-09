import {Button} from "@surfnet/curve-react";
import React, {useState} from "react";
import I18n from "../locale/I18n";
import "./Cron.scss";
import "./Users.scss";
import {cronCleanup, cronExpiryNotifications} from "../api";
import {isEmpty} from "../utils/Utils";
import {allExpanded, defaultStyles, JsonView} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';
import DOMPurify from "dompurify";

export const Cron = () => {
    const [results, setResults] = useState({});
    const [mailResults, setMailResults] = useState({});

    const resourceCleanerCron = () => {
        return <div className="mod-cron">
            <div className="actions">
                <span>{I18n.t("system.cronInfo")}</span>
                {isEmpty(results) && <Button onClick={() => cronCleanup().then(res => setResults(res))}>{I18n.t("system.trigger")}</Button>}
                {!isEmpty(results) && <Button onClick={() => setResults({})}>{I18n.t("system.clear")}</Button>}

            </div>
            {!isEmpty(results) &&
                <div className="cron-results">
                    <JsonView data={results} shouldInitiallyExpand={allExpanded} style={defaultStyles}/>
                </div>}
        </div>;
    }

    const expiredNotificationsCron = () => {
        return <div className="mod-cron">
            <div className="actions">
                <span>{I18n.t("system.cronNotificationsInfo")}</span>
                {isEmpty(mailResults) &&
                    <Button onClick={() => cronExpiryNotifications().then(res => setMailResults(res))}>{I18n.t("system.trigger")}</Button>}
                {!isEmpty(mailResults) && <Button onClick={() => setMailResults({})}>{I18n.t("system.clear")}</Button>}

            </div>
            {!isEmpty(mailResults) &&
                <div className="cron-results">
                    {mailResults["mails"].map((mail, index) => <div key={index} className="mail-content">
                        <p dangerouslySetInnerHTML={{
                            __html: DOMPurify.sanitize(mail)
                        }}/>
                    </div>)}
                    {isEmpty(mailResults["mails"]) && <p>
                        {I18n.t("system.noMails")}
                    </p>}
                </div>}
        </div>;
    }

    return (
        <div className="mod-cron-container">
            {resourceCleanerCron()}
            {expiredNotificationsCron()}
        </div>)

}
