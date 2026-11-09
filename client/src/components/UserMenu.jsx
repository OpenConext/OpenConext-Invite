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
    DropdownMenuTrigger,
    SidebarMenuButton
} from "@surfnet/curve-react";
import {CaretDownIcon, CaretRightIcon, UserCircleIcon} from "@phosphor-icons/react";
import {useAppStore} from "../stores/AppStore";
import {logout} from "../api";
import {markExplicitLogout} from "../utils/Login";
import {AUTHORITIES, highestAuthority} from "../utils/UserRole";

//variant sidebar: rendered in the footer of the sidebar, the menu opens to the right. Variant header: top right.
export const UserMenu = ({user, variant = "header", organizationName}) => {
    const inSidebar = variant === "sidebar";
    const navigate = useNavigate();
    const clearFlash = useAppStore(state => state.clearFlash);

    const authority = highestAuthority(user);
    const roleName = I18n.t(`access.${highestAuthority(user, false)}`);
    const apiTokenLink = authority === AUTHORITIES.INVITER || authority === AUTHORITIES.MANAGER ||
        authority === AUTHORITIES.APPLICATION_MANAGER || authority === AUTHORITIES.INSTITUTION_ADMIN;

    const logoutUser = () => {
        logout().then(() => {
            //The sidebar and header are only for authenticated users, so switch to the public layout directly
            markExplicitLogout();
            navigate("/login");
            useAppStore.setState(() => ({authenticated: false, user: {}, impersonator: null, breadcrumbPath: []}));
        });
    }

    const link = (to, label) =>
        <DropdownMenuItem render={<Link to={to} onClick={() => clearFlash()}>{label}</Link>}/>;

    return (
        <DropdownMenu>
            <DropdownMenuTrigger render={inSidebar ?
                <SidebarMenuButton size="lg" className="user-menu-trigger">
                    <UserCircleIcon className="user-menu-icon"/>
                    <span className="user-menu-textual">
                        <span className="user-menu-name">{user.name}</span>
                        <span className="user-menu-organization">{organizationName || roleName}</span>
                    </span>
                    <CaretRightIcon className="user-menu-caret"/>
                </SidebarMenuButton> :
                <Button variant="ghost" className="user-menu-trigger user-menu-trigger-header">
                    <span className="user-menu-textual">
                        <span className="user-menu-name">{user.name}</span>
                        <span className="user-menu-organization">{roleName}</span>
                    </span>
                    <CaretDownIcon/>
                </Button>
            }/>
            <DropdownMenuContent side={inSidebar ? "right" : "bottom"} align="end" className="user-menu-content">
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
