import I18n from "../locale/I18n";
import React from "react";
import "./UserMenu.scss";
import {Link, useNavigate} from "react-router";
import {
    Button,
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger
} from "@surfnet/curve-react";
import {CaretDownIcon} from "@phosphor-icons/react";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";

export const UserMenu = ({user}) => {
    const navigate = useNavigate();
    const clearFlash = useAppStore(state => state.clearFlash);

    const authority = highestAuthority(user);
    const roleName = I18n.t(`access.${highestAuthority(user, false)}`);
    const apiTokenLink = authority === AUTHORITIES.INVITER || authority === AUTHORITIES.MANAGER ||
        authority === AUTHORITIES.APPLICATION_MANAGER || authority === AUTHORITIES.INSTITUTION_ADMIN;

    const logoutUser = () => {
        logout().then(() => {
            //The sidebar and header are only for authenticated users, so switch to the public layout directly
            navigate("/login", {state: "force"});
            useAppStore.setState(() => ({authenticated: false, user: {}, impersonator: null, breadcrumbPath: []}));
        });
    }

    const link = (to, label) =>
        <DropdownMenuItem render={<Link to={to} onClick={() => clearFlash()}>{label}</Link>}/>;

    return (
        <DropdownMenu>
            <DropdownMenuTrigger render={
                <Button variant="ghost" className="user-menu-trigger">
                    <span className="user-menu-textual">
                        <span className="user-menu-name">{user.name}</span>
                        <span className="user-menu-organization">{roleName}</span>
                    </span>
                    <CaretDownIcon/>
                </Button>
            }/>
            <DropdownMenuContent align="end" className="user-menu-content">
                <DropdownMenuGroup>
                    <DropdownMenuLabel>
                        <span className="user-menu-name">{user.name}</span>
                        <span className="user-menu-organization">{roleName}</span>
                    </DropdownMenuLabel>
                </DropdownMenuGroup>
                <DropdownMenuSeparator/>
                {link("/profile", I18n.t("header.links.profile"))}
                {apiTokenLink && link("/tokens", I18n.t("header.links.tokens"))}
                {(!user.superUser && user.institutionAdmin && user.organizationGUID) &&
                    link("/institution-admins", I18n.t("header.links.colleagues"))}
                {(user.superUser || (user.institutionAdmin && user.organizationGUID)) &&
                    link("/audit", I18n.t("header.links.audit"))}
                {user.superUser && link("/system", I18n.t("header.links.system"))}
                <DropdownMenuItem onClick={logoutUser}>
                    {I18n.t("header.links.logout")}
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
