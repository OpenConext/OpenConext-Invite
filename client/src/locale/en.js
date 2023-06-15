const en = {
    code: "EN",
    name: "English",
    select_locale: "Change language to English",
    landing: {
        header: {
            title: "Manage access to your applications",
            login: "Login",
            sup: "The Access application is by invite only.",
        },
        works: "How does it work?",
        adminFunction: "admin function",
        info: [
            //Arrays of titles and info blocks and if a function is an admin function
            ["Invites", "<p>SURF invites institution managers who can create roles for their applications.</p>" +
            "<p>Applications are services connected to SURFconext.</p>", true],
            ["Roles", "<p>The application managers will invite colleagues for roles who can invite guests.</p>" +
            "<p>Invites.</p>", true],
            ["Join", "<p>Invited colleagues who accept the invitation are granted access rights for the applications.</p><br/>", false],
            ["Groups", "<p>The roles are actually group memberships that can be provisioned to external SCIM API's.</p>", false]
        ],
        footer: "<p>SURF Access is a service for access management of Dutch led service providers.</p>" +
            "<p>Do you want to know more? Please visit <a href='https://surf.nl/en/access'>https://surf.nl/en/access</a>.</p>",
    },
    header: {
        title: "Access",
        subTitle: "Everything will be Owl right",
        links: {
            login: "Login",
            system: "System",
            impersonate: "Impersonate",
            createApplication: "Create application",
            help: "Help",
            profile: "Profile",
            logout: "Logout",
            helpUrl: "https://edu.nl/vw3jx"
        },
    },
    tabs: {
        home: "Home",
        applications: "Applications",
        users: "Users",
        roles: "Roles",
        profile: "Profile"
    },
    home: {
        access: "SURF Access",
    },
    impersonate: {
        exit: "Stop impersonating",
        impersonator: "You are impersonating <strong>{{name}}</strong> | <strong>{{role}}</strong>",
        impersonatorTooltip: "You are really <em>{{impersonator}}</em>, but you are impersonating <em>{{currentUser}}</em>.",
        flash: {
            startedImpersonation: "You now impersonate {{name}}.",
            clearedImpersonation: "Cleared your impersonation. You are you again."
        },
    },
    access: {
        SUPER_USER: "Super User",
        MANAGER: "Manager",
        INVITER: "Inviter",
        GUEST: "Guest",
    },
    users: {
        found: "{{count}} {{plural}} found",
        moreResults: "There are more results then shown, please refine your search.",
        name_email: "Name / email",
        name: "Name",
        email: "Email",
        highestAuthority: "Role",
        schacHomeOrganization: "Institution",
        lastActivity: "Last activity",
        eduPersonPrincipalName: "EPPN",
        sub: "Sub",
        singleUser: "user",
        multipleUsers: "users",
        noEntities: "No users found",
        new: "New invitation",
        title: "Users",
        roles: "Roles",
        noRolesInfo: "You have no roles (which means you must be super-user)",
        rolesInfo: "You have the following roles",
        landingPage: "Website",
        access: "Access",
        organisation: "Organisation",
        noResults: "No users are found",
        searchPlaceHolder: "Search for users...",
        authority: "Authority",
        endDate: "End date"
    },
    forms: {
        none: "None"
    },
    profile: {
        info: "The account of {{name}} was created on {{createdAt}}"
    },
    inviteOnly: {
        welcome: "Welcome to SURF Access",
        roles: "You don't have any roles.",
        info: "The Access application is by invite only. If you want to enter, but don't have access, please contact <a href='mailto:access@surf.nl'>access@surf.nl</a>.",
        preLogin: "Or ",
        login: "login",
        postLogin: " again with a different institution",
    },
    tooltips: {
        userIcon: "User {{name}} provisioned at {{createdAt}} with last activity on {{lastActivity}}",
        impersonateIcon: "Impersonate user {{name}}"
    },
    footer: {
        terms: "Terms of Use",
        termsLink: "https://edu.nl/6wb63",
        privacy: "Privacy policy",
        privacyLink: "https://edu.nl/fcgbd",
        surfLink: "https://surf.nl",
    },
    expirations: {
        expires: "Expires {{relativeTime}}",
        expired: "Expired {{relativeTime}}",
        never: "Never expires",
        activity: {
            now: "Just now",
            seconds: "Today",
            minute: "Today",
            minutes: "Today",
            hour: "Today",
            hours: "Today",
            day: "Yesterday",
            days: "This week",
            week: "This week",
            weeks: "This month",
            month: "Last month",
            months: "%s months ago",
            year: "1 year ago",
            years: "%s years ago"
        },
        ago: {
            now: "just now",
            seconds: "%s seconds ago",
            minute: "1 minute ago",
            minutes: "%s minutes ago",
            hour: "1 hour ago",
            hours: "%s hours ago",
            day: "1 day ago",
            days: "%s days ago",
            week: "1 week ago",
            weeks: "%s weeks ago",
            month: "1 month ago",
            months: "%s months ago",
            year: "1 year ago",
            years: "%s years ago"
        },
        in: {
            now: "right now",
            seconds: "in %s seconds",
            minute: "in 1 minute",
            minutes: "in %s minutes",
            hour: "in 1 hour",
            hours: "in %s hours",
            day: "in 1 day",
            days: "in %s days",
            week: "in 1 week",
            weeks: "in %s weeks",
            month: "in 1 month",
            months: "in %s months",
            year: "in 1 year",
            years: "in %s years"
        }
    }
}

export default en;