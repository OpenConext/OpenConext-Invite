import React, {useEffect, useState} from "react";
import {other} from "../api";
import I18n from "../locale/I18n";
import "./Profile.scss";
import {Loader} from "@surfnet/sds";
import {useNavigate, useParams} from "react-router-dom";
import {useAppStore} from "../stores/AppStore";
import {User} from "../components/User";
import {UnitHeader} from "../components/UnitHeader";
import Logo from "@surfnet/sds/icons/functional-icons/id-1.svg";
import {dateFromEpoch} from "../utils/Date";
import {isEmpty} from "../utils/Utils";

export const Profile = () => {
    const {id} = useParams();
    const {user: currentUser, config} = useAppStore(state => state);
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        if (id) {
            other(id)
                .then(res => {
                    setUser(res);
                    setLoading(false);
                    useAppStore.setState({
                        breadcrumbPath: [
                            {path: "/home", value: I18n.t("tabs.home")},
                            {path: "/home/users", value: I18n.t("tabs.users")},
                            {value: res.name}
                        ]
                    });
                })
                .catch(() => navigate("/404"))
        } else {
            setUser(currentUser);
            setLoading(false);
            useAppStore.setState({
                breadcrumbPath: [
                    {path: "/home", value: I18n.t("tabs.home")},
                    {value: I18n.t("tabs.profile")}
                ]
            });
        }

    }, [id, currentUser]);// eslint-disable-line react-hooks/exhaustive-deps

    if (loading) {
        return <Loader/>
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
                <User user={user} other={!isEmpty(id)} config={config} currentUser={currentUser}/>
            </div>
        </div>);
};
