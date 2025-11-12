import {NavigationMenu} from "@surfnet/sds"
import HomeIcon from "@surfnet/sds/icons/illustrative-icons/home.svg";
import TeamIcon from "@surfnet/sds/icons/illustrative-icons/team.svg";
import IdIcon from "@surfnet/sds/icons/functional-icons/id-2.svg";
import ScreenIcon from "@surfnet/sds/icons/illustrative-icons/screen.svg";
import ShieldCheckIcon from "@surfnet/sds/icons/illustrative-icons/shield-check.svg";
import {useLocation, useNavigate} from "react-router-dom";

export const SharedMenu = () => {
    const {pathname, assign} = useLocation();
    const navigate = useNavigate();

    const isActive = (path) => pathname === path;

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
            items: [
                {
                    label: 'Access roles',
                    Logo: TeamIcon,
                    href: '/home/roles',
                },
                {
                    label: 'Users',
                    Logo: IdIcon,
                    href: '/home/users',
                },
                {
                    label: 'Applications',
                    Logo: ScreenIcon,
                    href: '/home/applications',
                },
                {
                    label: 'API tokens',
                    Logo: ShieldCheckIcon,
                    href: '/home/tokens',
                }
            ]
        }
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
            <div
                style={{
                    color: 'white',
                    marginTop: '140px'
                }}
            >
                <span>NL</span>{' '}|{' '}<span>EN</span>
            </div>
        </NavigationMenu>
    );
}
