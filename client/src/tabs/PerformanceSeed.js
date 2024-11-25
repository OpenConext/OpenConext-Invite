import React, {useState} from "react";
import I18n from "../locale/I18n";
import "./PerformanceSeed.scss";
import {Button, Loader} from "@surfnet/sds";
import {performanceSeed} from "../api";


export const PerformanceSeed = () => {
    const [seed, setSeed] = useState(null);
    const [loading, setLoading] = useState(false);
    const [millis, setMillis] = useState(0);

    if (loading) {
        return <Loader/>
    }

    const doPerformanceSeed = () => {
        setLoading(true);
        const start = new Date().getTime();
        performanceSeed().then(res => {
            setSeed(res);
            setLoading(false);
            setMillis(new Date().getTime() - start);
        })
    }

    return (
        <div className="mod-performance-seed-container">
            <div className="mod-performance-seed">
                <div className="actions">
                    <p>{I18n.t("system.performanceSeedInfo")}</p>
                    <Button onClick={doPerformanceSeed}
                            txt={I18n.t("system.performanceSeed")}
                    />
                </div>
                {seed &&
                    <div className="results">
                        <p>{I18n.t("system.performanceMillis", {millis: millis})}</p>
                        <code className="results">
                            {JSON.stringify(seed, null, 4)}
                        </code>
                    </div>}
            </div>


        </div>)

}
