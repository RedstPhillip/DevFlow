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

### GET /api/users

Alle registrierten Benutzer auflisten (ausser dem aktuellen User).

**Response 200 (OK):**
```json
[
  {
    "id": 1,
    "username": "alice",
    "createdAt": "2026-04-10T08:00:00Z"
  },
  {
    "id": 2,
    "username": "bob",
    "createdAt": "2026-04-11T09:00:00Z"
  }
]
```

---

### GET /api/users/{id}

Einzelnen Benutzer nach ID abrufen.

**Response 200 (OK):**
```json
{
  "id": 1,
  "username": "alice",
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
  "createdAt": "2026-04-10T08:00:00Z"
}
```

---

## Chats

Alle Chat-Endpoints sind authentifiziert.

### GET /api/chats

Alle Chats des aktuellen Users abrufen, sortiert nach letzter Nachricht (neueste zuerst).

**Response 200 (OK):**
```json
[
  {
    "id": 10,
    "type": "DM",
    "createdAt": "2026-04-12T10:00:00Z",
    "participants": [
      { "id": 1, "username": "alice" },
      { "id": 2, "username": "bob" }
    ],
    "lastMessage": {
      "id": 55,
      "content": "Hey, how are you?",
      "transmitterId": 2,
      "createdAt": "2026-04-14T09:30:00Z"
    }
  }
]
```

**Hinweise:**
- `lastMessage` kann `null` sein wenn der Chat keine Nachrichten hat
- `participants` enthaelt immer beide User des DM-Chats
- Sortierung: Chats mit neuester `lastMessage.createdAt` zuerst

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
- `type` ist immer `"DM"` fuer Stufe 1
- Beide `Chat_Participant` Eintraege werden automatisch erstellt

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
- `400` - Bad Request (Validierungsfehler)
- `401` - Unauthorized (nicht angemeldet / Token abgelaufen)
- `403` - Forbidden (kein Zugriff auf diese Ressource)
- `404` - Not Found
- `409` - Conflict (z.B. Username schon vergeben)
- `500` - Internal Server Error

---

## Datenbank Schema

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(10) NOT NULL DEFAULT 'DM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_participant (
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (chat_id, user_id),
    FOREIGN KEY (chat_id) REFERENCES chat(id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chat_id BIGINT NOT NULL,
    transmitter_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat(id),
    FOREIGN KEY (transmitter_id) REFERENCES user(id)
);

-- Index fuer schnelles Polling
CREATE INDEX idx_message_chat_id ON message(chat_id, id);
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
