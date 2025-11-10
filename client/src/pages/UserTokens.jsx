import React, {useEffect} from "react";
import I18n from "../locale/I18n";
import "./UserTokens.scss";
import {useAppStore} from "../stores/AppStore";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as Logo} from "@surfnet/sds/icons/illustrative-icons/shield-check.svg";
import {Tokens} from "../tabs/Tokens";

export const UserTokens = () => {

    useEffect(() => {
        useAppStore.setState({
            breadcrumbPath: [
                {path: "/home", value: I18n.t("tabs.home")},
                {path: "/home/tokens", value: I18n.t("tabs.tokens")}
            ]
        });

    }, []);// eslint-disable-line react-hooks/exhaustive-deps

    return (
        <div className="mod-tokens">
            <UnitHeader obj={({name: I18n.t("tokens.userTokens"), svg: Logo, style: "small"})}>
            </UnitHeader>
            <div className="token-container">
                <Tokens/>
            </div>
        </div>);
};
