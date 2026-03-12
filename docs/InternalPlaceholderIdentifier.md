# Technical Documentation: Internal Placeholder Identifier

The **Internal Placeholder Identifier** feature allows institutions to bridge
the gap between their local identity management systems and eduID. It enables
"Pre-Provisioning," where an institution can assign roles and rights to a user
in their internal systems *before* that user has accepted an invitation or
created their eduID.

## Functional Overview

An institution can associate a local, internal ID (e.g., a staff number or
temporary registration ID) with an invitation. When the user accepts the
invitation, this identifier is used to synchronize the account with the
institution’s downstream systems. Once the invite is accepted and provisioning
is done, the system transitions to using the permanent identifier for all
future updates.

## API Specification (`POST /api/external/v1/invitations`)

The Invitation API supports two mutually exclusive methods for specifying
recipients. You must use **either** the standard `invites` list **or**
the `invitesWithInternalPlaceholderIdentifiers` list.

### Request Constraints

* **Mutually Exclusive:** Do not populate both arrays in a single request.
* **Requirement:** At least one email address must be present in one of the
two arrays.
* **Labeling:** This field is explicitly named `internalPlaceholderIdentifier`
in the API and UI to encourage its use as a transient "stand-in".

### Request Body (`InvitationRequest`)

| Attribute | Type | Description |
| --- | --- | --- |
| `invites` | Array (String) | Standard email list for users without pre-provisioned internal IDs. |
| `invitesWithInternalPlaceholderIdentifiers` | Array (Object) | List of objects containing `email` and `internalPlaceholderIdentifier`. |
| `intendedAuthority` | String | Authority level of the inviter (e.g., `"GUEST"`). |
| `roleIdentifiers` | Array (Int) | List of Role IDs to be granted upon acceptance. |

#### Example Payload: Pre-Provisioning Invitation

```json
{
  "intendedAuthority": "GUEST",
  "language": "en",
  "invitesWithInternalPlaceholderIdentifiers": [
    {
      "email": "new_hire@institution.edu",
      "internalPlaceholderIdentifier": "STF-2024-9901"
    }
  ],
  "roleIdentifiers": [99],
  "roleExpiryDate": 1760788376,
  "expiryDate": 1730461976
}

```

## Provisioning & Lifecycle Logic

### Initial Provisioning (SCIM POST)

When the user accepts the invitation, the system sends a SCIM `POST /v1/Users`
message to the institution's provisioning endpoint. The
`internalPlaceholderIdentifier` is sent as the `id` in this message.

### Identifier Transition

The system will use the `id` returned by your SCIM server in the response to
that initial POST for all future updates (e.g., role changes or group
memberships). This allows the institution to replace the temporary placeholder
with a permanent identifier (such as a UUID or eduID URN) for all subsequent
SCIM messages.
