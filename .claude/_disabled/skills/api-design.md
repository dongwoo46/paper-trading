Skill: API Design (REST)

## URL Design

- Use resource-centric nouns: `/orders`, `/accounts`, `/positions`.
- No verbs: `/createOrder` ✗ → `POST /orders` ✓
- Express hierarchy: `/accounts/{id}/orders`, `/orders/{id}/fills`.
- Plural nouns: `/order` ✗ → `/orders` ✓
- kebab-case: `/market-data` ✓, `/marketData` ✗

## HTTP Methods

- `GET`: Read (idempotent, no side effects).
- `POST`: Create.
- `PUT`: Full update (idempotent).
- `PATCH`: Partial update.
- `DELETE`: Delete.

## Status Codes

- `200 OK`: Successful read or update.
- `201 Created`: Successful creation (include `Location` header).
- `204 No Content`: Successful deletion.
- `400 Bad Request`: Input validation error.
- `401 Unauthorized`: Authentication required.
- `403 Forbidden`: Insufficient permissions.
- `404 Not Found`: Resource does not exist.
- `409 Conflict`: Business rule violation (insufficient balance, duplicate, etc.).
- `500 Internal Server Error`: Server-side error.

## Error Response Format

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "Insufficient balance.",
  "detail": { "required": 100000, "available": 50000 }
}
```

## Request / Response Rules

- Request: only the fields that are needed. Do not expose unnecessary fields.
- Response: only the fields the client actually uses.
- Pagination: prefer cursor-based (offset has performance issues at scale).
- Date/time: ISO 8601 (`2025-04-16T10:30:00Z`).
- Money: string or integer (smallest currency unit). No `float`.

## Versioning

- URL versioning: `/api/v1/orders`.
- Backwards compatibility: adding fields is non-breaking; removing or renaming requires a new version.

## Internal APIs

- Inter-service calls: `/api/internal/{resource}`.
- Never expose externally. Use separate authentication.