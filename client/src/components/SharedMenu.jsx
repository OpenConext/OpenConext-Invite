import {NavigationMenu} from "@surfnet/sds"
import {ReactComponent as HomeIcon} from "@surfnet/sds/icons/illustrative-icons/home.svg";

export const SharedMenu = () => {
    const filteredMenuGroups = [
        {
            label: null,
            items: [
                {
                    Logo: HomeIcon,
                    active: false,
                    href: '/home',
                    label: 'Home'
                }
            ]
        },
        {
            // Todo: items need to come from logic in "Home.js" (create a context)
            items: [
                {
                    label: 'Access roles',
                    Logo: () => {},
                    href: '/home/roles',
                    active: false
                },
                {
                    label: 'Users',
                    Logo: () => {},
                    href: '/home/users',
                    active: false,
                },
                {
                    label: 'Applications',
                    Logo: () => {},
                    href: '/home/applications',
                    active: false,
                },
                {
                    label: 'API tokens',
                    Logo: () => {},
                    href: '/home/tokens',
                    active: false,
                }
            ]
        }
    ]

    return (
        <NavigationMenu
            groups={ filteredMenuGroups }
            logoLabel="SURFconext Invite"
            setActiveMenuItem={() => {}}
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

