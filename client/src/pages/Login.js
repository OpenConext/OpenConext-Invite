import {Button, ButtonSize, ButtonType} from "@surfnet/sds";
import {stopEvent} from "../utils/Utils";
import {useAppStore} from "../stores/AppStore";
import './Login.scss';
import I18n from "../locale/I18n";
import DOMPurify from "dompurify";
import {LandingInfo} from "../components/LandingInfo";
import HappyLogo from "../icons/landing/undraw_startled_-8-p0r.svg";

export const Login = () => {

    const config = useAppStore((state) => state.config)

    const doLogin = e => {
        stopEvent(e);
        window.location.href = config.serverUrl;
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
                        <img src={HappyLogo} alt="logo"/>
                    </div>
                </div>
            </div>
            <LandingInfo/>
        </div>
    );

}