import React, {useEffect, useState} from "react";
import I18n from "../locale/I18n";
import "./Profile.scss";
import {Loader} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {User} from "../components/User";
import {UnitHeader} from "../components/UnitHeader";
import {ReactComponent as Logo} from "@surfnet/sds/icons/functional-icons/id-1.svg";
import {dateFromEpoch} from "../utils/Date";

export const Profile = () => {
    const {user: currentUser} = useAppStore(state => state);
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        setUser(currentUser);
        setLoading(false);
        useAppStore.setState({
            breadcrumbPath: [
                {value: I18n.t("header.links.profile")}
            ]
        });

    }, [currentUser]);

    if (loading) {
        return <Loader/>
    }
    return (
        <div className="mod-profile">
            <UnitHeader obj={({name: user.name, svg: Logo, style: "small"})}>
                <p>{I18n.t("profile.info", {name: user.name, createdAt: dateFromEpoch(user.createdAt)})}</p>
            </UnitHeader>
            <div className="profile-container">
                <User user={user}/>
            </div>
        </div>);
};
