import {Button, ButtonSize, ButtonType} from "@surfnet/sds";
import './Login.scss';
import I18n from "../locale/I18n";
import DOMPurify from "dompurify";
import {LandingInfo} from "../components/LandingInfo";
import HappyLogo from "../icons/landing/undraw_startled_-8-p0r.svg";
import {login} from "../utils/Login";
import {useAppStore} from "../stores/AppStore";
import {isEmpty} from "../utils/Utils";
import {useLocation} from "react-router-dom";
import {useState} from "react";

export const Login = () => {
    const location = useLocation();
    const [spin, setSpin] = useState(false);

    const config = useAppStore((state) => state.config);

    const doLogin = () => {
        const force = location.state === "force";
        login(config, !isEmpty(force));
    }

    const toggleSpin = () => {
        setSpin(true);
        setTimeout(() => setSpin(false), 725);
    }

    return (
        <div className="top-container">
            <div className="mod-login-container">
                <div className="mod-login">
                    <div className="header-left">
                        <h2 className={"header-title"}
                            dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("landing.header.title"))}}/>
                        <Button onClick={doLogin}
                                txt={I18n.t("landing.header.login")}
                                type={ButtonType.Primary}
                                size={ButtonSize.Full}/>
                        <p className={"sup"}
                           dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(I18n.t("landing.header.sup"))}}/>
                    </div>
                    <div className="header-right">
                        <img className={spin ? "spin" : ""} onClick={() => toggleSpin()} src={HappyLogo} alt="logo"/>
                    </div>
                </div>
            </div>
            <LandingInfo/>
        </div>
    );

}