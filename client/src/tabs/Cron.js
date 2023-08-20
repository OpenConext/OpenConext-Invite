import React, {useState} from "react";
import I18n from "../locale/I18n";
import "./Cron.scss";
import {Button} from "@surfnet/sds";
import "./Users.scss";
import {cron} from "../api";
import {isEmpty} from "../utils/Utils";
import {allExpanded, defaultStyles, JsonView} from 'react-json-view-lite';
import 'react-json-view-lite/dist/index.css';

export const Cron = () => {
    const [results, setResults] = useState({});

    return (
        <div className="mod-cron-container">
            <div className="mod-cron">
                <div className="actions">
                    <span>{I18n.t("system.cronInfo")}</span>
                    {isEmpty(results) && <Button onClick={() => cron().then(res => setResults(res))}
                                                 txt={I18n.t("system.trigger")}/>}
                    {!isEmpty(results) && <Button onClick={() => setResults({})}
                                                  txt={I18n.t("system.clear")}/>}

                </div>
                {!isEmpty(results) &&
                    <div className="cron-results">
                        <JsonView data={results} shouldInitiallyExpand={allExpanded} style={defaultStyles}/>
                    </div>}
            </div>

        </div>)

}
