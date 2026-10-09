import {Spinner} from "@surfnet/curve-react";
import {IdentificationCardIcon as Logo} from "@phosphor-icons/react";
import React, {useEffect, useState} from "react";
import {institutionAdmins, other} from "../api";
import I18n from "../locale/I18n";
import "./Profile.scss";
import {useNavigate, useParams} from "react-router";
import {useAppStore} from "../stores/AppStore";
import {User} from "../components/User";
import {UnitHeader} from "../components/UnitHeader";
import {dateFromEpoch} from "../utils/Date";
import {isEmpty} from "../utils/Utils";

export const Profile = () => {
    const {id} = useParams();
    const {user: currentUser, config} = useAppStore(state => state);
    const [user, setUser] = useState(currentUser);
    const [otherInstitutionAdmins, setOtherInstitutionAdmins] = useState([]);

    const [loading, setLoading] = useState(!isEmpty(id) || user.institutionAdmin);

    const navigate = useNavigate();

    useEffect(() => {
        if (id) {
            other(id)
                .then(res => {
                    setUser(res);
                    setLoading(false);
                })
                .catch(() => navigate("/404"))
        } else if (user.institutionAdmin) {
            institutionAdmins()
                .then(res => {
                    setOtherInstitutionAdmins(res);
                    setLoading(false);
                })
        }

    }, [id, currentUser]);// eslint-disable-line react-hooks/exhaustive-deps

    useEffect(() => {
        if (!loading && user) {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {path: "/home/users", value: I18n.t("tabs.users")},
                    {value: user.name}
                ]
            });
        } else {
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {value: I18n.t("tabs.profile")}
                ]
            });
        }
    }, [user]);// eslint-disable-line react-hooks/exhaustive-deps

    if (loading) {
        return <div className="loading-container"><Spinner className="size-8"/></div>
    }

    return (
        <div className="mod-profile">
            <UnitHeader obj={({name: user.name, svg: Logo, style: "small"})}>
                <p>{I18n.t(`profile.${id ? "info" : "your"}`, {
                    name: user.name,
                    createdAt: dateFromEpoch(user.createdAt)
                })}</p>
            </UnitHeader>
            <div className="profile-container">
                <User user={user}
                      other={!isEmpty(id)}
                      otherInstitutionAdmins={otherInstitutionAdmins}
                      config={config}
                      currentUser={currentUser}/>
            </div>
        </div>);
};
