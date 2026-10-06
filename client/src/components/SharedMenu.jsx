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
import {ACCESS_URL, allMenuGroups, mainMenuItems} from "../utils/MenuItems";
import {CaretUpDownIcon, CheckIcon} from "@phosphor-icons/react";
import {organizations as fetchOrganizations} from "../api";
import logoUrl from "../icons/logo2.svg?url";

export const SharedMenu = () => {

    const user = useAppStore(state => state.user);

    const [organizations, setOrganizations] = useState([]);

    useEffect(() => {
        fetchOrganizations().then(res => setOrganizations(res || [])).catch(() => setOrganizations([]));
    }, []);

    //The first organization is the current one, same as the default in Access
    const currentOrganization = organizations[0];
    const canSwitchOrganization = organizations.length > 1;

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
                {allMenuGroups.map((group, index) =>
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
                                                               <a href={`${ACCESS_URL}${item.path}`}>
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
