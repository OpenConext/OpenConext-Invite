
# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## Next (1.0.0)

## 1.0.1

- Fix: eduID only role
- fix: Adding application to form when sreating a role from the application page
- Fix: JS issues on create invitation page
- Fix: idpScoping for long list of IdP's
- Fix: javascipt issues on search
- Fix: Reuireing a validated eduID

## 1.0.0

- The `scim_user_identifier` from Manage determines the `userName` (and not primarily the `externalId`)
- Endpoint for Access to retrieve roles
- Add the option to only accept an invitation with a validated account
- Fix scim_user_provisioning_only
- Fix for duplicate table errors
- Move from cronjob master to database locks
- Improve logging
- Display visual indication of loading (many) roles for new invite
- Make organization_guid required for all Roles #591
- Fix for broken impersonation – race condition
- Return to the correct index
- Allow extra (e.g., Portuguese) translations
- Allow defining a Role fixed end date
- Fix for reset / edit function in user-roles #554
- Bugfix for scoping authorities in new invitations
- Make organization_guid required for all Roles

## 0.0.37

- Bugfix for required short_name in update Role
- Fix searching for short strings
- IFix Invitation form is not right if user also has Guest authority on that role
- Add users' institution identifier during invite creation
- Fix Role expiration shorter than the defined role-expiration-notifier-duration-days variable
- Fix Incorrect name for invitations when using API keys
- Improve the 'hint' descriptions in the 'advanced options'
- Allow more characters in shortname and URN
- Limit WAYF to IdP's connected to application
- Remove externalId from SCIM /Group PATCH operations

## 0.0.36

- UI: Consistent Date Formatting
- Fix: Don't stop cleanup job on remote error
- Fix: Inviter Authority: Invite interface shows too many roles, including ones with only Guest Access
- Handle Role expiration shorter than the defined role-expiration-notifier-duration-days variable correctly
- API: A POST to /api/external/v1/invitations should not allow to specify an id or urn

## 0.0.35

- Bugfix for duplicate tab's as superuser
- Fix Leading slash for `internal/**` in spring-security
- Warn when creating a new role if an app can't provision yet
- Add Prometheus endpoint for easy statistics

## 0.0.34

- Added more logging to manage API calls and improve stability
- Optimized table size on full screen view for better readability
- Invitation system now recognizes multiple Identity Providers (IdPs)
- Fixed bug allowing users to log in by pressing 'Enter' on the splash screen
- Applied a consistent naming convention in the application hierarchy
- Fixed searching for roles that contain a hyphen (-)
- Invitation applications no longer differentiate between individual IdPs of a single institution
- Implemented easy statistics via Prometheus for performance monitoring
- Store audit trail for role memberschips
- Allow creation of smaller scoped API access tokens
- Allow searching for institution name
- Allow extending invite validity end-date
- Institution admins can now be invited for roles outside their own organisation
- Super-users can delete users now
- Upgrade spring-security

## 0.0.33

- Lower mobile breakpoint to avoid mobile view on half of the desktop size
- Bugfix for accepting an invitation created by remote API user

## 0.0.32

- Bugfix for not redirecting to invitation
- Bugfix for scim bearer token
- Bugfix for accepting invitation in client
- Add active and phoneNumers to SCIM messages
- Handle empty SCIM provisioning response
- Fix scim_user_provisioning_only function
- Improve error log on missing ID in SCIM message
- Add more logging
- API endpoint '/v2/users' renamed to '/v2/Users', /groups to /Groups
- Remove identifiers from SCIM patch request

## 0.0.31

- Avoid deadlocks when trying to create roles with the same subset of applications under heavy load

## 0.0.30

- Use the new CompoundKeyStore for secrets in Manage

## 0.0.29

- Add more logging
- Fix plain text invitation email link
- Return to role instead of index after deleting people
- Make authority names more consistent

## 0.0.28

- Upped dependencies
- Userflow for adding an institution admin
- Add support for Bearer token authentication in SCIM provisionings

## 0.0.27 2025-02-11

- Also allow to remove the top application from a role
- Redesign overviewlist for applications, for apps that are included in many roles
- Update Spring versions

## 0.0.25 2025-01-14

- Change contact information for inviters
- eVA enddate and user data changeable
- Load data paginated server side
- Update SP-dashboard API paths
- Fixed for super-user
- Return role-identifier when creating a role using the API

## 0.0.24 2024-12-05

- Load and display lists paginated
- Repair sorting role guests on institution
- Repair searching for emails
- Provision changed EVA user attributes

## 0.0.23 2024-10-29

- Allow extending eVa role enddates
- Migrated to JDK21

## 0.0.22 2024-10-22

- Update texts to match other Openconext projects
- Limit the roles seen by institution admins
- Improve mobile view
- Avoid short 404 before login redirect
- Add API endpoints for Openconext-spdashboard integration
- Add API endpoints for Openconext-myconext integration
- Fixes https://github.com/OpenConext/OpenConext-Invite/issues/307,
https://github.com/OpenConext/OpenConext-Invite/issues/312,
https://github.com/OpenConext/OpenConext-Invite/issues/320,
https://github.com/OpenConext/OpenConext-Invite/issues/240,
https://github.com/OpenConext/OpenConext-Invite/issues/310

## 0.0.20 2024-09-13

- Update translations
- Bugfix for institution admin without applications
- Update javascript dependencies
- Use POST for Openconext-Manage raw
- Search to avoid long query URL
- JS tests for sorting / subsorting applications
- Migrate to different mariadb library
- Upgrade pgrade-azure-dependency
- Scope enum for API users
- Added sp_dashboard portal user and API
- Sort roles for institution admins
- Allow searching for specific role types in users
- Show correct landingpage for roles in UI and API
- Fixes issues https://github.com/OpenConext/OpenConext-Invite/issues/231,
https://github.com/OpenConext/OpenConext-Invite/issues/302,
https://github.com/OpenConext/OpenConext-Invite/issues/240,
https://github.com/OpenConext/OpenConext-Invite/issues/298,
https://github.com/OpenConext/OpenConext-Invite/issues/296,
https://github.com/OpenConext/OpenConext-Invite/issues/295,
https://github.com/OpenConext/OpenConext-Invite/issues/297,
https://github.com/OpenConext/OpenConext-Invite/issues/239,
https://github.com/OpenConext/OpenConext-Invite/issues/239,
https://github.com/OpenConext/OpenConext-Invite/issues/241,
https://github.com/OpenConext/OpenConext-Invite/issues/241,
https://github.com/OpenConext/OpenConext-Invite/issues/293,
https://github.com/OpenConext/OpenConext-Invite/issues/235,
https://github.com/OpenConext/OpenConext-Invite/issues/235,
https://github.com/OpenConext/OpenConext-Invite/issues/234,
https://github.com/OpenConext/OpenConext-Invite/issues/232,
https://github.com/OpenConext/OpenConext-Invite/issues/232,
https://github.com/OpenConext/OpenConext-Invite/issues/233,
https://github.com/OpenConext/OpenConext-Invite/issues/233,
https://github.com/OpenConext/OpenConext-Invite/issues/231,
https://github.com/OpenConext/OpenConext-Invite/issues/230,
https://github.com/OpenConext/OpenConext-Invite/issues/227,
https://github.com/OpenConext/OpenConext-Invite/issues/228,
https://github.com/OpenConext/OpenConext-Invite/issues/291

## 0.0.19 2024-09-10

- Bugfix for too long Manage API calls

## 0.0.17 2024-08-27

- Institution admin can now select all applications connected to their IdP
- Add user search tab for institution admins
- Applications tab now shows list of applications
- Bugfix for too many role expiry notifications
- Bugfix for profile API
- UI language improvements
- Logging improvements

## 0.0.16 2024-07-02

- Super-user tokens
- Bugfix for NPE after adding users by API
- Configurable scim external ID

## 0.0.15 - 2024-06-26

- Update dependencies
- Force login for authenticated user new invite
- Warn user for email-formats not accepted by Microsoft
- Show error when SCIM is not responding
- CreatedAt for new provisionings
- Show warning when deleting role
- Endpoint for Profile to fetch roles for user
- Introduce RoleManager permissions

## 0.0.14 - 2024-05-27

- Different invitation screen for inviters
- Institution manager can manage managers
- Improve invite email texts
- Fix typo in help message
- Ignore exceptions in remote endpoints during login
- When creating role, not preselect random/first app
- SCIM update group request after user role delete
- Resending an invite should use the language setting the original invite was sent in
- Do not delete expired user-roles without notification

## 0.0.12 - 2024-20-04

- Fix error-handling when name-update fails in SCIM call

## 0.0.11 - 2024-04-16

- Fix styling of welcome deadend on medium screens
- Make clear on welcome/profile which are your actual guest roles and for which you have a different authority
- Fix wrong explanation with override invite settings option
- Voot and invite-AA endpoints will only return roles the user is actually a guest in, not a different authority, just like provisioning already works
- Unbreak 'also invite as guest' when a user upgrades their own role
- Support updating changed attributes in SCIM
- Provisionings now show created date
- Fix correct target authorities of Teams admins, managers while migrating a team
- Update several frontend dependencies for vulnerabilities
- Update to newest version of SURF SDS

## 0.0.10 - 2024-03-14

- Not used bi-directional relation removed
- Enable metrics endpoint
- Update github page
- Choose language of invitation mail
- Bugfix for migrated users
- Idempotency bugfix for manage queries
- Refactor the GitHub action for building the app and publishing the docker
- Rename confusingly misspelled lifecy(c)le config
- Enable metrics

## [0.0.5] - 17-02-2024

- Remove Institution admin role when entitlement is no longer present
- Fix javascript error when searching in apps
- Allow role expiery to be changed
- Require givenname and sn attributes

## [0.0.4] - 16-01-2024

- Add API tokens for direct access to api
- Add Multirole applications
- Add features for SURFconext Teams migation
- Update translations
- Notifications for removed applications
- Add encryption for manage secrets
- Allow admins to accept eduID-only invites with institution account

## [0.0.3] - 15-11-2023

- Delete owl sound
- Fix userrole deletion

## [0.0.2] - 07-11-2023

- First stable release
