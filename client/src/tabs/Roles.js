import "./Applications.scss";
import {useAppStore} from "../stores/AppStore";
import {useEffect, useState} from "react";
import {Entities} from "../components/Entities";
import I18n from "../locale/I18n";
import {Loader} from "@surfnet/sds";
import {useNavigate} from "react-router-dom";

const Roles = () => {
    const user = useAppStore(state => state.user);
    const {roleSearchRequired} = useAppStore(state => state.config);
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [roles, setRoles] = useState([]);

    useEffect(() => {
        useAppStore.setState({
            breadcrumbPath:
                [
                    {path: "/home", value: I18n.t("paths.home")},
                    {path: "/home/roles", value: I18n.t("paths.roles")}
                ]
        });
        if (!roleSearchRequired) {

        }
    }, [navigate]);

    if (loading) {
        return <Loader/>
    }

    return (
        <div className={"mod-roles"}>
            <Entities

            />
        </div>
    );

}

export default Roles;