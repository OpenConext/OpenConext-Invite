const en = {
    code: "EN",
    name: "English",
    select_locale: "Change language to English",
    landing: {
        header: {
            title: "Get access to your applications",
            login: "Login",
            sup: "SURFconext Invite is by invite only.",
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
        footer: "<p>SURFconext Invite is a service for access management of Dutch led service providers.</p>" +
            "<p>Do you want to know more? Please visit <a href='https://surf.nl/en/invite'>https://surf.nl/en/invite</a>.</p>",
    },
    header: {
        title: "SURFconext Invite",
        subTitle: "Everything will be Owl right",
        links: {
            login: "Login",
            switchApp: "Go to {{app}}",
            welcome: "Welcome",
            access: "Access",
            help: "Help",
            profile: "Profile",
            logout: "Logout",
            helpUrl: "https://support.surfconext.nl/help-invite-en"
        },
    },
    home: {
        access: "SURFconext Invite",
    },
    users: {
        roles: "Applications",
        noRolesInfo: "You have no applications (which means you must be super-user)",
        noRolesFound: "You have not been invited for any guest roles for educational applications",
        rolesInfo: "You have access to the following applications.",
        expiryDays: "Expiry days"
    },
    forms: {
        ok: "Ok",
        and: "and",
        more: "More",
        less: "Less",
        error: "You can <a href=\"mailto:support@surfconext.nl\">contact SURFconext Invite</a> for more information.<br/><br/>" +
            "The reference number for ths exception is {{reference}}."
    },
    profile: {
        welcome: "Welcome, {{name}}",
        info: "Here are the educational applications you can access through SURFconext Invite",
        toaster: "You are logged in with the institution {{institution}} (",
        changeThis: "change this",
        tooltipApps: "You have been granted access to applications as a guest user by your institution"
    },
    inviteOnly: {
        welcome: "Welcome to SURFconext Invite",
        roles: "You don't have any roles.",
        info: "The SURFconext Invite application is by invite only. If you want to enter, but don't have access, please contact <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a>.",
        preLogin: "Or ",
        login: "login",
        postLogin: " again with a different institution",
    },
    missingAttributes: {
        welcome: "Welcome to SURFconext Invite",
        attributes: "Your institution has not provided all required attributes. The following attributes are missing:",
        info: "If you want more information, please contact <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a>.",
        preLogin: "Or ",
        login: "login",
        postLogin: " again with a different institution.",
    },
    invitationAccept: {
        hi: "Hi{{name}},",
        nextStep: "Next: enjoy your new role",
        expired: "This invitation has expired at {{expiryDate}}",
        expiredInfo: "Please contact the person that invited you and ask this person to send you a new invite.",
        enforceEmailEquality: " This invite can only be accepted by <strong>{{email}}</strong>.",
        invited: "You have been invited for the {{plural}} {{roles}} by {{inviter}}.",
        role: "role",
        roles: "roles",
        progress: "1",
        info: "SURFconext Invite provides access to application based on your roles.",
        infoLogin: "You can login with your institution account or eduID.",
        infoLoginEduIDOnly: "You must login with eduID.",
        infoLoginAgain: "To accept the invitation you'll need to login again.",
        login: "Login",
        loginWithSub: "Login",
        access: "Access granted",
        applicationInfo: "This application has been added to your SURF-invite homepage.",
        applicationInfoMultiple: "These applications have been added to your SURF-invite homepage.",
        continue: "Continue",
        emailMismatch: "The inviter has indicated that you must accept this invitation with the email {{email}}, " +
            "but you have logged in with an account with a different email. Please login in with a different account.",
        inviteRedeemUrl: "Your new role requires a microsoft account. Please press Continue to register one."
    },
    proceed: {
        info: "Congrats! You have accepted the {{plural}} {{roles}} and you now can go to the application",
        progress: "2",
        goto: "Visit application",
        nextStep: "Next: go to the application",
        launch: "Launch",
        new: "New"
    },
    tooltips: {
    },
    confirmationDialog: {
        title: "Confirm",
        error: "Error",
        subTitle: "This action requires a confirmation",
        subTitleError: "An error has occurred",
        confirm: "Confirm",
        ok: "OK",
        cancel: "Cancel",
    },
    footer: {
        terms: "Terms of Use",
        termsLink: "https://support.surfconext.nl/terms-en",
        privacy: "Privacy policy",
        privacyLink: "https://support.surfconext.nl/privacy-en",
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
    },
    notFound: {
        alt: "404 Page not found"
    }
}

export default en;
