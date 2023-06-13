import "./Applications.scss";
import {useAppStore} from "../stores/AppStore";
import {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";

const Applications = () => {
    const user = useAppStore((state) => state.user);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [apps, setApps] = useState([]);

    return (
        <div className={"mod-applications"}>
            {/*<Entities*/}

            {/*/>*/}
            <p>TODO</p>
        </div>
    );

}

export default Applications;