import "./Applications.scss";
import {useAppStore} from "../stores/AppStore";
import {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";
import {highestAuthority} from "../utils/UserRole";

export const Roles = () => {
    const user = useAppStore(state => state.user);
    const {roleSearchRequired} = useAppStore(state => state.config);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [roles, setRoles] = useState([]);

    useEffect(() => {
        const authority = highestAuthority(user);
        debugger;
        if (!roleSearchRequired) {

        }
        setLoading(false);
    }, []);

    if (loading) {
        return <Loader/>
    }

    return (
        <div className={"mod-roles"}>
            TODO
            {/*<Entities*/}

            {/*/>*/}
        </div>
    );

}
