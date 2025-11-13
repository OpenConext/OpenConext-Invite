import I18n from "../locale/I18n";
import React, {useState} from "react";
import "./UserMenu.scss";
import {useNavigate} from "react-router-dom";
import {isEmpty, stopEvent} from "../utils/Utils";
import {UserInfo} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";


export const UserMenu = ({user, actions}) => {
    const navigate = useNavigate();

    const [dropDownActive, setDropDownActive] = useState(false);

    const {objectRole} = useAppStore((state) => state);

    const logoutUser = e => {
        stopEvent(e);
        logout().then(() => {
            useAppStore.setState(() => ({breadcrumbPath: []}));
            navigate("/login", {state: "force"});
            setTimeout(() =>
                useAppStore.setState(() => ({user: null, impersonator: null, breadcrumbPath: []})), 500);
        });
    }

    const renderMenu = () => {
        return (<>
                {!isEmpty(actions) && <ul>
                    {actions.map(action => <li key={action.name}>
                        <a href={`/${action.name}`} onClick={action.perform}>{action.name}</a>
                    </li>)}
                </ul>}
                <ul>
                    <li>
                        <a href="/logout" onClick={logoutUser}>{I18n.t(`header.links.logout`)}</a>
                    </li>
                </ul>
            </>
        )
    }

    return (
        <div className="user-menu"
             tabIndex={1}
             onBlur={() => setTimeout(() => setDropDownActive(false), 250)}>
            <UserInfo isOpen={dropDownActive}
                      children={renderMenu()}
                      organisationName={objectRole || ""}
                      userName={user.name}
                      toggle={() => setDropDownActive(!dropDownActive)}
            />
        </div>
    );


}
