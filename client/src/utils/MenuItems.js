import {
    AppWindowIcon as ScreenIcon,
    SquaresFourIcon as LaptopIcon,
    LinkIcon as ConnectedIcon,
    LockIcon as PolicyIcon,
    UserCircleCheckIcon as TeamIcon,
    FediverseLogoIcon as HierarchyIcon,
    BuildingIcon as LaptopFloatIcon,
    UserRectangleIcon as UserIcon,
    ChartLineIcon as StatsIcon,
    HouseIcon as HomeIcon,
    LifebuoyIcon as HeadPhonesIcon,
    ChatCenteredTextIcon as FeedbackIcon
} from "@phosphor-icons/react";

// The sidebar is a copy of the SURF Access sidebar. Every item points to the Access
// application, except for "invite" (Roles), which is this application.
// The base url of SURF Access comes from the server (config.access-url), so one build works in every environment.
// VITE_ACCESS_URL is only a fallback for local development.
export const accessUrl = config => ((config && config.accessUrl) || import.meta.env.VITE_ACCESS_URL || "").replace(/\/$/, "");

export const mainMenuItems = {
    home: "home",
    users: "users",
    idp: "idp",
    yourApps: "yourApps",
    catalogue: "catalogue",
    policies: "policies",
    accessibleApps: "accessibleApps",
    invite: "invite",
    sram: "sram",
    serviceDesk: "serviceDesk",
    feedback: "feedback",
    statistics: "statistics"
}

// Paths are the Access paths. Items with internal: true are served by this application.
export const allMenuGroups = [
    {
        label: null,
        items: [
            {name: mainMenuItems.home, path: "/home", Logo: HomeIcon}
        ]
    },
    {
        label: "applications",
        items: [
            {name: mainMenuItems.yourApps, path: "/organization/organizationId", Logo: ScreenIcon},
            {name: mainMenuItems.catalogue, path: "/catalogue", Logo: LaptopIcon},
            {name: mainMenuItems.accessibleApps, path: "/accessible-apps", Logo: ConnectedIcon}
        ]
    },
    {
        label: "externalMaintenance",
        items: [
            {name: mainMenuItems.policies, path: "/policies/overview", Logo: PolicyIcon},
            {name: mainMenuItems.invite, path: "/home/roles", Logo: TeamIcon, internal: true},
            {name: mainMenuItems.sram, path: "/external/sram", Logo: HierarchyIcon}
        ]
    },
    {
        label: "organisation",
        items: [
            {name: mainMenuItems.idp, path: "/idp/organizationId", Logo: LaptopFloatIcon},
            {name: mainMenuItems.users, path: "/users/organizationId", Logo: UserIcon},
            {name: mainMenuItems.statistics, path: "/statistics", Logo: StatsIcon}
        ]
    },
    {
        label: null,
        className: "custom-group",
        items: [
            {name: mainMenuItems.serviceDesk, path: "/external/serviceDesk", Logo: HeadPhonesIcon},
            {name: mainMenuItems.feedback, path: "/feedback", Logo: FeedbackIcon}
        ]
    }
];
