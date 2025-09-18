import I18n from "../locale/I18n";
import React, {useState} from "react";
import "./UserMenu.scss";
import {Link, useNavigate} from "react-router-dom";
import {stopEvent} from "../utils/Utils";
import {UserInfo} from "@surfnet/sds";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";


export const UserMenu = ({user, config, actions}) => {
    const navigate = useNavigate();

    const [dropDownActive, setDropDownActive] = useState(false);

    const {clearFlash} = useAppStore((state) => state);

    const toggleUserMenu = () => {
        setDropDownActive(false);
        clearFlash();
    }

    const logoutUser = e => {
        stopEvent(e);
        logout().then(() => {
            useAppStore.setState(() => ({breadcrumbPath: []}));
            navigate("/login", {state: "force"});
            setTimeout(() =>
                useAppStore.setState(() => ({user: null, impersonator: null, breadcrumbPath: []})), 500);
        });
    }

    const renderMenu = adminLinks => {
        const authority = highestAuthority(user);
        const apiTokenLink = authority === AUTHORITIES.INVITER || authority === AUTHORITIES.MANAGER;
        return (<>
                <ul>
                    {user.superUser && adminLinks.map(l =>
                        <li key={l}>
                            <Link onClick={toggleUserMenu} to={`/${l}`}>{I18n.t(`header.links.${l}`)}</Link>
                        </li>)}
                    {apiTokenLink && <li>
                        <Link onClick={toggleUserMenu} to={`/tokens`}>{I18n.t(`header.links.tokens`)}</Link>
                    </li>}
                    <li>
                        <Link onClick={toggleUserMenu} to={`/profile`}>{I18n.t(`header.links.profile`)}</Link>
                    </li>
                    {actions.map(action => <li key={action.name}>
                        <a href={`/${action.href}`} onClick={action.perform}>{action.name}</a>
                    </li>)}
                </ul>
                <ul>
                    <li>
                        <a href="/logout" onClick={logoutUser}>{I18n.t(`header.links.logout`)}</a>
                    </li>
                </ul>
            </>
        )
    }

    const adminLinks = ["system"];
    return (
        <div className="user-menu"
             tabIndex={1}
             onBlur={() => setTimeout(() => setDropDownActive(false), 325)}>
            <UserInfo isOpen={dropDownActive}
                      children={renderMenu(adminLinks)}
                      organisationName={I18n.t(`access.${highestAuthority(user, false)}`)}
                      userName={user.name}
                      toggle={() => setDropDownActive(!dropDownActive)}
            />
        </div>
    );


}
