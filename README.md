# PulseDesk — Comment-to-Ticket Triage

IBM Internship Technical Challenge.

A Spring Boot backend application that collects user comments from different channels, analyzes them using the **HuggingFace Inference API**, and automatically generates structured support tickets for actionable feedback. Includes a simple web UI as a bonus.

---

## What It Does

- User submits a comment (bug report, feature request, billing issue, compliment, etc.)
- The app sends the comment to a HuggingFace AI model for analysis
- The AI decides whether the comment needs a support ticket
- If yes — a ticket is generated with a title, category, priority, and summary
- If no (e.g. a compliment) — the comment is stored but no ticket is created
- All comments and tickets are stored in an embedded H2 database
- A simple web UI lets you submit comments and view results at `http://localhost:8080`

---

## Tech Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Data JPA + H2** (in-memory database)
- **HuggingFace Inference API** (Qwen/Qwen2.5-72B-Instruct)
- **Lombok**
- **JUnit 5 + Mockito** (unit tests)

---

## Project Structure

```
src/main/java/ticket/exercise/pulsedesk/
├── controller/          # REST endpoints
│   ├── CommentController.java
│   └── TicketController.java
├── service/             # Business logic interfaces
│   ├── AiAnalysisService.java
│   ├── CommentService.java
│   ├── TicketService.java
│   └── impl/
│       ├── HuggingFaceAnalysisService.java
│       ├── CommentServiceImpl.java
│       └── TicketServiceImpl.java
├── model/
│   ├── entity/          # JPA entities (Comment, Ticket)
│   ├── enums/           # Category, Priority
│   └── dto/             # Request / Response DTOs
├── mapper/              # Entity <-> DTO conversion
├── repository/          # Spring Data JPA repositories
├── exception/           # Custom exceptions + GlobalExceptionHandler
└── config/              # AppConfig (ObjectMapper, CORS)

src/main/resources/
├── application.properties.example   # Config template (copy and fill in token)
└── static/
    └── index.html                   # Simple web UI

src/test/java/ticket/exercise/pulsedesk/
├── CommentServiceTest.java
├── TicketServiceTest.java
├── CommentMapperTest.java
├── TicketMapperTest.java
└── GlobalExceptionHandlerTest.java
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- A free [HuggingFace](https://huggingface.co) account and API token

---

## Setup & Running

### 1. Clone the repository
```bash
git clone https://github.com/YOUR_USERNAME/pulsedesk.git
cd pulsedesk
```

### 2. Configure your HuggingFace token

Copy the example config file:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Open `application.properties` and replace the token:
```properties
huggingface.api.token=YOUR_TOKEN_HERE
```

Get your token at: https://huggingface.co/settings/tokens (Read access is enough)

### 3. Build and run
```bash
mvn clean install
mvn spring-boot:run
```

The server starts at **http://localhost:8080**

### 4. Open the UI

Go to **http://localhost:8080** in your browser to use the web interface.

---

## API Reference

### POST /comments
Submit a comment for AI analysis.

**Request body:**
```json
{
  "content": "I can't log in after the last update, the page just freezes.",
  "source": "web-form"
}
```

**Response — ticket created:**
```json
{
  "success": true,
  "message": "Comment submitted and converted to ticket #1",
  "data": {
    "id": 1,
    "content": "I can't log in after the last update...",
    "source": "web-form",
    "analyzed": true,
    "convertedToTicket": true,
    "ticketId": 1,
    "createdAt": "2025-03-08T10:30:00"
  }
}
```

**Response — no ticket needed:**
```json
{
  "success": true,
  "message": "Comment submitted. No action required.",
  "data": {
    "id": 2,
    "analyzed": true,
    "convertedToTicket": false,
    "ticketId": null
  }
}
```

---

### GET /comments
Returns all submitted comments ordered by most recent.

### GET /comments/{id}
Returns a single comment by ID.

---

### GET /tickets
Returns all generated tickets.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Login page freezes after update",
      "category": "BUG",
      "priority": "HIGH",
      "summary": "User cannot log in due to page freeze after recent update.",
      "createdAt": "2025-03-08T10:30:00",
      "commentId": 1,
      "originalComment": "I can't log in after the last update..."
    }
  ]
}
```

### GET /tickets/{ticketId}
Returns a single ticket by ID.

---

## Triage Logic

A comment is converted to a ticket if it contains any actionable request — bugs, errors, billing issues, account problems, or feature requests. Pure compliments and non-actionable praise are not converted to tickets.

This ensures all 5 supported categories are reachable:

| Category | Example |
|---|---|
| `BUG` | "The login button doesn't work" |
| `FEATURE` | "It would be great to export data to CSV" |
| `BILLING` | "I was charged twice this month" |
| `ACCOUNT` | "I can't reset my password" |
| `OTHER` | Any other actionable issue |

Priority is set to `LOW`, `MEDIUM`, or `HIGH` based on the AI's assessment of urgency.

---

## H2 Database Console

Useful for inspecting stored data during development:

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:pulsedeskdb
Username: sa
Password: (leave empty)
```

Note: the database is in-memory — all data is cleared on restart.

---

## Running Tests

```bash
mvn test
```

Tests cover the service layer, mappers, and exception handler — all using mocks, with no real API calls or database required.

---

## Error Handling

All errors return a consistent JSON envelope:

```json
{
  "success": false,
  "message": "Comment not found with id: 99",
  "timestamp": "2025-03-08T10:30:00"
}
```

| Scenario | HTTP Status |
|---|---|
| Comment or ticket not found | 404 Not Found |
| Invalid request body | 400 Bad Request |
| HuggingFace API failure | 503 Service Unavailable |
| Unexpected server error | 500 Internal Server Error |

---

## Notes

- `application.properties` is excluded from version control via `.gitignore` to protect the API token. Use `application.properties.example` as a setup template.
- The AI model used is `Qwen/Qwen2.5-72B-Instruct` via the HuggingFace router. This can be changed in `application.properties` by updating `huggingface.api.model`.
- The `AiAnalysisService` is an interface — swapping the AI provider requires no changes to business logic.
