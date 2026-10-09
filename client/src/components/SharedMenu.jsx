import I18n from "../locale/I18n";
import "./SharedMenu.scss"
import {Link} from "react-router";
import React, {useEffect, useState} from "react";
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuTrigger,
    Sidebar,
    SidebarContent,
    SidebarGroup,
    SidebarGroupContent,
    SidebarGroupLabel,
    SidebarHeader,
    SidebarMenu,
    SidebarMenuButton,
    SidebarMenuItem,
    SidebarSeparator
} from "@surfnet/curve-react";
import {useAppStore} from "../stores/AppStore";
import {accessUrl, allMenuGroups, mainMenuItems} from "../utils/MenuItems";
import {CaretUpDownIcon, CheckIcon} from "@phosphor-icons/react";
import {menu as fetchMenu} from "../api";
import logoUrl from "../icons/logo2.svg?url";

export const SharedMenu = () => {

    const user = useAppStore(state => state.user);
    const ACCESS_URL = accessUrl(useAppStore(state => state.config));

    //The menu model (visible items, organizations, current organization) is owned by SURF Access
    const [menuModel, setMenuModel] = useState({menuItems: [], organizations: []});

    useEffect(() => {
        fetchMenu(new URLSearchParams(window.location.search).get("organizationId"))
            .then(res => setMenuModel(res || {menuItems: [], organizations: []}))
            .catch(() => setMenuModel({menuItems: [mainMenuItems.invite], organizations: []}));
    }, []);

    const organizations = menuModel.organizations || [];
    const currentOrganization = menuModel.currentOrganization || organizations[0];
    const canSwitchOrganization = organizations.length > 1;

    const visibleGroups = allMenuGroups
        .map(group => ({...group, items: group.items.filter(item => (menuModel.menuItems || []).includes(item.name))}))
        .filter(group => group.items.length > 0);
    const hrefFor = item => `${ACCESS_URL}${item.path.replace("organizationId", currentOrganization?.id)}`;

    const organizationButtonContent = (
        <>
            <span className="organization-name">{currentOrganization?.name || user.schacHomeOrganization}</span>
            {canSwitchOrganization && <CaretUpDownIcon className="organization-caret"/>}
        </>
    );

    return (
        <Sidebar collapsible="icon">
            <SidebarHeader>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <SidebarMenuButton size="lg" className="brand-button" render={
                            <a href={`${ACCESS_URL}/`} className="brand-logo">
                                <img src={logoUrl} alt="SURF Access"/>
                            </a>
                        }/>
                    </SidebarMenuItem>
                </SidebarMenu>
                <SidebarSeparator/>
                <SidebarMenu>
                    <SidebarMenuItem>
                        {canSwitchOrganization ?
                            <DropdownMenu>
                                <DropdownMenuTrigger render={
                                    <SidebarMenuButton size="lg">
                                        {organizationButtonContent}
                                    </SidebarMenuButton>
                                }/>
                                <DropdownMenuContent align="start" className="organization-switch-content">
                                    <DropdownMenuGroup>
                                        {organizations.map(org =>
                                            <DropdownMenuItem key={org.id}
                                                              render={
                                                                  <a href={`${ACCESS_URL}/home?organizationId=${encodeURIComponent(org.id)}`}>
                                                                      <span className="organization-option-name">{org.name}</span>
                                                                      {currentOrganization?.id === org.id && <CheckIcon/>}
                                                                  </a>
                                                              }/>
                                        )}
                                    </DropdownMenuGroup>
                                </DropdownMenuContent>
                            </DropdownMenu> :
                            <SidebarMenuButton size="lg" className="organization-name-static">
                                {organizationButtonContent}
                            </SidebarMenuButton>}
                    </SidebarMenuItem>
                </SidebarMenu>
                <SidebarSeparator/>
            </SidebarHeader>
            <SidebarContent>
                {visibleGroups.map((group, index) =>
                    <SidebarGroup key={index} className={group.className}>
                        {group.label &&
                            <SidebarGroupLabel>{I18n.t(`navigation.${group.label}`)}</SidebarGroupLabel>}
                        <SidebarGroupContent>
                            <SidebarMenu>
                                {group.items.map(item =>
                                    <SidebarMenuItem key={item.name}>
                                        <SidebarMenuButton isActive={item.name === mainMenuItems.invite}
                                                           render={item.internal ?
                                                               <Link to={item.path}>
                                                                   <item.Logo/>
                                                                   <span>{I18n.t(`navigation.${item.name}`)}</span>
                                                               </Link> :
                                                               <a href={hrefFor(item)}>
                                                                   <item.Logo/>
                                                                   <span>{I18n.t(`navigation.${item.name}`)}</span>
                                                               </a>}/>
                                    </SidebarMenuItem>
                                )}
                            </SidebarMenu>
                        </SidebarGroupContent>
                    </SidebarGroup>
                )}
            </SidebarContent>
        </Sidebar>
    );
}
