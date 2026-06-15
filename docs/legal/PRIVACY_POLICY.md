# Privacy Policy — VibeCheck

> **DRAFT — not legal advice.** This is an engineering draft that reflects how
> the app actually handles data. Have it reviewed by a qualified lawyer before
> publishing. Replace every `[PLACEHOLDER]` and confirm jurisdiction-specific
> obligations (UK GDPR, EU GDPR, US state laws, COPPA, UK Online Safety Act).

**Effective date:** `[DATE]`
**Provider:** `[COMPANY / DEVELOPER NAME]` ("we", "us")
**Contact:** `[PRIVACY CONTACT EMAIL]`
**App:** VibeCheck (Android), package `com.vibecheck.app`

VibeCheck is an anonymous mood-tracking app for users in the United States and
the United Kingdom. Privacy is the core design principle: we collect as little
as possible and never ask for your real identity.

## 1. The short version

- **No account, no email, no phone number, no real name.** You use the app
  through an anonymous identifier only.
- **No precise location.** We use a coarse region (your nearest large city) at
  most — never GPS coordinates.
- **Mood check-ins are anonymous even to us.** They are stored without any link
  to your identifier.
- **No advertising, no data selling, no third-party tracking SDKs for ads.**
- **Everything is deleted after 90 days of inactivity.**

## 2. Information we process

| Data | Why | Stored where | Identifiable? |
|---|---|---|---|
| Anonymous user id (Firebase Anonymous Auth) | Run the app, save your settings | Device + our backend | Pseudonymous; not linked to a real person |
| Age bracket (under 16 / 16–17 / 18+) | Age gating (16+ only) | Device + backend | No |
| Optional username | Display only; never your real name | Device + backend | No |
| Mood check-ins (mood, optional ≤5-word note, timestamp, coarse region) | Your history, streaks, aggregated heatmap | Device (full history) + backend (anonymised) | **No** — the backend record carries no user id (the document id is a one-way hash of the timestamp plus a random salt) |
| Chat messages (if you opt in) | Deliver a 5-minute anonymous chat | Backend, temporarily | Pseudonymous to participants; auto-deleted shortly after the chat ends |
| Subscription status | Unlock VibeCheck Plus | Google Play + backend | Pseudonymous |

We do **not** collect: real name, email, phone number, precise location/GPS,
device advertising identifiers, contacts, photos, or biometric data. We do not
log IP addresses for analytics or tracking.

## 3. How we use it

- Provide the core features: check-ins, history, micro-actions, the aggregated
  anonymous heatmap, optional anonymous chat, and insights.
- Maintain safety: a profanity filter and a report function in chat.
- Operate the subscription (via Google Play Billing).
- We do **not** use your data for advertising or profiling, and we do not sell it.

## 4. The anonymous heatmap

Individual check-ins are never shown. The heatmap displays only **aggregated**
mood per region (counts and averages). Your contribution cannot be singled out.

## 5. Anonymous chat (opt-in)

If you enable chat, you may be matched with another user for up to 5 minutes.
No real names, no photos. Messages are filtered for profanity and can be
reported. Sessions and their messages are **automatically deleted** shortly
after they end. Depending on regulatory review, chat may be restricted to users
18 and over.

## 6. Children

VibeCheck is for ages **16 and over**. We do not knowingly collect data from
anyone under 16. The app applies an age gate at onboarding. (US COPPA /
UK age-appropriate design considerations: `[CONFIRM WITH COUNSEL]`.)

## 7. Sharing

We share data only with infrastructure providers that run the service on our
behalf:

- **Google Firebase** (anonymous authentication, database, cloud functions) — `[link to Google's terms]`
- **Google Play Billing** (subscriptions) — `[link]`

We disclose information if required by law or to protect users' safety.

## 8. Retention & deletion

- Your on-device history stays until you delete the app or clear its data.
- Backend data is deleted after **90 days of inactivity**.
- Chat content is deleted shortly after each session ends.
- You can erase your data at any time via **Settings → Delete all my data**.

## 9. Your rights

Depending on where you live (UK/EU GDPR, US state privacy laws) you may have
rights to access, correct, delete, or port your data, and to object to certain
processing. Because the data is anonymous, we may be unable to locate records
tied to you specifically; the in-app delete provides immediate erasure. Contact
`[PRIVACY CONTACT EMAIL]` for requests. `[CONFIRM rights wording with counsel.]`

## 10. Security

Data in transit is encrypted (HTTPS/TLS). Access to backend data is restricted
by deny-by-default security rules. No method is 100% secure, but we design for
data minimisation so there is little sensitive data to expose.

## 11. Not a medical service

VibeCheck is a wellbeing tool, **not a medical device** and not a substitute for
professional care. If you are in crisis: in the US call or text **988**; in the
UK call **116 123** (Samaritans).

## 12. Changes

We will update this policy as needed and revise the effective date. Material
changes will be highlighted in the app.

## 13. Contact

`[COMPANY / DEVELOPER NAME]` — `[PRIVACY CONTACT EMAIL]` — `[POSTAL ADDRESS IF REQUIRED]`
