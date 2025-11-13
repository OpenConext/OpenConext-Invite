import {useAppStore} from "../stores/AppStore";
import {isEmpty} from "../utils/Utils";
import {useEffect, useState} from "react";

export const InviteTabs = {
    ROLES: 'roles',
    APPLICATION_USERS: 'application-users',
    APPLICATIONS: 'applications',
    TOKENS: 'tokens',
    USERS: 'users',
}

export const useUserTabs = () => {
    const [userTabs, setUserTabs] = useState([]);
    const user = useAppStore((state) => state.user)

    useEffect(() => {
        const updatedTabs = [ InviteTabs.ROLES ];
        if (user && user.superUser) {
            updatedTabs.push(InviteTabs.USERS);
            updatedTabs.push(InviteTabs.APPLICATIONS);
        }
        if (user && !user.superUser && user.institutionAdmin && user.organizationGUID && !isEmpty(user.applications)) {
            updatedTabs.push(InviteTabs.APPLICATION_USERS);
            updatedTabs.push(InviteTabs.APPLICATIONS);
        }
        if (user && (user.superUser || (user.institutionAdmin && user.organizationGUID))) {
            updatedTabs.push(InviteTabs.TOKENS);
        }

        setUserTabs(updatedTabs)
    }, [user])

    return {
        userTabs
    };
}
