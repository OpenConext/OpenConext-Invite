import "./SharedMenu.scss";
import {NavigationMenu} from "@surfnet/sds"
import HomeIcon from "@surfnet/sds/icons/illustrative-icons/home.svg";
import TeamIcon from "@surfnet/sds/icons/illustrative-icons/team.svg";
import IdIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";
import ScreenIcon from "@surfnet/sds/icons/illustrative-icons/screen.svg";
import ShieldCheckIcon from "@surfnet/sds/icons/illustrative-icons/shield-check.svg";
import HeadPhonesIcon from "@surfnet/sds/icons/illustrative-icons/headphones.svg";
import FeedbackIcon from "@surfnet/sds/icons/illustrative-icons/feedback.svg";
import {useLocation, useNavigate} from "react-router-dom";
import {SharedMenuFooter} from "./SharedMenuFooter";
import {InviteTabs, useUserTabs} from "../hooks/useUserTabs";
import I18n from "../locale/I18n";

export const SharedMenu = () => {
    const {pathname} = useLocation();
    const navigate = useNavigate();
    const { userTabs } = useUserTabs();

    const isActive = (path) => pathname === path;

    const allMenuItems = {
        [InviteTabs.ROLES]: {
            label: I18n.t(`tabs.${InviteTabs.ROLES}`),
            Logo: TeamIcon,
            href: '/home/roles',
        },
        [InviteTabs.USERS]: {
            label: I18n.t(`tabs.${InviteTabs.USERS}`),
            Logo: IdIcon,
            href: '/home/users',
        },
        [InviteTabs.APPLICATIONS]: {
            label: I18n.t(`tabs.${InviteTabs.APPLICATIONS}`),
            Logo: ScreenIcon,
            href: '/home/applications',
        },
        [InviteTabs.APPLICATION_USERS]: {
            label: I18n.t(`tabs.${InviteTabs.APPLICATION_USERS}`),
            Logo: IdIcon,
            href: '/home/application-users',
        },
        [InviteTabs.TOKENS]: {
            label: I18n.t(`tabs.${InviteTabs.TOKENS}`),
            Logo: ShieldCheckIcon,
            href: '/home/tokens',
        }
    }

    const menuGroups = [
        {
            label: null,
            items: [
                {
                    label: 'Home',
                    Logo: HomeIcon,
                    href: '/home'
                }
            ]
        },
        {
            items: userTabs.map(tab => allMenuItems[tab])
        },
        {
            label: 'support',
            className: 'custom-group',
            items: [
                {
                    label: 'SURF Servicedesk',
                    href: '/external/serviceDesk',
                    Logo: HeadPhonesIcon,
                },
                {
                    label: 'Provide Feedback',
                    href: '/feedback',
                    Logo: FeedbackIcon,
                },
            ]
        },
    ];

    const menuGroupsWithActiveState = menuGroups.map(group => ({
        ...group,
        items: group.items.map(item => ({
            ...item,
            active: isActive(item.href)
        }))
    }));

    return (
        <NavigationMenu
            groups={ menuGroupsWithActiveState }
            logoLabel="SURFconext Invite"
            setActiveMenuItem={(menuItem) => navigate(menuItem.href)}
            title="SURFconext Invite"
        >
            <SharedMenuFooter />
        </NavigationMenu>
    );
}
