import {Button, ButtonSize, ButtonType} from "@surfnet/sds";
import {stopEvent} from "../utils/Utils";
import {useAppStore} from "../stores/AppStore";
import './Login.scss';

export const Login = () => {

    const config = useAppStore((state) => state.config)

    const doLogin = e => {
        stopEvent(e);
        window.location.href = config.serverUrl;
    }

    return (
        <div>
            <h1>Login</h1>
            <Button onClick={doLogin} txt={"Login"} type={ButtonType.Primary} size={ButtonSize.Full}/>
        </div>
    );
}