const en = {
    code: "EN",
    name: "English",
    select_locale: "Change language to English",
    languages: {
        language: "Language",
        languageTooltip: "Choose the language of the invitation mail",
        en: "English",
        nl: "Dutch",
    },
    landing: {
        header: {
            title: "Manage access to your applications",
            login: "Log in",
            sup: "SURFconext Invite is by invitation only.",
        },
        works: "How does it work?",
        adminFunction: "admin function",
        info: [
            //Arrays of titles and info blocks and if a function is an admin function
            ["Invites", "<p>SURF invites institution admins who can create roles for their applications.</p>" +
            "<p>The application list consists of applications connected to SURFconext.</p>", true],
            ["Roles", "<p>The role managers will invite colleagues for roles who can in turn invite users.</p>", true],
            ["Join", "<p>Invited colleagues who accept the invitation are granted access rights for the applications.</p><br/>", false],
            ["Groups", "<p>The roles are actually group memberships that can be used in SURFconext authorisation rules, or provisioned as attributes or to external SCIM APIs.</p>", false]
        ],
        footer: "<p>SURFconext Invite offers access management to SURFconext-connected applications.</p>" +
            "<p>Do you want to know more? <a href='https://support.surfconext.nl/invite-en'>Read more</a>.</p>",
    },
    header: {
        title: "SURFconext Invite",
        subTitle: "Everything will be owl right",
        links: {
            login: "Log in",
            system: "System",
            switchApp: "Go to {{app}}",
            welcome: "Welcome",
            access: "Invite",
            help: "Help",
            profile: "Profile",
            logout: "Log out"
        },
    },
    tabs: {
        home: "Home",
        applications: "Applications",
        users: "Users",
        applicationUsers: "Users",
        maintainers: "Role managers & inviters",
        guests: "User with this role",
        invitations: "Invitations",
        roles: "Access roles",
        profile: "Profile",
        userRoles: "Role managers & inviters",
        guestRoles: "Users with this role",
        cron: "Cron",
        invite: "Invite",
        tokens: "API tokens",
        unknownRoles: "Missing applications",
        expiredUserRoles: "User role expirations",
        pendingInvitations: "Pending",
        allPendingInvitations: "Pending invitations",
        acceptedInvitations: "Accepted",
        performanceSeed: "Seed",
        seed: "Seed"
    },
    home: {
        access: "SURFconext Invite",
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
        SUPER_USER: "Superuser",
        INSTITUTION_ADMIN: "Institution admin",
        MANAGER: "Role manager",
        INVITER: "Inviter",
        GUEST: "User",
        "No member": "No member"
    },
    users: {
        found: "{{count}} {{plural}} found",
        moreResults: "There are more results than shown, please refine your search.",
        applicationsSearchPlaceHolder: "Search for application...",
        name_email: "Name / email",
        name: "Name",
        email: "Email",
        highestAuthority: "Role",
        createdAt: "Created",
        schacHomeOrganization: "Institution",
        lastActivity: "Last activity",
        organizationGUID: "Organization GUID",
        eduPersonPrincipalName: "EPPN",
        sub: "Sub",
        singleUser: "user",
        multipleUsers: "users",
        noEntities: "No users found",
        new: "New invitation",
        title: "Users",
        roles: "Roles",
        applications: "Applications",
        noRolesInfo: "You have no roles (which means you must be super-user)",
        noRolesInstitutionAdmin: "You are an institution admin and you have no roles (but you might have access to applications)",
        noRolesNoApplicationsInstitutionAdmin: "You are an institution admin, but you have no roles and apparently your institution has also no access to applications",
        guestRoleOnly: "You are not an administrator. Are you looking for <a href='{{welcomeUrl}}'>the apps you can access</a>?",
        rolesInfo: "You have the following roles",
        applicationsInfo: "You have access to the following applications",
        noRolesFound: "No roles are found.",
        noApplicationsFound: "No applications are found.",
        rolesInfoOther: "{{name}} has the following roles",
        applicationsInfoOther: "{{name}} has access to the following applications",
        landingPage: "Landing page",
        access: "Access",
        organisation: "Organisation",
        noResults: "No users are found",
        searchPlaceHolder: "Search for users...",
        authority: "Authority",
        endDate: "End date",
        expiryDays: "Expiry days",
        roleExpiryTooltip: "Sort on roles to see which roles will expire the soonest"
    },
    role: {
        copyUrn: "Copy urn",
        userInfo: "{{nbr}} member(s) & valid for {{valid}} days",
        roleInfo: "Role valid for <strong>{{days}} days</strong>",
        roleInfoNoEndDate: "Role has <strong>no end date</strong>",
        contactAdmin: "Contact role manager(s)"
    },
    roles: {
        title: "Access Roles",
        applicationName: "Application",
        auditable: "Role <span>{{name}}</span> was created by <span>{{createdBy}}</span> at {{createdAt}}",
        roleDetails: "Role details",
        invitationDetails: "Invitation details",
        applicationDetails: "Application(s) this role applies to",
        addApplication: "Add application",
        multiple: "Multiple applications",
        applicationPlaceholder: "Choose an application...",
        accessRole: "Name",
        name: "Name",
        namePlaceHolder: "The name of the role",
        shortName: "Short name",
        organizationGUID: "Organization GUID",
        identityProvider: "Identity provider: {{name}}",
        landingPage: "(Custom) landing page",
        userRoleCount: "# Users",
        landingPagePlaceHolder: "https://landingpage.com",
        defaultExpiryDays: "Expiry days",
        endDate: "End date",
        noEndDate: "-",
        authority: "Authority",
        yourRole: "Your role",
        description: "Description",
        descriptionPlaceHolder: "The description of the role",
        noResults: "No roles are found",
        noMember: "No member",
        searchPlaceHolder: "Search for roles...",
        found: "{{count}} {{plural}} found",
        singleRole: "role",
        multipleRoles: "roles",
        new: "Add new role",
        edit: "Edit role {{name}}",
        urn: "URN",
        advanced: "Advanced settings",
        showAdvancedSettings: "Show advanced invite settings",
        hideAdvancedSettings: "Hide advanced invite settings",
        override: "Override of settings allowed?",
        manage: "Application",
        manageMetaData: "SURFconext entity",
        provisioning: "Provisioning",
        deleteFlash: "Role {{name}} has been deleted",
        deleteConfirmation: "Are you sure you want to delete this role?",
        createFlash: "Role {{name}} has been created",
        updateFlash: "Role {{name}} has been updated",
        unknownInManage: "Unknown in Manage",
        unknownInManageToolTip: "The application for this role has been removed from the SURF backend. Please contact <a href=\"mailto:support@surfconext.nl\">support@surfconext.nl</a> to resolve this.",
        unknownInManageDisabled: "The application for this role has been removed from the SURF backend. Therefore, you can't invite new users. Contact <a href=\"mailto:support@surfconext.nl\">support@surfconext.nl</a> to resolve this.",
        consequences: {
            info: "The following users will lose their access:",
            userInfo: "{{name}} ({{authority}}), last activity {{lastActivity}}",
            andMore: "And {{nbr}} more.. Check the list of current users for more details."
        }
    },
    applications: {
        title: "Access Roles for this application ({{nbr}})",
        applicationFound: "Applications ({{nbr}})",
        new: "New Access Role",
        searchPlaceHolder: "Search for applications",
        noResults: "No applications found...",
        name: "Application name",
        types: {
            saml20_sp: "Service Provider",
            oidc10_rp: "Relying Party"
        },
        type: "Type",
        organization: "Organization",
        url: "URL",
        roles: "Roles",
        provisionings: "Provisionings",
        accessRole: "Access Role",
    },
    applicationRoles: {
        searchPlaceHolder: "Search for Access Roles",
        noEntities: "No access roles found",
    },
    userRoles: {
        found: "{{count}} {{plural}} found",
        singleUserRole: "user role",
        multipleUserRoles: "user roles",
        searchPlaceHolder: "Search for users...",
        noResults: "No user roles where found",
        guestRoles: "{{count}} users",
        managerRoles: "{{count}} managers & inviters",
        notAllowed: "You're not allowed to delete this user role because of missing roles",
        updateConfirmation: "Are you sure you want to change the end date of role {{roleName}} for {{userName}}?",
        updateConfirmationRemoveEndDate: "Are you sure you want to remove the end date of role {{roleName}} for {{userName}}?",
        updateFlash: "The end date for role {{roleName}} has been updated",
        deleteConfirmation: "Are you sure  you want to remove this role from this user(s)?",
        deleteOneConfirmation: "Are you sure  you want to remove this role from this user?",
        deleteFlash: "User role(s) have been removed",
        createdAt: "Date accepted",
        delete: "Remove"
    },
    invitations: {
        title: "Invitations",
        searchPlaceHolder: "Search for invitation...",
        noResults: "No invitation where found",
        inviter: "Invited by",
        status: "Status",
        pending: "pending",
        open: "Open",
        accepted: "Accepted",
        expired: "Expired",
        enforceEmailEquality: "Email equality",
        eduIDOnly: "eduID only",
        new: "Invite manager or inviter",
        newInvitation: "Invite inviter",
        newInvite: "New invite",
        newGuest: "Invite new user",
        invitees: "Invitees",
        intendedRoles: "Roles",
        inviteesPlaceholder: "Invitee email addresses",
        requiredEmail: "At least one email address is required",
        requiredRole: "At least one role is required for an invitation",
        requiredOrganizationGUID: "A valid Organization GUID is required for an institution admin invitation",
        intendedAuthority: "Authority",
        roles: "Roles",
        inviterRoles: "Select the roles for the new invitation",
        rolesPlaceHolder: "Choose one or more roles",
        expiryDate: "Valid till",
        acceptedAt: "Date accepted",
        roleExpiryDate: "Role expiry date",
        roleExpiryDateQuestion: "Set a custom role expiration period",
        roleExpiryDateInfo: "This role will be removed from the user {{expiry}}",
        customInviterDisplayNameQuestion: "Set a custom inviter name",
        inviterDisplayName: "Custom inviter display name for invitations",
        inviterDisplayNamePlaceholder: "e.g. working@home.university.nl",
        inviterDisplayNameError: "Custom inviter name is required, unless you use the default inviter name",
        customInviterDisplayNameInfoDefault: "Invitations for this role will show the name of the inviter as sender",
        customInviterDisplayNameInfo: "Invitations for this role will show a custom name / email as sender",
        expiryDateQuestion: "Set a custom invitation expiration period",
        expiryDateInfo: "Default an invitation is valid for 1 month",
        withinThreeMonths: "Within 1 month",
        createdAt: "Sent",
        message: "Personal note",
        messagePlaceholder: "Add an optional personal note to your invitation",
        invite: "Send invite",
        guestRoleIncluded: "Add the user role?",
        invalidEmails: "Invalid email addresses removed: {{emails}}.",
        createFlash: "Invitation was sent",
        delete: "Revoke",
        resend: "Resend",
        notAllowed: "You're not allowed to delete or resend this invitation because of missing roles",
        deleteFlash: "Invitation(s) have been revoke",
        deleteConfirmation: "Are you sure you want to revoke these invitations?",
        deleteOneConfirmation: "Are you sure you want to revoke this invitation?",
        resendConfirmation: "Are you sure you want to resend these invitations?",
        resendConfirmationOne: "Are you sure you want to resend this invitation?",
        resendFlash: "Invitation(s) have been resent",
        inviterRole: {
            title: "Send new invite",
            roles: "For the following roles",
            to: "To",
            message: "Personal note",
            settings: "Advanced invite settings"
        },
        statuses: {
            all: "All ({{nbr}})",
            open: "Open",
            accepted: "Accepted",
            expired: "Expired",
            mine: "Mine"
        }
    },
    forms: {
        none: "None",
        notApplicable: "N/A",
        you: "You",
        yes: "Yes",
        no: "No",
        ok: "OK",
        or: "or ",
        edit: "Edit",
        cancel: "Cancel",
        save: "Save",
        specificDate: "Set specific date",
        and: "and",
        more: "More",
        less: "Less",
        alreadyExists: "The {{attribute}} '{{value}}' already exists",
        alreadyExistsParent: "The {{attribute}} {{value}} already exists within {{parent}}",
        required: "{{attribute}} is required",
        invalid: "The value '{{value}}' is invalid for {{attribute}}",
        error: "You can contact <a href=\"mailto:support@surfconext.nl?subject=Error SURF Invite reference {{reference}}\">support@surfconext.nl</a> for help.<br/><br/>" +
            "The reference number for this exception is {{reference}}."
    },
    profile: {
        info: "The account of {{name}} was created on {{createdAt}}",
        your: "Your account was created on {{createdAt}}"
    },
    inviteOnly: {
        welcome: "Welcome to SURFconext Invite",
        roles: "You don't have any roles.",
        info: "SURFconext Invite is by invitation only. Please contact <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a> with questions.",
        preLogin: "Or ",
        login: "log in",
        postLogin: " again with a different institution",
    },
    missingAttributes: {
        welcome: "Welcome to SURFconext Invite",
        attributes: "Your institution has not provided all required personal data. The following are missing:",
        info: "If you want more information, please contact <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a>.",
        preLogin: "Or ",
        login: "login",
        postLogin: " again with a different institution.",
        sub: "sub",
        email: "email",
        givenName: "givenName",
        familyName: "familyName",
        schacHomeOrganization: "schacHomeOrganization"
    },
    invitationAccept: {
        hi: "Hi{{name}},",
        nextStep: "Next: enjoy your new role",
        expired: "This invitation has expired at {{expiryDate}}",
        expiredInfo: "Please contact <a href='mailto:{{email}}'>{{email}}</a> and ask this person to send you a new invite.",
        invited: "You have been invited to become {{authority}} for the {{plural}} {{roles}} by {{inviter}}.",
        invitedNoRoles: "You have been invited to become {{authority}} by {{inviter}}",
        enforceEmailEquality: " This invite can only be accepted by <strong>{{email}}</strong>.",
        role: "role",
        roles: "roles",
        progress: "1",
        info: "SURFconext Invite provides access to application based on your roles.",
        infoLogin: "You can log in with your institution account or eduID.",
        infoLoginEduIDOnly: "You need to log in with eduID.",
        infoLoginAgain: "To accept the invitation you'll need to log in again.",
        login: "Log in",
        loginWithSub: "Log in",
        emailMismatch: "The inviter has indicated that you must accept this invitation with email address {{email}}, " +
            "but you have logged in with an account with a different email address. Please log in in with a different account."
    },
    inviter: {
        welcome: "Welcome, {{name}}",
        info: "Manage who gets access to the <strong>educational applications</strong> at <strong>your institution</strong>.",
        sendInvite: "Send new invite",
        viewHistory: "view history",
        manage: "You can manage users and send invites for",
        details: "Show details",
        history: "Invitation history"
    },
    institutionAdmin: {
        welcome: "Welcome institution administrator of {{name}}! You can start with creating your first role and subsequently invite role managers.",
        create: "Create access role"
    },
    tokens: {
        title: "API tokens",
        new: "Add API token",
        searchPlaceHolder: "Search for API tokens...",
        noEntities: "No API tokens",
        titleNew: "Create an API token for {{institutions}}",
        backToOverview: "Back to all API tokens",
        createdAt: "Created at",
        secretDisclaimer: "You can view this API token only once. Copy it and store it somewhere safe.<br><br>If the token is lost, delete it and create a new one.",
        secret: "API token",
        secretValue: "One-way hashed token",
        secretTooltip: "The value to use in the X-API-TOKEN header",
        description: "Description",
        superUserToken: "Super User token",
        organizationGUID: "Organization GUID",
        descriptionPlaceHolder: "Description for this API token",
        descriptionTooltip: "A description explaining the use of this API token",
        deleteFlash: "API token has been deleted",
        deleteConfirmation: "Are you sure you want to delete this API token?",
        createFlash: "API token has been created",
        submit: "Submit",
        required: "The description is required for an API token",
    },
    tooltips: {
        userIcon: "User {{name}} provisioned at {{createdAt}} with last activity on {{lastActivity}}",
        impersonateIcon: "Impersonate user {{name}}",
        roleIcon: "Role {{name}} created at {{createdAt}}",
        userRoleIcon: "User role accepted by {{name}} at {{createdAt}}",
        invitationIcon: "Invitation for {{email}} sent at {{createdAt}} with expiration date {{expiryDate}}",
        roleShortName: "The unique short name of the role within a provisioning. It is used to format the urn and therefore not all characters are allowed.",
        organizationGUID: "The Manage organizational identifier to scope the visibility of roles of the institution admin. Only specify a value if you are creating or editing this role on behalf of an institution admin",
        roleUrn: "The urn of the role. It is based on the sanitized name and the role identifier. It is used as the unique global identifier of this role and therefore not all characters are allowed.",
        manageService: "The required application from SURFconext, with may have an optional provisioning",
        defaultExpiryDays: "The default number of days the role will expire, from the moment a user has accepted the invitation for this role",
        enforceEmailEqualityTooltip: "When checked the invitee must accept the invitation with an account with the email address where the invitation was sent to",
        eduIDOnlyTooltip: "When checked the invitees will be required to log in with eduID",
        roleExpiryDateTooltip: "The end date of this role. After this date the role is removed from the user.",
        expiryDateTooltip: "The date on which this invitation expires",
        inviterDisplayName: "The functional address which will used in the invitations of the role.<br><br>Default the name of the inviter is show.",
        rolesTooltip: "Select all the roles that the invitee will be granted after accepting the invitation",
        intendedAuthorityTooltip: "The authority determines the rights the invitee will be granted on accepting the invitation",
        inviteesTooltip: "Add email addresses separated by comma, space or semi-colon or on seperate lines. You can also paste a csv file with line-separated email addresses.",
        removeInvitation: "Delete all selected invitations",
        removeOneInvitation: "Delete this invitation",
        resendInvitation: "Resend all selected invitations",
        resendOneInvitation: "Resend this invitation",
        inviter: "Send invitations to persons who will - once accepted - gain user access to the application",
        overrideSettingsAllowed: "When checked, invitations for this role can override the advanced settings (e.g. email equality, eduID only and the role expiry end date)",
        removeUserRole: "Remove all selected user roles",
        removeOneUserRole: "Remove this user role",
        guestRoleIncludedTooltip: "Do you also want to grant the invitees the user role when they accept the invitation?",
        expiredUserRole: "This role will expire soon",
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
            seconds: "after %s seconds",
            minute: "after 1 minute",
            minutes: "after %s minutes",
            hour: "after 1 hour",
            hours: "after %s hours",
            day: "after 1 day",
            days: "after %s days",
            week: "after 1 week",
            weeks: "after %s weeks",
            month: "after 1 month",
            months: "after %s months",
            year: "after 1 year",
            years: "after %s years"
        }
    },
    notFound: {
        alt: "404 Page not found"
    },
    system: {
        trigger: "Trigger",
        clear: "Clear",
        cronInfo: "Trigger the cron job to cleanup resources like expired user-roles, orphaned users and in-active users",
        cronNotificationsInfo: "Trigger the cron job to send notification mails for user-roles that will expire in X days",
        noMails: "No notification mails for user-role expirations were send",
        performanceSeedInfo: "The performance seed is dummy / generated data to test the performance of the system. It is deleted when you run the tests.",
        performanceSeed: "Insert performance seed",
        performanceLoadTime: "Inserted following entities in ~{{minutes}} minutes"
    },
    unknownRoles: {
        title: "Roles linked to applications unknown in Manage",
        searchPlaceHolder: "Search...",
        noRoles: "Yeah, no unknown manage applications"
    },
    expiredUserRoles: {
        title: "Roles to be expired the next month",
        searchPlaceHolder: "Zoek...",
        noResults: "Yeah, no user-roles to be expired within one month"
    }
}

export default en;
