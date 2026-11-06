# Mail templates

Mails are rendered by `invite.mail.MailBox` with [mustache.java](https://github.com/spullara/mustache.java).
Every mail has an HTML and a plain-text body per language: `<name>_en.html`, `<name>_en.txt`, `<name>_nl.html`, `<name>_nl.txt`.
Subjects are in `subjects.json`. 
## How a mail is built

```
invitation_en.html            layout.html (page, card, h1, footer)
{{<layout.html}}      ───────▶ {{$heading}}…{{/heading}}   default: {{title}}
  {{$heading}}…                {{$preheader}}…{{/preheader}}
  {{$preheader}}…              {{$content}}{{/content}}    <- the mail body goes here
  {{$content}}                 {{$signoff}}…{{/signoff}}   default: support text
    <p>…</p>
    {{<ui_button.html}} ─────▶ ui_button.html   {{$href}} {{$label}} {{$linkText}} {{$width}}
    {{<ui_sender.html}} ─────▶ ui_sender.html   {{$caption}}
    {{<ui_quote.html}}  ─────▶ ui_quote.html    {{$caption}} (only shown if `message` is set)
  {{/content}}
{{/layout.html}}
```

There are three different Mustache constructs in these files:

| Syntax | What it is | Example |
|---|---|---|
| `{{name}}` / `{{{name}}}` | **Variable** from the Java `Map` in `MailBox`. In `.html` files `{{ }}` HTML-escapes (also correct inside `href`/`src`), `{{{ }}}` does not: use it only for `messageHtml`, which is escaped in Java. In `.txt` files nothing is escaped (`MailBox` uses a separate factory), so `{{ }}` and `{{{ }}}` behave the same. | `{{displaySenderName}}` |
| `{{<layout.html}}…{{/layout.html}}` | **Inheritance**: this file *extends* another template. The child can only fill the `{{$block}}`s the parent declares. | `layout.html`, `ui_button.html` |
| `{{$block}}default{{/block}}` | **Block**: a named slot. The parent declares it (with an optional default), the child overrides it. This is how parameters are passed to a "component". | `{{$label}}Accept invitation{{/label}}` |

So in `invitation_en.html`: `layout.html`, `ui_button.html`, `ui_sender.html` and `ui_quote.html` are **templates we extend**,
not variables. The `{{$…}}` lines are the **blocks** being filled in, and `{{productName}}`, `{{displaySenderName}}`, `{{url}}` and the like are
**variables** supplied by `MailBox`. Everything else (the `<p style="…">`) is plain HTML copied into the `content` block.
`{{> partial}}` (a plain include) is not used; inheritance replaces it because it lets us pass parameters.

Variables available in every mail (added by `MailBox.sendMail`): `title` (subject), `productName` (property `email.productName`, default SURFconext Invite), `lang`, `nl` (boolean, use
`{{#nl}}…{{/nl}}{{^nl}}…{{/nl}}` for language switches inside shared files), `supportEmail`, `privacyUrl`, and `environment`
(only set when not prod; shows the yellow banner). Free-text from users is passed as `message` (raw, for `.txt`) and
`messageHtml` (escaped, with `<br/>`, for `.html`).

## Where the styling lives

Email clients (Gmail, Outlook) strip or ignore `<style>`, classes and CSS variables, so **all styles are inline**, and they are
defined in as few places as possible:

- `layout.html` – page background, 709px card, title (`h1`), sign-off, footer, environment banner, fonts, Outlook fixes.
- `ui_button.html` – the green button (VML version for Outlook Windows) and the "or copy this link" line.
- `ui_quote.html` – the grey bordered box for personal messages. `ui_sender.html` – logo + name + organization.
- Body paragraphs inside a mail repeat the `<p style="…">` string. Copy it from an existing mail.

Rules for email-safe HTML: tables for layout (`role="presentation"`), no `flex`/`grid`, no SVG, no CSS variables, no `margin`
on tables (Outlook ignores it: wrap in a cell with `padding` instead), every text element carries its own `font-family`, images
have `alt`, and the SURF logo is the inline `cid:surfLogo` PNG (`surf_logo.png`, 142×72).

## Add a new mail

1. Add a subject key to `subjects.json` for `en` and `nl`.
2. Copy the closest existing mail, e.g. `invitation_accepted_*`, to `<name>_en.html`, `<name>_nl.html`, `<name>_en.txt`, `<name>_nl.txt`.
   Change the `heading`, `preheader` and the text in `content`. Compose `ui_button` / `ui_quote` / `ui_sender` as needed.
   Internal mails without the support sign-off add an empty `{{$signoff}}{{/signoff}}`.
3. Add a `sendXxx` method to `MailBox` that puts the variables in the map and calls
   `sendMail("<name>_" + language, title, variables, recipients)`.
4. Add the mail to `MailBoxTemplatesTest`. It renders every mail in both languages and fails on unresolved `{{ }}`, flexbox, SVG,
   a missing logo or a missing `<h1>`. It writes the results to `server/target/mail-preview/` for inspection in a browser.
5. Send it for real to a local SMTP catcher such as Mailpit (http://localhost:8025) and use its *HTML Check* tab.

## When the design changes

| Change | Edit |
|---|---|
| Colours, fonts, card width/padding, title style, footer, support sign-off | `layout.html` (and the matching `font-family` string in the other files) |
| Button look | `ui_button.html`. Keep the VML block in sync with the table version (colour, height, radius). |
| Quote box / sender block | `ui_quote.html` / `ui_sender.html` |
| Paragraph style | the `<p style="…">` string; replace it in all `*_en.html`/`*_nl.html` (a search-and-replace) |
| Logo | replace `surf_logo.png` (PNG, 2× resolution: 142×72, opaque white background, shown at 71×36) |
| Copy of one mail | that mail's `_en`/`_nl` HTML **and** `.txt`, and the subject in `subjects.json` |
| Footer links | `PRIVACY_URL_*` constants in `MailBox` |

After any change run `mvn test -Dtest=MailBoxTemplatesTest`, open a few files from `target/mail-preview/`, and check them in Mailpit.
Spot-check in real clients when the layout changes (Outlook for Windows, Gmail, iOS Mail).
