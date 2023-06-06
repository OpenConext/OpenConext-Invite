const en = {
    code: "EN",
    name: "English",
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
    paths: {
        home: "Home"
    },
    home: {
        access: "SURF Access",
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