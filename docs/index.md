# Openconext-Invite

## Intro

This provides an application to manage invite-roles for external (and
internal) users. A user is invited to join a role by email. Once the invite is
accepted, the new user roles can be sent to external systems by
[SCIM](https://datatracker.ietf.org/doc/html/rfc7643#section-4.1), graph API
or email (ticketing system). Also a
[VOOT](https://wiki.geant.org/display/gn3pjra3/VOOT+specifications) provider is
available for publishing the users' memberships, for usage in SURFconext PDP
rules or managing invite in the application.

The application uses two different frontends. The first (inviter) is for
managing roles and invites. The other web interface is for the guests, used
for accepting invites and providing a overview of roles and links to the
applications.

It's also possible to use an API for connection directly to your own IdM system
or Selfservice Portal.

## Documentation

- [API documentation](./api/)
- [SCIM Protocol description](./SCIM/)
- [Archimate Model](./Archi/?view=id-942fd1b8aeda45388631ddde7877a745)
- [Swagger](https://invite.test.surfconext.nl/ui/swagger-ui/index.html)
- [Code](https://github.com/OpenConext/OpenConext-Invite/)
- [Backlog](https://github.com/orgs/OpenConext/projects/5)
