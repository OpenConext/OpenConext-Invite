const pt = {
    code: "PT",
    name: "Português",
    select_locale: "Mudar idioma para Português",
    landing: {
        header: {
            title: "Aceda às suas aplicações",
            login: "Iniciar sessão",
            sup: "SURFconext Invite é apenas por convite.",
        },
        works: "Como funciona?",
        adminFunction: "função de administrador",
        info: [
            ["Convites", "<p>A SURF convida gestores institucionais que podem criar funções para as suas aplicações.</p>" +
            "<p>As aplicações são serviços conectados ao SURFconext.</p>", true],
            ["Funções", "<p>Os gestores de aplicações convidarão colegas para funções que, por sua vez, podem convidar convidados.</p>", true],
            ["Participar", "<p>Os colegas convidados que aceitarem o convite recebem direitos de acesso às aplicações.</p><br/>", false],
            ["Grupos", "<p>As funções são, na verdade, membros de grupos que podem ser usados em regras do SURFconext, ou provisionados como atributos ou para APIs SCIM externas.</p>", false]
        ],
        footer: "<p>O SURFconext Invite é um serviço de gestão de acessos para investigação e educação.</p>" +
            "<p>Quer saber mais? <a href='https://support.surfconext.nl/invite-en'>Leia mais</a>.</p>",
    },
    header: {
        title: "SURFconext Invite",
        subTitle: "Tudo ficará bem",
        links: {
            login: "Iniciar sessão",
            switchApp: "Ir para {{app}}",
            welcome: "Bem-vindo",
            access: "Acesso",
            help: "Ajuda",
            profile: "Perfil",
            logout: "Terminar sessão"
        },
    },
    home: {
        access: "SURFconext Invite",
    },
    users: {
        roles: "Aplicações",
        noRolesInfo: "Não tem aplicações (o que significa que deve ser super-utilizador)",
        noRolesFound: "Não foi convidado para nenhum papel de convidado para aplicações educacionais",
        rolesInfo: "Tem acesso às seguintes aplicações.",
        expiryDays: "Dias de expiração"
    },
    roles: {
        multiple: "Múltiplas aplicações",
    },
    forms: {
        ok: "Ok",
        and: "e",
        more: "Mais",
        less: "Menos",
        error: "Pode <a href=\"mailto:support@surfconext.nl\">contactar o SURFconext Invite</a> para mais informações.<br/><br/>" +
            "O número de referência para esta exceção é {{reference}}."
    },
    profile: {
        welcome: "Bem-vindo, {{name}}",
        info: "Aqui estão as aplicações educacionais às quais pode aceder através do SURFconext Invite",
        toaster: "Iniciou sessão com a instituição {{institution}} (",
        changeThis: "alterar isto",
        tooltipApps: "Foi concedido acesso a aplicações como utilizador convidado pela sua instituição"
    },
    inviteOnly: {
        welcome: "Bem-vindo ao SURFconext Invite",
        roles: "Não tem nenhuma função.",
        info: "A aplicação SURFconext Invite é apenas por convite. Se quiser entrar, mas não tem acesso, por favor contacte <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a>.",
        preLogin: "Ou ",
        login: "inicie sessão",
        postLogin: " novamente com uma instituição diferente",
    },
    missingAttributes: {
        welcome: "Bem-vindo ao SURFconext Invite",
        attributes: "A sua instituição não forneceu todos os dados pessoais necessários. Os seguintes estão em falta:",
        info: "Se quiser mais informações, por favor contacte <a href='mailto:support@surfconext.nl'>support@surfconext.nl</a>.",
        preLogin: "Ou ",
        login: "inicie sessão",
        postLogin: " novamente com uma instituição diferente.",
        sub: "sub",
        email: "email",
        givenName: "nome próprio",
        familyName: "apelido",
        schacHomeOrganization: "organização de origem"
    },
    invitationAccept: {
        hi: "Olá{{name}},",
        nextStep: "Próximo: aproveite a sua nova função",
        expired: "Este convite expirou em {{expiryDate}}",
        expiredInfo: "Por favor contacte a pessoa que o convidou e peça-lhe para enviar um novo convite.",
        enforceEmailEquality: " Este convite só pode ser aceite por <strong>{{email}}</strong>.",
        invited: "Foi convidado para o {{plural}} {{roles}} por {{inviter}}.",
        role: "função",
        roles: "funções",
        progress: "1",
        info: "O SURFconext Invite fornece acesso à aplicação com base nas suas funções.",
        infoLogin: "Pode iniciar sessão com a conta da sua instituição ou eduID.",
        infoLoginEduIDOnly: "Deve iniciar sessão com eduID.",
        infoLoginAgain: "Para aceitar o convite, precisará de iniciar sessão novamente.",
        login: "Iniciar sessão",
        loginWithSub: "Iniciar sessão",
        access: "Acesso concedido",
        applicationInfo: "Esta aplicação foi adicionada à sua página inicial do SURFconext Invite.",
        applicationInfoMultiple: "Estas aplicações foram adicionadas à sua página inicial do SURFconext Invite.",
        continue: "Continuar",
        emailMismatch: "O convidador indicou que deve aceitar este convite com o email {{email}}, " +
            "mas iniciou sessão com uma conta com um email diferente. Por favor, inicie sessão com uma conta diferente.",
        inviteRedeemUrl: "A sua nova função requer uma conta Microsoft. Por favor, pressione Continuar para registar uma.",
        graphEmailViolation: "A sua nova função requer uma conta Microsoft, no entanto, a Microsoft não suporta o seu email. Não podemos criar uma conta Microsoft.",
        userWaitTime: "Antes de poder usar a sua nova aplicação, a sua conta para {{role}} precisa primeiro de ser criada. Receberá um email quando isto estiver concluído (estimativa {{waitTime}})."
    },
    proceed: {
        info: "Parabéns! Aceitou o {{plural}} {{roles}} e agora pode ir para a aplicação",
        progress: "2",
        goto: "Visitar aplicação",
        nextStep: "Próximo: ir para a aplicação",
        launch: "Lançar",
        new: "Novo"
    },
    tooltips: {
    },
    confirmationDialog: {
        title: "Confirmar",
        error: "Erro",
        subTitle: "Esta ação requer uma confirmação",
        subTitleError: "Ocorreu um erro",
        confirm: "Confirmar",
        ok: "OK",
        cancel: "Cancelar",
    },
    footer: {
        terms: "Termos de Uso",
        termsLink: "https://support.surfconext.nl/terms-en",
        privacy: "Política de Privacidade",
        privacyLink: "https://support.surfconext.nl/privacy-en",
        surfLink: "https://surf.nl",
    },
    expirations: {
        expires: "Expira {{relativeTime}}",
        expired: "Expirou {{relativeTime}}",
        never: "Nunca expira",
        activity: {
            now: "Agora mesmo",
            seconds: "Hoje",
            minute: "Hoje",
            minutes: "Hoje",
            hour: "Hoje",
            hours: "Hoje",
            day: "Ontem",
            days: "Esta semana",
            week: "Esta semana",
            weeks: "Este mês",
            month: "Mês passado",
            months: "%s meses atrás",
            year: "1 ano atrás",
            years: "%s anos atrás"
        },
        ago: {
            now: "agora mesmo",
            seconds: "há %s segundos",
            minute: "há 1 minuto",
            minutes: "há %s minutos",
            hour: "há 1 hora",
            hours: "há %s horas",
            day: "há 1 dia",
            days: "há %s dias",
            week: "há 1 semana",
            weeks: "há %s semanas",
            month: "há 1 mês",
            months: "há %s meses",
            year: "há 1 ano",
            years: "há %s anos"
        },
        in: {
            now: "agora mesmo",
            seconds: "em %s segundos",
            minute: "em 1 minuto",
            minutes: "em %s minutos",
            hour: "em 1 hora",
            hours: "em %s horas",
            day: "em 1 dia",
            days: "em %s dias",
            week: "em 1 semana",
            weeks: "em %s semanas",
            month: "em 1 mês",
            months: "em %s meses",
            year: "em 1 ano",
            years: "em %s anos"
        }
    },
    notFound: {
        alt: "404 Página não encontrada"
    }
}

export default pt;