import React from "react";
import "./Profile.scss";
import {useAppStore} from "../stores/AppStore";
import {User} from "../components/User";
import {Welcome} from "../components/Welcome";

export const Profile = () => {
    const user = useAppStore(state => state.user);

    return (
        <div className="mod-profile">
            <div className="profile-container">
                <Welcome user={user}/>
                <User user={user}/>
            </div>
        </div>);
};
