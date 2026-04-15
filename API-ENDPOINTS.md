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

## Chats

Alle Chat-Endpoints sind authentifiziert.

Chats koennen zwei Typen haben:
- **`DM`** - Direktnachricht zwischen genau zwei Usern
- **`GROUP`** - Gruppenchat mit Name, Owner und beliebig vielen Mitgliedern

Das `Chat`-Objekt hat folgende Felder:

| Feld              | Typ                             | Beschreibung                                  |
|-------------------|---------------------------------|-----------------------------------------------|
| id                | long                            | Primary Key                                   |
| type              | `"DM"` \| `"GROUP"`             | Chat-Typ                                      |
| name              | string \| null                  | Nur bei GROUP, bei DM immer `null`            |
| ownerId           | long \| null                    | Nur bei GROUP: User der die Gruppe erstellt hat |
| memberAddPolicy   | `"OWNER_ONLY"` \| `"ALL_MEMBERS"` \| null | Nur bei GROUP: wer darf Mitglieder hinzufuegen |
| createdAt         | ISO 8601 string                 |                                               |
| participants      | User[]                          | Alle Teilnehmer (inkl. aktuellem User)        |
| lastMessage       | Message \| null                 | Letzte Nachricht (oder `null`)                |

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
  "memberIds": [2, 3, 4]
}
```

| Feld            | Typ                                       | Pflicht | Beschreibung                                      |
|-----------------|-------------------------------------------|---------|---------------------------------------------------|
| name            | string                                    | ja      | 1-100 Zeichen                                     |
| memberAddPolicy | `"OWNER_ONLY"` \| `"ALL_MEMBERS"`         | ja      | Wer darf neue Mitglieder hinzufuegen              |
| memberIds       | long[]                                    | ja      | Initiale Mitglieder (ohne Owner, mind. 1 Eintrag) |

**Response 201 (Created):**
```json
{
  "id": 20,
  "type": "GROUP",
  "name": "SWP Team",
  "ownerId": 1,
  "memberAddPolicy": "ALL_MEMBERS",
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
```json
{ "error": "Group name must not be empty" }
```

**Response 404 (Not Found):**
```json
{ "error": "One or more users not found" }
```

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

---

## Datenbank Schema

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    online BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(10) NOT NULL DEFAULT 'DM',        -- 'DM' | 'GROUP'
    name VARCHAR(100),                              -- nur bei GROUP
    owner_id BIGINT,                                -- nur bei GROUP
    member_add_policy VARCHAR(20),                  -- 'OWNER_ONLY' | 'ALL_MEMBERS'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES user(id)
);

CREATE TABLE chat_participant (
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES chat(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    transmitter_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat(id) ON DELETE CASCADE,
    FOREIGN KEY (transmitter_id) REFERENCES user(id)
);

-- Index fuer schnelles Polling
CREATE INDEX idx_message_chat_id ON message(chat_id, id);
-- Index fuer User-Suche
CREATE INDEX idx_user_username_lower ON user((LOWER(username)));
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

## Changelog

- **2026-04-15**: Gruppen-Chats (`POST/PUT /api/chats/group`, Member-Management, Leave), User-Suche (`/api/users/search`), Profil-Update (`PUT /api/users/me`), `avatarUrl` + `online` am User, `name`/`ownerId`/`memberAddPolicy` am Chat.
