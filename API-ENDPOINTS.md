# DevFlow Backend API Endpoints

Base URL: `http://{host}:{port}/api`

Alle Request/Response Bodies sind JSON (`Content-Type: application/json`).
Authentifizierte Endpoints brauchen den Header `Authorization: Bearer {jwt}`.
Timestamps im ISO 8601 Format (z.B. `2026-04-14T12:00:00Z`).

---

## Auth

### POST /api/auth/register

Neuen Benutzer registrieren.

**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response 201 (Created):**
```json
{
  "accessToken": "jwt-string",
  "refreshToken": "string",
  "expiresAt": "2026-04-14T12:00:00Z",
  "user": {
    "id": 1,
    "username": "alice",
    "avatarUrl": null,
    "online": true,
    "createdAt": "2026-04-10T08:00:00Z"
  }
}
```

**Response 409 (Conflict):**
```json
{ "error": "Username already taken" }
```

**Response 400 (Bad Request):**
```json
{ "error": "Username must not be empty" }
```

---

### POST /api/auth/login

Benutzer anmelden.

**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response 200 (OK):**
```json
{
  "accessToken": "jwt-string",
  "refreshToken": "string",
  "expiresAt": "2026-04-14T12:00:00Z",
  "user": {
    "id": 1,
    "username": "alice",
    "avatarUrl": null,
    "online": true,
    "createdAt": "2026-04-10T08:00:00Z"
  }
}
```

**Response 401 (Unauthorized):**
```json
{ "error": "Invalid credentials" }
```

---

### POST /api/auth/refresh

Access-Token mit Refresh-Token erneuern.

**Request:**
```json
{
  "refreshToken": "string"
}
```

**Response 200 (OK):**
```json
{
  "accessToken": "jwt-string",
  "refreshToken": "string",
  "expiresAt": "2026-04-14T13:00:00Z"
}
```

**Response 401 (Unauthorized):**
```json
{ "error": "Invalid or expired refresh token" }
```

---

## Users

Alle User-Endpoints sind authentifiziert (`Authorization: Bearer {jwt}`).

Das `User`-Objekt hat folgende Felder:

| Feld      | Typ             | Beschreibung                                 |
|-----------|-----------------|----------------------------------------------|
| id        | long            | Primary Key                                  |
| username  | string          | Eindeutig                                    |
| avatarUrl | string \| null  | URL zum Profilbild (optional)                |
| online    | boolean         | Online-Status (z.B. aus Session/Heartbeat)   |
| createdAt | ISO 8601 string | Registrierungszeitpunkt                      |

### GET /api/users

Alle registrierten Benutzer auflisten (ausser dem aktuellen User).

**Response 200 (OK):**
```json
[
  {
    "id": 1,
    "username": "alice",
    "avatarUrl": null,
    "online": true,
    "createdAt": "2026-04-10T08:00:00Z"
  },
  {
    "id": 2,
    "username": "bob",
    "avatarUrl": "https://example.com/avatars/bob.png",
    "online": false,
    "createdAt": "2026-04-11T09:00:00Z"
  }
]
```

---

### GET /api/users/search?q={query}

Benutzer nach Username suchen (case-insensitive, enthaelt Match).
Wird vom Client fuer die Sidebar-Suche und den "Neuer Chat"-Dialog verwendet.

**Query Parameter:**

| Parameter | Typ    | Pflicht | Beschreibung                                  |
|-----------|--------|---------|-----------------------------------------------|
| q         | string | nein    | Suchbegriff. Leerer String -> gleiche Ausgabe wie `GET /api/users` |

**Response 200 (OK):**
```json
[
  { "id": 1, "username": "alice", "avatarUrl": null, "online": true, "createdAt": "2026-04-10T08:00:00Z" }
]
```

**Hinweise:**
- Der aktuelle User ist aus den Ergebnissen auszuschliessen
- Empfohlenes Matching: `LOWER(username) LIKE '%' || LOWER(:q) || '%'`
- Empfohlenes `limit`: 50

---

### GET /api/users/{id}

Einzelnen Benutzer nach ID abrufen.

**Response 200 (OK):**
```json
{
  "id": 1,
  "username": "alice",
  "avatarUrl": null,
  "online": true,
  "createdAt": "2026-04-10T08:00:00Z"
}
```

**Response 404 (Not Found):**
```json
{ "error": "User not found" }
```

---

### GET /api/users/me

Aktuell angemeldeten Benutzer abrufen (aus JWT extrahiert).

**Response 200 (OK):**
```json
{
  "id": 1,
  "username": "alice",
  "avatarUrl": null,
  "online": true,
  "createdAt": "2026-04-10T08:00:00Z"
}
```

---

### PUT /api/users/me

Profil des aktuellen Users aktualisieren (Username und/oder Avatar).
Felder sind optional - nur gesendete Felder werden aktualisiert.

**Request:**
```json
{
  "username": "alice2",
  "avatarUrl": "https://example.com/avatars/alice.png"
}
```

**Response 200 (OK):**
```json
{
  "id": 1,
  "username": "alice2",
  "avatarUrl": "https://example.com/avatars/alice.png",
  "online": true,
  "createdAt": "2026-04-10T08:00:00Z"
}
```

**Response 409 (Conflict):**
```json
{ "error": "Username already taken" }
```

**Response 400 (Bad Request):**
```json
{ "error": "Username must not be empty" }
```

**Hinweise:**
- `avatarUrl = null` oder leerer String loescht das Profilbild
- Password-Aenderung ist (noch) nicht vorgesehen

---

## Workspaces

Alle Workspace-Endpoints sind authentifiziert.

Ein **Workspace** ist der Top-Level-Container fuer Team-Zusammenarbeit. Jeder
User hat mindestens einen Workspace ("Persoenlich", beim Register automatisch
angelegt) und kann beliebig vielen weiteren angehoeren. Gruppenchats leben
innerhalb eines Workspaces; DMs leben ausserhalb.

> **Phase 2b MVP + 2c (Stand 2026-04-24):**
> Implementiert sind: List, Create, Detail, Rename, **Join via Invite-Code**,
> Members list, Member remove, sowie **Workspace-Gruppen** (Ordner, CRUD).
> Rollen im MVP: `OWNER` und `MEMBER` (kein `ADMIN`). Der Ersteller wird
> `OWNER`, alle anderen beim Join `MEMBER`.
>
> **Reserved (spaeter):** `DELETE /api/workspaces/{id}`, `POST /{id}/members`
> (direktes Hinzufuegen per userId — im MVP laeuft Beitritt ausschliesslich
> ueber Invite-Code), `PUT /{id}/members/{userId}` (Rollenwechsel).
>
> Das Feld `slug`/`description` aus aelteren Spec-Versionen ist **entfallen**.
> Stattdessen gibt es `inviteCode` (8 Zeichen, fuer alle Mitglieder sichtbar)
> und `isPersonal` (true fuer den beim Register angelegten Workspace).

Das `Workspace`-Objekt:

| Feld        | Typ                               | Beschreibung                                            |
|-------------|-----------------------------------|---------------------------------------------------------|
| id          | long                              | Primary Key                                             |
| name        | string                            | Anzeigename                                             |
| ownerId     | long \| null                      | Ersteller; kann `null` werden wenn der Owner-User geloescht wird (FK `ON DELETE SET NULL`) |
| inviteCode  | string (8 Zeichen)                | Aus `[A-HJ-NP-Z2-9]` (ohne verwechselbare I/O/0/1). Wird beim Create vergeben, derzeit nicht rotierbar. |
| isPersonal  | boolean                           | `true` beim automatisch angelegten persoenlichen Workspace; kann umbenannt, aber nicht geloescht werden. |
| createdAt   | ISO 8601 string                   |                                                         |
| memberCount | int                               | Anzahl Mitglieder (immer gesetzt)                       |
| role        | `"OWNER"` \| `"MEMBER"`           | Rolle des aktuellen Users (immer gesetzt)               |

### GET /api/workspaces

Alle Workspaces auflisten, in denen der aktuelle User Mitglied ist.
Sortierung: persoenlicher Workspace zuerst (`is_personal DESC`), danach nach
`created_at ASC`.

**Response 200 (OK):**
```json
[
  {
    "id": 1,
    "name": "Persoenlich",
    "ownerId": 1,
    "inviteCode": "A7K2P9QX",
    "isPersonal": true,
    "createdAt": "2026-04-10T08:00:00Z",
    "memberCount": 1,
    "role": "OWNER"
  },
  {
    "id": 2,
    "name": "DevFlow",
    "ownerId": 1,
    "inviteCode": "B3M7R2SV",
    "isPersonal": false,
    "createdAt": "2026-04-11T09:00:00Z",
    "memberCount": 12,
    "role": "OWNER"
  }
]
```

---

### POST /api/workspaces

Neuen Workspace erstellen. Der Ersteller wird automatisch `OWNER`. Der
`inviteCode` wird serverseitig erzeugt (8 Zeichen, Alphabet ohne I/O/0/1,
bis zu 5 Kollisionsversuche).

**Request:**
```json
{ "name": "DevFlow" }
```

| Feld | Typ    | Pflicht | Beschreibung      |
|------|--------|---------|-------------------|
| name | string | ja      | 1-100 Zeichen     |

**Response 201 (Created):** `Workspace`-Objekt.

**Response 400 (Bad Request):** `{ "error": "name must not be blank" }`

---

### GET /api/workspaces/{workspaceId}

Detail eines einzelnen Workspaces.

**Response 200 (OK):** `Workspace`-Objekt (wie oben).

**Response 403 (Forbidden):** User ist nicht Mitglied.

---

### PUT /api/workspaces/{workspaceId}

Workspace umbenennen (nur `OWNER`). Funktioniert auch fuer den persoenlichen
Workspace.

**Request:**
```json
{ "name": "DevFlow Team" }
```

**Response 200 (OK):** aktualisiertes `Workspace`-Objekt.

**Response 403 (Forbidden):** Nicht-Owner.

> **Reserved:** `DELETE /api/workspaces/{workspaceId}` — weiterhin offen (nicht
> in 2c gemacht). Im aktuellen Scope gibt es keine Workspace-Loeschung.
> Persoenliche Workspaces koennen sowieso nicht geloescht werden
> (`isPersonal = true`).

---

### POST /api/workspaces/join

Einem Workspace per 8-stelligem Invite-Code beitreten. Der Code wird
case-insensitiv verglichen und serverseitig auf Grossbuchstaben normalisiert.
Der beitretende User wird `MEMBER`.

**Request:**
```json
{ "inviteCode": "a7k2p9qx" }
```

| Feld        | Typ    | Pflicht | Beschreibung                      |
|-------------|--------|---------|-----------------------------------|
| inviteCode  | string | ja      | Exakt 8 Zeichen `[A-Za-z0-9]`     |

**Response 201 (Created):** `Workspace`-Objekt (mit `role = "MEMBER"`).

**Response 400 (Bad Request):** `{ "error": "inviteCode must match ..." }`
**Response 404 (Not Found):** `{ "error": "Invite code not found" }`
**Response 409 (Conflict):** `{ "error": "Already a member of this workspace" }`

---

### GET /api/workspaces/{workspaceId}/members

Mitglieder eines Workspaces. Sortierung: Owner zuerst, danach nach
`joined_at ASC`.

**Response 200 (OK):**
```json
[
  { "userId": 1, "username": "alice", "role": "OWNER",  "joinedAt": "2026-04-10T08:00:00Z" },
  { "userId": 2, "username": "bob",   "role": "MEMBER", "joinedAt": "2026-04-11T09:00:00Z" }
]
```

**Response 403 (Forbidden):** User ist nicht Mitglied.

---

### DELETE /api/workspaces/{workspaceId}/members/{userId}

Mitglied entfernen. Regeln:
- Ein User kann sich selbst entfernen (austreten) — aber **nicht** aus dem
  persoenlichen Workspace (`isPersonal = true` → 400).
- Der `OWNER` kann andere `MEMBER` entfernen (Kick).
- Der `OWNER` kann sich **nicht** selbst entfernen (400). Owner-Handover ist im
  MVP nicht vorgesehen.

**Response 204 (No Content)**

**Response 400 (Bad Request):**
- `{ "error": "Owner cannot leave their own workspace" }`
- `{ "error": "Cannot leave your personal workspace" }`

**Response 403 (Forbidden):** Caller ist weder Betroffener noch Owner.
**Response 404 (Not Found):** User ist nicht Mitglied.

> **Reserved:** `POST /api/workspaces/{id}/members` (Add by userId) und
> `PUT /api/workspaces/{id}/members/{userId}` (Rollenwechsel) — siehe MVP-Scope
> oben.

---

## Workspace-Gruppen (Ordner)

Alle Workspace-Group-Endpoints sind authentifiziert. Seit **Phase 2c**
(Stand 2026-04-24) implementiert.

Eine **Gruppe** (workspace-group) ist rein organisatorisch — ein **Ordner**
unterhalb eines Workspaces, der Gruppenchats buendelt. Gruppen haben keine
eigenen Permissions: jedes Workspace-Mitglied sieht und kann in jede Gruppe
posten. Permissions bleiben am Chat (`ownerId`, `memberAddPolicy`).

**Sichtbarkeit:** Nur Workspace-Mitglieder sehen die Gruppen des Workspaces.
**Edit-Rechte (create/rename/delete):** Nur `OWNER` des Workspaces.
**Delete ist nicht-kaskadierend:** Beim Loeschen einer Gruppe wird
`chats.group_id` aller enthaltenen Chats auf `NULL` gesetzt (FK
`ON DELETE SET NULL`). Die Chats ueberleben und fallen in die implizite
"Allgemein"-Section zurueck.

Das `Group`-Objekt:

| Feld        | Typ             | Beschreibung                                         |
|-------------|-----------------|------------------------------------------------------|
| id          | long            | Primary Key                                          |
| workspaceId | long            | Besitzender Workspace                                |
| name        | string          | Anzeigename, 1-100 Zeichen                           |
| sortOrder   | int             | Reihenfolge in der Sidebar (aufsteigend), default 0  |
| createdAt   | ISO 8601 string |                                                      |

### GET /api/workspaces/{workspaceId}/groups

Alle Gruppen eines Workspaces auflisten. Sortierung: `sort_order ASC`, dann
`created_at ASC`.

**Response 200 (OK):**
```json
[
  { "id": 1, "workspaceId": 2, "name": "Frontend", "sortOrder": 0, "createdAt": "2026-04-24T10:00:00Z" },
  { "id": 2, "workspaceId": 2, "name": "Backend",  "sortOrder": 1, "createdAt": "2026-04-24T10:05:00Z" }
]
```

**Response 403 (Forbidden):** User ist nicht Mitglied des Workspaces.

---

### POST /api/workspaces/{workspaceId}/groups

Neue Gruppe erstellen. Nur `OWNER` des Workspaces.

**Request:**
```json
{ "name": "Frontend" }
```

| Feld | Typ    | Pflicht | Beschreibung  |
|------|--------|---------|---------------|
| name | string | ja      | 1-100 Zeichen |

**Response 201 (Created):** `Group`-Objekt. `sortOrder` wird serverseitig
als `MAX(sort_order) + 1` gesetzt.

**Response 400 (Bad Request):** `{ "error": "name must not be blank" }`
**Response 403 (Forbidden):** Nicht-Owner.
**Response 404 (Not Found):** Workspace existiert nicht.

---

### PUT /api/workspaces/{workspaceId}/groups/{groupId}

Gruppe umbenennen. Nur `OWNER` des Workspaces.

**Request:**
```json
{ "name": "Frontend Team" }
```

**Response 200 (OK):** aktualisiertes `Group`-Objekt.
**Response 403 (Forbidden):** Nicht-Owner.
**Response 404 (Not Found):** Workspace oder Gruppe existiert nicht.

---

### DELETE /api/workspaces/{workspaceId}/groups/{groupId}

Gruppe loeschen. Nur `OWNER` des Workspaces.

Die enthaltenen Chats werden **nicht** geloescht — ihr `group_id` wird auf
`NULL` gesetzt (FK `ON DELETE SET NULL`), und sie tauchen in der impliziten
"Allgemein"-Section des Sidebars auf.

**Response 204 (No Content)** oder **200 (OK)**.

**Response 403 (Forbidden):** Nicht-Owner.
**Response 404 (Not Found):** Workspace oder Gruppe existiert nicht.

---

## Chats

Alle Chat-Endpoints sind authentifiziert.

Chats koennen zwei Typen haben:
- **`DM`** - Direktnachricht zwischen genau zwei Usern (immer ohne `workspaceId`).
- **`GROUP`** - Gruppenchat mit Name, Owner, Member-Add-Policy und beliebig vielen
  Mitgliedern. Seit Phase 2b an einen Workspace gebunden (`workspaceId` gesetzt).

Die Response nutzt `@JsonInclude(NON_NULL)` — Felder die fuer DMs nicht gelten
(`name`, `ownerId`, `memberAddPolicy`, `workspaceId`) werden im DM-Payload
komplett weggelassen, nicht als `null` serialisiert. Die Tabelle listet sie
trotzdem auf, weil sie fuer GROUP-Chats immer da sind.

Das `Chat`-Objekt hat folgende Felder:

| Feld              | Typ                                       | Beschreibung                                  |
|-------------------|-------------------------------------------|-----------------------------------------------|
| id                | long                                      | Primary Key                                   |
| type              | `"DM"` \| `"GROUP"`                       | Chat-Typ                                      |
| name              | string (GROUP) / fehlt (DM)               | Anzeigename der Gruppe                        |
| ownerId           | long (GROUP) / fehlt (DM)                 | User der die Gruppe erstellt hat              |
| memberAddPolicy   | `"OWNER_ONLY"` \| `"ALL_MEMBERS"` (GROUP) | Wer darf Mitglieder hinzufuegen               |
| workspaceId       | long (GROUP) / fehlt (DM)                 | Workspace in dem die Gruppe lebt. Seit 2b fuer neue Gruppen immer gesetzt. |
| groupId           | long \| null (GROUP) / fehlt (DM)         | Workspace-Ordner in dem die Gruppe gefiled ist. `null` = implizite "Allgemein"-Section (keiner Gruppe zugeordnet). Seit Phase 2c befuellbar. |
| createdAt         | ISO 8601 string                           |                                               |
| participants      | User[]                                    | Alle Teilnehmer (inkl. aktuellem User)        |
| lastMessage       | Message \| null                           | Letzte Nachricht (oder `null`)                |

### GET /api/chats

Alle Chats des aktuellen Users abrufen, sortiert nach letzter Nachricht (neueste zuerst).

**Response 200 (OK):**
```json
[
  {
    "id": 10,
    "type": "DM",
    "name": null,
    "ownerId": null,
    "memberAddPolicy": null,
    "createdAt": "2026-04-12T10:00:00Z",
    "participants": [
      { "id": 1, "username": "alice", "avatarUrl": null, "online": true },
      { "id": 2, "username": "bob",   "avatarUrl": null, "online": false }
    ],
    "lastMessage": {
      "id": 55,
      "chatId": 10,
      "content": "Hey, how are you?",
      "transmitterId": 2,
      "createdAt": "2026-04-14T09:30:00Z"
    }
  },
  {
    "id": 20,
    "type": "GROUP",
    "name": "SWP Team",
    "ownerId": 1,
    "memberAddPolicy": "ALL_MEMBERS",
    "createdAt": "2026-04-13T12:00:00Z",
    "participants": [
      { "id": 1, "username": "alice" },
      { "id": 2, "username": "bob" },
      { "id": 3, "username": "carol" }
    ],
    "lastMessage": null
  }
]
```

**Hinweise:**
- `lastMessage` kann `null` sein wenn der Chat keine Nachrichten hat
- `participants` enthaelt bei DM beide User, bei GROUP alle Mitglieder
- Sortierung: Chats mit neuester `lastMessage.createdAt` zuerst, Chats ohne Nachricht nach `createdAt`

---

### POST /api/chats/dm

DM-Chat mit einem anderen Benutzer erstellen oder bestehenden finden.
Wenn bereits ein DM-Chat zwischen den beiden Usern existiert, wird dieser zurueckgegeben (200).
Wenn nicht, wird ein neuer Chat erstellt (201).

**Request:**
```json
{
  "otherUserId": 2
}
```

**Response 200 (OK) - bestehender Chat gefunden:**
```json
{
  "id": 10,
  "type": "DM",
  "name": null,
  "ownerId": null,
  "memberAddPolicy": null,
  "createdAt": "2026-04-12T10:00:00Z",
  "participants": [
    { "id": 1, "username": "alice" },
    { "id": 2, "username": "bob" }
  ]
}
```

**Response 201 (Created) - neuer Chat erstellt:**
```json
{
  "id": 11,
  "type": "DM",
  "name": null,
  "ownerId": null,
  "memberAddPolicy": null,
  "createdAt": "2026-04-14T10:00:00Z",
  "participants": [
    { "id": 1, "username": "alice" },
    { "id": 2, "username": "bob" }
  ]
}
```

**Response 404 (Not Found):**
```json
{ "error": "User not found" }
```

**Hinweise:**
- Der aktuelle User (aus JWT) ist automatisch Teilnehmer
- Beide `chat_participant` Eintraege werden automatisch erstellt

---

### POST /api/chats/group

Neue Gruppe erstellen. Der aktuelle User wird automatisch Owner und Mitglied.

**Request:**
```json
{
  "name": "SWP Team",
  "memberAddPolicy": "ALL_MEMBERS",
  "memberIds": [2, 3, 4],
  "workspaceId": 2,
  "groupId": 5
}
```

| Feld            | Typ                                       | Pflicht | Beschreibung                                      |
|-----------------|-------------------------------------------|---------|---------------------------------------------------|
| name            | string                                    | ja      | 1-100 Zeichen                                     |
| memberAddPolicy | `"OWNER_ONLY"` \| `"ALL_MEMBERS"`         | ja      | Wer darf neue Mitglieder hinzufuegen              |
| memberIds       | long[]                                    | ja      | Initiale Mitglieder (ohne Owner, mind. 1 Eintrag) |
| workspaceId     | long                                      | nein*   | Workspace in dem der Chat lebt. *Waehrend der Phase-2b-Migration optional: fehlt er, faellt der Server auf den persoenlichen Workspace des Callers zurueck und loggt WARN. Sobald das 2b-UI ausgerollt ist, schickt der Client das Feld immer. |
| groupId         | long                                      | nein    | Workspace-Ordner in dem der Chat gefiled werden soll (Phase 2c). Wenn gesetzt: muss zum angegebenen (oder ermittelten) `workspaceId` gehoeren — sonst 400. Wenn weggelassen oder `null`: Chat landet in der impliziten "Allgemein"-Section des Workspaces. |

**Response 201 (Created):**
```json
{
  "id": 20,
  "type": "GROUP",
  "name": "SWP Team",
  "ownerId": 1,
  "memberAddPolicy": "ALL_MEMBERS",
  "workspaceId": 2,
  "groupId": 5,
  "createdAt": "2026-04-14T11:00:00Z",
  "participants": [
    { "id": 1, "username": "alice" },
    { "id": 2, "username": "bob" },
    { "id": 3, "username": "carol" },
    { "id": 4, "username": "dave" }
  ],
  "lastMessage": null
}
```

**Response 400 (Bad Request):**
- `{ "error": "name must not be blank" }`
- `{ "error": "memberIds must contain at least one other user" }`
- `{ "error": "User is not a member of the target workspace: 4" }` — einer der
  `memberIds` ist nicht Mitglied des angegebenen (oder ermittelten) Workspaces.
- `{ "error": "Group does not belong to target workspace" }` — `groupId` ist
  gesetzt, aber zeigt auf eine Gruppe eines anderen Workspaces.

**Response 403 (Forbidden):**
```json
{ "error": "Not a member of the target workspace" }
```
Der Caller hat einen `workspaceId` geschickt, ist dort aber kein Mitglied.

**Response 404 (Not Found):**
```json
{ "error": "User not found: 99" }
```

**Hinweise zur Workspace-Validierung:**
- Legacy-Pfad (`workspaceId` weggelassen): persoenlicher Workspace des Callers
  wird genommen. Damit landen Gruppen, die das alte UI erzeugt, stillschweigend
  im `Persoenlich`-Workspace des Erstellers. Andere User koennen beitreten nur,
  wenn sie dort Mitglied sind — praktisch also nur der Ersteller, was einen
  400 ausloest wenn `memberIds` weitere User enthaelt. Fuer die Uebergangsphase
  akzeptabel, der WARN-Log macht es auffindbar.
- Neuer Pfad (`workspaceId` gesetzt): Caller-Membership-Check (403 wenn nicht
  Mitglied) und danach pro `memberIds`-Eintrag (400 wenn Nicht-Member des
  Zielworkspaces).

---

### PUT /api/chats/{chatId}

Gruppe aktualisieren (Name und/oder Policy). Nur fuer GROUP-Chats.
Nur Felder die gesendet werden, werden aktualisiert.

**Request:**
```json
{
  "name": "SWP Team 2026",
  "memberAddPolicy": "OWNER_ONLY"
}
```

**Response 200 (OK):**
```json
{
  "id": 20,
  "type": "GROUP",
  "name": "SWP Team 2026",
  "ownerId": 1,
  "memberAddPolicy": "OWNER_ONLY",
  "createdAt": "2026-04-13T12:00:00Z",
  "participants": [ ... ]
}
```

**Response 403 (Forbidden):**
```json
{ "error": "Only the group owner can edit group settings" }
```

**Response 404 (Not Found):**
```json
{ "error": "Chat not found" }
```

**Hinweise:**
- Nur der Owner (`ownerId`) darf die Gruppe editieren
- DM-Chats koennen nicht editiert werden (`400`)

---

### POST /api/chats/{chatId}/members

Mitglied zu einer Gruppe hinzufuegen.

**Request:**
```json
{
  "userId": 5
}
```

**Response 200 (OK) oder 201 (Created):**

Gibt den aktualisierten `Chat` mit der neuen Mitgliederliste zurueck.
```json
{
  "id": 20,
  "type": "GROUP",
  "name": "SWP Team",
  "ownerId": 1,
  "memberAddPolicy": "ALL_MEMBERS",
  "participants": [ ... ]
}
```

**Response 403 (Forbidden):**
```json
{ "error": "You are not allowed to add members to this chat" }
```

**Response 404 (Not Found):**
```json
{ "error": "User not found" }
```

**Response 409 (Conflict):**
```json
{ "error": "User is already a member" }
```

**Hinweise:**
- Wer hinzufuegen darf haengt von `memberAddPolicy` ab:
  - `OWNER_ONLY`: nur `ownerId` des Chats
  - `ALL_MEMBERS`: jedes bestehende Mitglied
- Nur fuer GROUP-Chats

---

### DELETE /api/chats/{chatId}/members/{userId}

Mitglied aus einer Gruppe entfernen (nur durch Owner).

**Response 204 (No Content)** oder **200 (OK)** mit aktualisiertem Chat.

**Response 403 (Forbidden):**
```json
{ "error": "Only the group owner can remove members" }
```

**Response 404 (Not Found):**
```json
{ "error": "Member not found in this chat" }
```

**Hinweise:**
- Der Owner kann sich selbst NICHT ueber diesen Endpoint entfernen - dafuer `POST /api/chats/{chatId}/leave` nutzen
- Nur fuer GROUP-Chats

---

### DELETE /api/chats/{chatId}/leave

Gruppe als aktueller User verlassen.

**Response 204 (No Content)** oder **200 (OK)**.

**Response 403 (Forbidden):**
```json
{ "error": "Cannot leave a DM chat" }
```

**Response 404 (Not Found):**
```json
{ "error": "Chat not found" }
```

**Hinweise:**
- Wenn der verlassende User der Owner ist und weitere Mitglieder existieren:
  Empfehlung: Owner-Rolle an das aelteste Mitglied weiterreichen
- Wenn nach dem Verlassen keine Mitglieder mehr uebrig sind: Chat und dessen Nachrichten loeschen
- Fuer DM-Chats: `403` (DMs werden nicht "verlassen")

---

## Messages

Alle Message-Endpoints sind authentifiziert.
Der Server muss pruefen ob der aktuelle User Teilnehmer des Chats ist.

### GET /api/chats/{chatId}/messages

Nachrichten eines Chats abrufen.

**Query Parameter:**

| Parameter | Typ    | Pflicht | Default | Beschreibung                                    |
|-----------|--------|---------|---------|--------------------------------------------------|
| afterId   | long   | nein    | -       | Nur Nachrichten mit `id > afterId` (fuer Polling) |
| limit     | int    | nein    | 50      | Max. Anzahl Nachrichten (max 200)                |

**Response 200 (OK) - sortiert nach `createdAt` ASC:**
```json
[
  {
    "id": 56,
    "chatId": 10,
    "transmitterId": 1,
    "content": "I am fine, thanks!",
    "createdAt": "2026-04-14T09:31:00Z"
  },
  {
    "id": 57,
    "chatId": 10,
    "transmitterId": 2,
    "content": "Great to hear!",
    "createdAt": "2026-04-14T09:32:00Z"
  }
]
```

**Response 403 (Forbidden):**
```json
{ "error": "Not a participant of this chat" }
```

**Hinweise:**
- Ohne `afterId` werden die neuesten `limit` Nachrichten zurueckgegeben
- Mit `afterId` werden nur Nachrichten mit `id > afterId` zurueckgegeben (Client pollt alle 2 Sekunden)
- Sortierung immer `createdAt ASC` (aelteste zuerst)

---

### POST /api/chats/{chatId}/messages

Neue Nachricht in einen Chat senden.

**Request:**
```json
{
  "content": "Hello!"
}
```

**Response 201 (Created):**
```json
{
  "id": 57,
  "chatId": 10,
  "transmitterId": 1,
  "content": "Hello!",
  "createdAt": "2026-04-14T09:32:00Z"
}
```

**Response 403 (Forbidden):**
```json
{ "error": "Not a participant of this chat" }
```

**Response 400 (Bad Request):**
```json
{ "error": "Content cannot be empty" }
```

**Hinweise:**
- `transmitterId` wird vom Server aus dem JWT gesetzt, nicht vom Client
- `createdAt` wird vom Server gesetzt (Serverzeit)
- Content darf nicht leer sein und nicht nur Whitespace enthalten

---

## Error-Format

Alle Fehler-Responses folgen diesem einheitlichen Format:

```json
{
  "error": "Human-readable error message"
}
```

HTTP Status Codes:
- `200` - OK
- `201` - Created
- `204` - No Content (z.B. nach DELETE)
- `400` - Bad Request (Validierungsfehler)
- `401` - Unauthorized (nicht angemeldet / Token abgelaufen)
- `403` - Forbidden (kein Zugriff / keine Berechtigung)
- `404` - Not Found
- `409` - Conflict (z.B. Username schon vergeben, User schon im Chat)
- `500` - Internal Server Error

---

## Endpoint-Uebersicht

| Methode | Pfad                                       | Auth | Zweck                              |
|---------|--------------------------------------------|------|------------------------------------|
| POST    | /api/auth/register                         | nein | Registrieren                       |
| POST    | /api/auth/login                            | nein | Anmelden                           |
| POST    | /api/auth/refresh                          | nein | Token erneuern                     |
| GET     | /api/users                                 | ja   | Alle User auflisten                |
| GET     | /api/users/search?q=...                    | ja   | User suchen                        |
| GET     | /api/users/me                              | ja   | Aktuellen User abrufen             |
| PUT     | /api/users/me                              | ja   | Profil aktualisieren               |
| GET     | /api/users/{id}                            | ja   | Einzelnen User abrufen             |
| GET     | /api/chats                                 | ja   | Eigene Chat-Liste                  |
| POST    | /api/chats/dm                              | ja   | DM erstellen/finden                |
| POST    | /api/chats/group                           | ja   | Gruppe erstellen                   |
| PUT     | /api/chats/{chatId}                        | ja   | Gruppe aktualisieren (Owner)       |
| POST    | /api/chats/{chatId}/members                | ja   | Mitglied hinzufuegen               |
| DELETE  | /api/chats/{chatId}/members/{userId}       | ja   | Mitglied entfernen (Owner)         |
| DELETE  | /api/chats/{chatId}/leave                  | ja   | Gruppe verlassen                   |
| GET     | /api/chats/{chatId}/messages               | ja   | Nachrichten abrufen / pollen       |
| POST    | /api/chats/{chatId}/messages               | ja   | Nachricht senden                   |
| GET     | /api/workspaces                            | ja   | Eigene Workspaces auflisten        |
| POST    | /api/workspaces                            | ja   | Workspace erstellen                |
| GET     | /api/workspaces/{id}                       | ja   | Workspace-Details                  |
| PUT     | /api/workspaces/{id}                       | ja   | Workspace umbenennen (Owner)       |
| POST    | /api/workspaces/join                       | ja   | Workspace per Invite-Code beitreten|
| GET     | /api/workspaces/{id}/members               | ja   | Workspace-Mitglieder auflisten     |
| DELETE  | /api/workspaces/{id}/members/{userId}      | ja   | Mitglied entfernen (Kick/Leave)    |
| GET     | /api/workspaces/{id}/groups                | ja   | Gruppen eines Workspaces auflisten |
| POST    | /api/workspaces/{id}/groups                | ja   | Gruppe erstellen (Owner)           |
| PUT     | /api/workspaces/{id}/groups/{gid}          | ja   | Gruppe umbenennen (Owner)          |
| DELETE  | /api/workspaces/{id}/groups/{gid}          | ja   | Gruppe loeschen (Owner)            |
| ~~DELETE~~ | ~~/api/workspaces/{id}~~                | —    | Reserved (Workspace-Delete)        |
| ~~POST~~   | ~~/api/workspaces/{id}/members~~        | —    | Reserved (direct-add by userId)    |
| ~~PUT~~    | ~~/api/workspaces/{id}/members/{userId}~~ | —  | Reserved (Rollenwechsel, ADMIN)    |

---

## Datenbank Schema

Gelebtes Schema nach Phase 2b (MySQL, `schema.sql` + `spring.sql.init.mode=always`,
`CREATE TABLE IF NOT EXISTS`). Tabellennamen sind Plural.

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    online BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(3) NOT NULL
);

CREATE TABLE workspaces (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NULL,                           -- FK SET NULL bei User-Delete (Orphan-Risiko, siehe HANDOVER)
    invite_code CHAR(8) NOT NULL UNIQUE,            -- Alphabet [A-HJ-NP-Z2-9], serverseitig erzeugt
    is_personal BOOLEAN NOT NULL DEFAULT FALSE,     -- true fuer den beim register() angelegten Workspace
    created_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_workspaces_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE workspace_members (
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,                      -- 'OWNER' | 'MEMBER' (ADMIN weiterhin reserved)
    joined_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (workspace_id, user_id),
    INDEX idx_workspace_members_user_id (user_id),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user      FOREIGN KEY (user_id)      REFERENCES users(id)      ON DELETE CASCADE
);

CREATE TABLE workspace_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL,
    INDEX idx_workspace_groups_workspace_id (workspace_id, sort_order),
    CONSTRAINT fk_workspace_groups_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE TABLE chats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(10) NOT NULL,                      -- 'DM' | 'GROUP'
    name VARCHAR(100) NULL,                         -- nur bei GROUP
    owner_id BIGINT NULL,                           -- nur bei GROUP (FK SET NULL)
    member_add_policy VARCHAR(20) NULL,             -- 'OWNER_ONLY' | 'ALL_MEMBERS' (nur GROUP)
    workspace_id BIGINT NULL,                       -- nur GROUP, ab Phase 2b; DMs immer NULL
    group_id BIGINT NULL,                           -- nur GROUP, ab Phase 2c; NULL = "Allgemein"
    created_at TIMESTAMP(3) NOT NULL,
    INDEX idx_chats_workspace_id (workspace_id),
    INDEX idx_chats_group_id (group_id),
    CONSTRAINT fk_chats_owner     FOREIGN KEY (owner_id)     REFERENCES users(id)            ON DELETE SET NULL,
    CONSTRAINT fk_chats_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id)       ON DELETE SET NULL,
    CONSTRAINT fk_chats_group     FOREIGN KEY (group_id)     REFERENCES workspace_groups(id) ON DELETE SET NULL
);

CREATE TABLE chat_participants (
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (chat_id, user_id),
    CONSTRAINT fk_chat_participants_chat FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_participants_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE dm_pairs (
    chat_id BIGINT PRIMARY KEY,                     -- ein Eintrag pro DM-Chat
    user1_id BIGINT NOT NULL,                       -- immer < user2_id (serverseitig normalisiert)
    user2_id BIGINT NOT NULL,
    UNIQUE KEY uk_dm_pair (user1_id, user2_id),
    CONSTRAINT fk_dm_pairs_chat  FOREIGN KEY (chat_id)  REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_pairs_user1 FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_pairs_user2 FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    transmitter_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL,
    INDEX idx_messages_chat_id (chat_id, id),
    CONSTRAINT fk_messages_chat        FOREIGN KEY (chat_id)        REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_transmitter FOREIGN KEY (transmitter_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    token VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Phase 2c Migration (additiv):** `workspace_groups` ist eine neue Tabelle,
`chats.group_id` ist eine neue nullable Spalte mit FK `ON DELETE SET NULL`.
Beides kann live gegen eine bestehende 2b-DB additiv ausgerollt werden; alle
Pre-2c-Chats bleiben mit `group_id = NULL` gueltig und erscheinen in der
impliziten "Allgemein"-Section.

```sql
-- Phase 2c — additive migration vs. 2b:
ALTER TABLE chats ADD COLUMN group_id BIGINT NULL,
    ADD INDEX idx_chats_group_id (group_id),
    ADD CONSTRAINT fk_chats_group FOREIGN KEY (group_id)
        REFERENCES workspace_groups(id) ON DELETE SET NULL;
-- workspace_groups selbst wird via CREATE TABLE IF NOT EXISTS angelegt
-- (siehe Schema-Block oben).
```

---

## JWT Token Struktur

Das JWT sollte mindestens folgende Claims enthalten:

```json
{
  "sub": "1",
  "username": "alice",
  "iat": 1713088800,
  "exp": 1713092400
}
```

- `sub`: User-ID als String
- `username`: Benutzername
- `iat`: Issued-at Timestamp
- `exp`: Expiration Timestamp (empfohlen: 1 Stunde)
- Refresh-Token Gueltigkeitsdauer: empfohlen 30 Tage

---

## Implementation Notes (Stand 2026-04-24, Phase 2c)

**Implementiert (Phase 2a.5 + 2b + 2c):**
- Auth, Users (inkl. `/search` und `PUT /me`), DMs, Messages.
- Alle 5 Gruppenchat-Endpoints (`POST /api/chats/group`, `PUT /api/chats/{chatId}`,
  `POST /api/chats/{chatId}/members`, `DELETE /api/chats/{chatId}/members/{userId}`,
  `DELETE /api/chats/{chatId}/leave`).
- **Workspaces MVP:** `GET /api/workspaces`, `POST /api/workspaces`,
  `GET /{id}`, `PUT /{id}`, `POST /api/workspaces/join`, `GET /{id}/members`,
  `DELETE /{id}/members/{userId}`.
- **Register-Hook:** `POST /api/auth/register` legt nach dem User-Insert im
  selben `@Transactional`-Block einen persoenlichen Workspace (`Persoenlich`,
  `isPersonal = true`) an. Invariant: jeder User hat >= 1 Workspace.
- **Gruppenchats haengen am Workspace:** `chats.workspace_id` wird befuellt
  (nullable in der DB fuer DMs + Legacy-Fallback). `POST /api/chats/group`
  akzeptiert `workspaceId` optional; fehlt es, faellt der Server auf den
  persoenlichen Workspace des Callers zurueck und loggt WARN.
- **Workspace-Gruppen (Ordner, Phase 2c):** Alle 4 CRUD-Endpoints
  (`GET/POST /api/workspaces/{id}/groups`, `PUT/DELETE /api/workspaces/{id}/groups/{gid}`),
  neue Tabelle `workspace_groups`, neue Spalte `chats.group_id` (nullable, FK
  SET NULL). `POST /api/chats/group` nimmt optional `groupId` entgegen; wenn
  gesetzt muss die Gruppe zum angegebenen Workspace gehoeren (400 sonst).
  Delete einer Gruppe ist **nicht-kaskadierend** — enthaltene Chats ueberleben
  mit `group_id = NULL` und fallen in "Allgemein". Edit-Rechte nur `OWNER`;
  Permissioning auf Chat-Ebene unveraendert (kein Permissioning per Gruppe).

**Reserved fuer spaeter:**
- Direct-Add (`POST /{id}/members` mit userId), Rollenwechsel
  (`PUT /{id}/members/{userId}`), Rolle `ADMIN`, Workspace-Delete,
  Owner-Handover, Invite-Code-Rotation.
- Gruppen-Reorder (drag-to-sort): `sort_order` in DB vorhanden, aber kein
  dedicated PATCH-Endpoint; UI zeigt sie in Insertion-Order.

**Design-Entscheidungen Phase 2b (verkuerzt):**
- **Rollen im MVP:** `OWNER` + `MEMBER`. `ADMIN` erst wenn echter Bedarf da ist.
- **Invite-Code-Alphabet:** `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (32 Zeichen,
  ohne `I O 0 1` — confusable-free). 8 Zeichen = 32^8 ≈ 1.1 * 10^12, Kollision
  praktisch irrelevant; Code wird via `SecureRandom` erzeugt, beim Insert
  Unique-Violation → bis zu 5 Neuversuche.
- **`workspaces.owner_id` nullable + `ON DELETE SET NULL`:** konsistent mit
  `chats.owner_id`. Erzeugt Orphan-Workspaces wenn der Owner geloescht wird —
  bekannt, siehe HANDOVER.md.
- **Kein Owner-Handover im MVP:** Owner-Leave → 400. Wenn der Owner den
  Workspace verlassen will, muss er (spaeter) loeschen. Persoenlicher
  Workspace laesst sich gar nicht verlassen (400).

---

## Changelog

- **2026-04-24 (Phase 2c)**: Workspace-Gruppen (Ordner) live. Neue Tabelle
  `workspace_groups` (id, workspace_id, name, sort_order, created_at). Neue
  Spalte `chats.group_id` (nullable, FK SET NULL). Vier neue Endpoints:
  `GET/POST /api/workspaces/{id}/groups`, `PUT/DELETE /api/workspaces/{id}/groups/{gid}` —
  alle Owner-only. `POST /api/chats/group` nimmt optional `groupId` entgegen
  (muss zum Workspace gehoeren, 400 sonst). `Chat`-Payload enthaelt `groupId`
  bei GROUP-Chats; DMs unveraendert (NON_NULL-Suppression). Delete einer
  Gruppe ist nicht-kaskadierend (Chats bleiben, fallen in die implizite
  "Allgemein"-Section). Migration gegen 2b-DB additiv (ALTER TABLE chats
  + CREATE TABLE workspace_groups). Stand: Client kompiliert,
  **nicht end-to-end smoke-getestet** (siehe HANDOVER.md).
- **2026-04-21 (Phase 2b)**: Workspaces-MVP live. Neue Tabellen `workspaces`
  und `workspace_members`. Neues Feld `chats.workspace_id` (nullable, FK
  SET NULL). `POST /api/auth/register` legt einen persoenlichen Workspace
  (`isPersonal = true`) an. Neue Endpoints: `GET/POST /api/workspaces`,
  `GET/PUT /api/workspaces/{id}`, `POST /api/workspaces/join`,
  `GET /api/workspaces/{id}/members`, `DELETE /api/workspaces/{id}/members/{userId}`.
  Rollen: `OWNER` + `MEMBER` (kein `ADMIN` im MVP). Invite-Code 8 Zeichen,
  confusable-frei. `POST /api/chats/group` nimmt `workspaceId` entgegen
  (optional, faellt auf persoenlichen Workspace + WARN zurueck); Member-List
  muss zum Ziel-Workspace gehoeren (400 sonst). Stand: kompiliert,
  **nicht end-to-end smoke-getestet** (siehe HANDOVER.md). Aus der Spec
  entfallen sind `Workspace.slug` und `Workspace.description`; die Sektion
  "Workspace-Gruppen" ist als Reserved / Phase 2c markiert.
- **2026-04-21 (Phase 2a.5)**: Backend-Paritaet — die 5 Gruppenchat-Endpoints
  (`POST /group`, `PUT /{chatId}`, `POST /{chatId}/members`,
  `DELETE /{chatId}/members/{userId}`, `DELETE /{chatId}/leave`) sind jetzt
  tatsaechlich im Backend implementiert (vorher nur in dieser Spec). Stand:
  kompiliert, **noch nicht end-to-end smoke-getestet** (siehe HANDOVER.md
  "Bekannte Risiken"). Schema-Migration via Drop+Recreate der Dev-DB; neuen
  Spalten an `chats` sind additive.
- **2026-04-17**: Workspaces (`/api/workspaces` mit Rollen OWNER/ADMIN/MEMBER) und Workspace-Gruppen als Ordner im Workspace (`/api/workspaces/{id}/groups`); `Chat` um optionale `workspaceId` + `groupId` erweitert, damit DMs/Gruppen in der Sidebar nach Workspace und Ordner gebuendelt werden koennen.
- **2026-04-15**: Gruppen-Chats (`POST/PUT /api/chats/group`, Member-Management, Leave), User-Suche (`/api/users/search`), Profil-Update (`PUT /api/users/me`), `avatarUrl` + `online` am User, `name`/`ownerId`/`memberAddPolicy` am Chat.
