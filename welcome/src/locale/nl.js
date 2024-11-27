const nl = {
    code: "NL",
    name: "Nederlands",
    select_locale: "Wissel taal naar Engels",
    landing: {
        header: {
            title: "Toegang tot je applicaties",
            login: "Inloggen",
            sup: "SURFconext Invite is alleen op uitnodiging.",
        },
        works: "Hoe werkt het?",
        adminFunction: "adminfunctie",
        info: [
            //Arrays of titles and info blocks and if a function is an admin function
            ["Uitnodigen", "<p>Instellingsbeheerders kunnen rollen aanmaken voor hun applicaties.</p>" +
            "<p>Applicaties zijn diensten gekoppeld aan SURFconext.</p>", true],
            ["Rollen", "<p>De applicatiebeheerders nodigen collega's uit die op hun beurt weer gasten kunnen uitnodigen.</p>", true],
            ["Word lid", "<p>Uitgenodigde collega's die op de uitnodiging zijn ingegaan krijgen toegang tot applicaties.</p><br/>", false],
            ["Groepen", "<p>De rollen zijn feitelijk groepslidmaatschappen die gebruikt kunnen worden in SURFconext autorisatieregels, doorgegeven als attributen of via externe SCIM API's.</p>", false]
        ],
        footer: "<p>SURFconext Invite biedt toegangsbeheer voor het Nederlandse onderwijs en onderzoek.</p>" +
            "<p>Meer weten? <a href='https://support.surfconext.nl/invite'>Lees verder</a>.</p>",
    },
    header: {
        title: "SURFconext Invite",
        subTitle: "Alles gaat uilstekend",
        links: {
            login: "Inloggen",
            switchApp: "Ga naar {{app}}",
            welcome: "Welcome",
            access: "Invite",
            help: "Help",
            profile: "Profiel",
            logout: "Log uit"
        },
    },
    home: {
        access: "SURFconext Invite",
    },
    users: {
        roles: "Applicaties",
        noRolesInfo: "Je hebt geen applicaties (je bent dus een super-user)",
        noRolesFound: "Je bent nog niet als gast uitgenodigd voor een rol.",
        rolesInfo: "Je bezit de volgende rollen.",
        expiryDays: "Verloopdagen"
    },
    roles: {
        multiple: "Meedere applicaties",
    },
    forms: {
        ok: "Ok",
        and: "en",
        more: "Meer",
        less: "Minder",
        error: "Je kunt <a href=\"mailto:support@surfconext.nl\">contact opnemen</a> voor meer informatie.<br/><br/>" +
            "Het foutmeldingsnummer is {{reference}}."
    },
    profile: {
        welcome: "Welkom, {{name}}",
        info: "Hier zijn de applicaties waar je via SURFconext Invite toegang toe hebt.",
        toaster: "Je bent ingelogd via {{institution}} (",
        changeThis: "wijzig",
        tooltipApps: "Je hebt toegang gekregen tot deze applicaties via een uitnodiging"
    },
    inviteOnly: {
        welcome: "Welkom bij SURFconext Invite",
        roles: "Je hebt nog geen rollen.",
        info: "Deze applicatie is op uitnodiging. Heb je hierover vragen? Neem <a href='mailto:support@surfconext.nl'>contact met ons op</a>.",
        preLogin: "Of ",
        login: "log in",
        postLogin: " via een andere instelling",
    },
    missingAttributes: {
        welcome: "Welkom bij SURFconext Invite",
        attributes: "Je instelling gaf niet alle benodigde persoonsgegevens vrij. Het volgende ontbreekt:",
        info: "Hulp nodig? Neem <a href='mailto:support@surfconext.nl'>contact met ons op</a>.",
        preLogin: "Of ",
        login: "log in",
        postLogin: " via een andere instelling",
        sub: "sub",
        email: "email",
        givenName: "voornaam",
        familyName: "achternaam",
        schacHomeOrganization: "schacHomeOrganisatie"
    },
    invitationAccept: {
        hi: "Hoi{{name}},",
        nextStep: "Volgende: veel plezier met je nieuwe rol",
        expired: "Deze uitnodiging is verlopen op {{expiryDate}}",
        expiredInfo: "Neem contact op met degene die je uitnodigde en vraag hen je een nieuwe uitnodiging te sturen.",
        enforceEmailEquality: " Deze uitnodiging kan alleen geaccepteerd worden door <strong>{{email}}</strong>.",
        invited: "Uitgenodigd voor {{plural}} {{roles}} door {{inviter}}.",
        role: "rol",
        roles: "rollen",
        progress: "1",
        info: "SURFconext Invite geeft toegang tot applicaties op basis van je rol.",
        infoLogin: "Je kunt inloggen met je instellings-account of eduID.",
        infoLoginEduIDOnly: "Je moet inloggen met eduID.",
        infoLoginAgain: "Om de uitnodiging te accepteren moet je opnieuw inloggen.",
        login: "Log in",
        loginWithSub: "Log in",
        access: "Toegang verleend",
        applicationInfo: "Deze applicatie is toegevoegd aan je SURFconext Invite startpagina.",
        applicationInfoMultiple:"Deze applicaties zijn toegevoegd aan je SURFconext Invite startpagina.",
        continue: "Doorgaan",
        emailMismatch: "De uitnodiger heeft aangegeven dat je de uitnodiging dient te accepteren met e-mailadres {{email}}, " +
            "maar je bent ingelogd met een account met een ander mailadres. Log opnieuw in met een ander account.",
        inviteRedeemUrl: "Je nieuwe rol vereist een Microsoft account. Druk op Doorgaan om deze te registreren.",
        graphEmailViolation: "Je nieuwe rol vereist een Microsoft account, alleen microsoft ondersteunt je e-mail niet. Er wordt geen MS account voor je aangemaakt.",
        // userWaitTime: "Je nieuwe rol {{role}} vereist externe provisioning. Je ontvangt een e-mail als dit is afgerond (inschatting ~{{waitTime}}).",
        userWaitTime: "Voordat je je nieuwe applicatie(s) kan gebruiken, moet je account voor {{role}} eerst worden gemaakt. Je ontvangt een e-mail als dit is gedaan (verwacht {{waitTime}})."

    },
    proceed: {
        info: "Gefeliciteerd! Je hebt de {{plural}} {{roles}} geaccepteerd en kunt nu verder naar de applicatie",
        progress: "2",
        goto: "Open applicatie",
        nextStep: "Volgende: ga naar de applicatie",
        launch: "Open",
        new: "Nieuw"
    },
    tooltips: {
    },
    confirmationDialog: {
        title: "Bevestig",
        error: "Fout",
        subTitle: "Deze actie vereist bevestiging",
        subTitleError: "Er is een fout opgetreden",
        confirm: "Bevestig",
        ok: "OK",
        cancel: "Annuleer",
    },
    footer: {
        terms: "Gebruiksvoorwaarden",
        termsLink: "https://support.surfconext.nl/terms-nl",
        privacy: "Privacybeleid",
        privacyLink: "https://support.surfconext.nl/privacy-nl",
        surfLink: "https://surf.nl",
    },
    expirations: {
        expires: "Verloopt {{relativeTime}}",
        expired: "Verlopen {{relativeTime}}",
        never: "Verloopt niet",
        activity: {
            now: "Zojuist",
            seconds: "Vandaag",
            minute: "Vandaag",
            minutes: "Vandaag",
            hour: "Vandaag",
            hours: "Vandaag",
            day: "Gisteren",
            days: "Deze week",
            week: "Deze week",
            weeks: "Deze maand",
            month: "Vorige maand",
            months: "%s maanden geleden",
            year: "1 jaar geleden",
            years: "%s jaar geleden"
        },
        ago: {
            now: "zojuist",
            seconds: "%s seconden geleden",
            minute: "1 minuut geleden",
            minutes: "%s minuten geleden",
            hour: "1 uur geleden",
            hours: "%s uren geleden",
            day: "1 dag geleden",
            days: "%s dagen geleden",
            week: "1 week geleden",
            weeks: "%s weken geleden",
            month: "1 maand geleden",
            months: "%s maanden geleden",
            year: "1 jaar geleden",
            years: "%s jaren geleden"
        },
        in: {
            now: "zojuist",
            seconds: "over %s seconden",
            minute: "over 1 minuut",
            minutes: "over %s minuten",
            hour: "over 1 uur",
            hours: "over %s uur",
            day: "over 1 dag",
            days: "over %s dagen",
            week: "over 1 week",
            weeks: "over %s weken",
            month: "over 1 maand",
            months: "over %s maanden",
            year: "over 1 jaar",
            years: "over %s jaar"
        }
    },
    notFound: {
        alt: "404 Pagina niet gevonden"
    }
}

export default nl;
