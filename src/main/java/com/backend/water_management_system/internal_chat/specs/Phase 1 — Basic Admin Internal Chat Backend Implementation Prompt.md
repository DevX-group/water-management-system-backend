# Phase 1 — Basic Admin Internal Chat Backend Implementation

## 1. Role and objective

You are working inside an existing Spring Boot + PostgreSQL water bill management system.

Implement **Phase 1 — Basic Internal Chat** for communication between authorized internal staff users.

Do NOT redesign or rewrite unrelated parts of the application.

Do NOT implement the frontend.

Do NOT implement Phase 2 features such as:

- group conversations
- file attachments
- images/files
- message editing
- message deletion
- typing indicators
- online/offline presence
- last-seen status
- message search
- external/customer messaging
- notifications module
- customer messaging
- MCP
- PWA

Only implement the backend required for Phase 1.

---

# 2. VERY IMPORTANT: Existing roles

The future business requirements will eventually change the admin roles, but those future roles have NOT been implemented yet.

Therefore, use the CURRENT `Role` enum exactly as it exists.

Current roles are:

```java
CUSTOMER
SUPER_ADMIN
SYSTEM_ADMIN
PAYMENT_HANDLER
METER_READER
```

For Phase 1 Internal Chat:

### Eligible internal-chat roles

```text
SUPER_ADMIN
SYSTEM_ADMIN
PAYMENT_HANDLER
METER_READER
```

### Not eligible

```text
CUSTOMER
```

Do NOT introduce future roles such as `CUSTOMER_HANDLER` into this implementation.

The role names should be centralized/reused from the existing `Role` enum rather than duplicated as strings throughout the implementation.

The role structure may be changed by the project team later, so avoid unnecessarily coupling the chat domain to assumptions about future role hierarchy.

---

# 3. IMPORTANT package separation

A package named:

```text
com.backend.water_management_system.internal_chat
```

already exists with these subpackages:

```text
entity
controller
service
repository
dto
config
enums
exceptions
```

Use this package for the entire internal-chat implementation.

Use only the subpackages that are actually necessary.

There is ALSO an existing package named:

```text
messaging
```

That package is for a completely different feature:

> customer messaging

DO NOT mix the internal chat implementation with the existing `messaging` package.

Do not move, rename, reuse, or modify customer messaging classes unless absolutely required by an existing shared infrastructure dependency.

The new implementation must clearly remain under:

```text
internal_chat
```

---

# 4. Existing authentication architecture

The application currently uses:

- Spring Security
- stateless JWT authentication
- `JwtAuthenticationFilter`
- `JwtService`
- `CustomUserDetailsService`
- `UserPrincipal`
- `@PreAuthorize`
- `@EnableMethodSecurity`

The existing `User` entity is:

```text
users
```

with:

```text
UUID id
String nic
String fullName
String email
String phoneNumber
String passwordHash
Role role
UserStatus status
LocalDateTime createdAt
LocalDateTime updatedAt
```

The user's UUID is the database ID.

The NIC is the login identifier.

The JWT subject is the NIC.

The JWT also contains the role.

The existing JWT authentication flow must NOT be replaced.

Reuse the existing authentication infrastructure wherever possible.

---

# 5. IMPORTANT: WebSocket authentication

The existing HTTP JWT filter authenticates normal HTTP requests using:

```http
Authorization: Bearer <JWT>
```

However, do NOT assume that the existing `JwtAuthenticationFilter` automatically authenticates STOMP/WebSocket messages.

The current HTTP security configuration permits:

```text
/ws/**
```

Therefore, implement explicit authentication for the STOMP connection/message channel using the existing:

```text
JwtService
CustomUserDetailsService
UserPrincipal
```

The frontend will be able to send the JWT in the STOMP CONNECT headers.

The WebSocket/STOMP authentication implementation must:

1. Receive the JWT from the STOMP CONNECT headers.
2. Validate the JWT using the existing `JwtService`.
3. Extract the NIC from the token.
4. Load the user using the existing `CustomUserDetailsService`.
5. Verify that the account is enabled and not locked.
6. Create an authenticated Spring Security `Authentication`.
7. Attach the authenticated principal to the STOMP/WebSocket session.
8. Make the authenticated user available to message-handling methods.
9. Reject unauthenticated WebSocket/STOMP connections.
10. Reject invalid/expired JWTs.

Do not create a second JWT implementation.

Do not create a second user authentication system.

Reuse the existing JWT infrastructure.

---

# 6. Authorization philosophy

REST authorization in the existing application is mainly handled using:

```java
@PreAuthorize(...)
```

because `@EnableMethodSecurity` is enabled.

Follow the existing project convention for REST controllers.

However, remember that WebSocket/STOMP message handling is different from normal REST controllers.

Do NOT assume that putting `@PreAuthorize` on a REST controller automatically protects STOMP messages.

For every chat operation, the backend must verify the authenticated user and their authorization.

Most importantly:

> A user must NEVER be able to access a conversation merely because they know its UUID.

Every conversation access must verify that the current user is a participant/member of that conversation.

---

# 7. Phase 1 functional scope

Implement the following:

## 7.1 Staff discovery

Authenticated internal-chat users must be able to search eligible internal staff.

Search must support:

- full name
- user ID

The user ID is the existing UUID `User.id`.

Do not search customers.

The endpoint must support filtering by role.

The frontend will have these tabs:

```text
All
Super Admin
System Admin
Payment Handler
Meter Reader
```

The current role names should be used because these are the roles currently implemented.

### All

Returns eligible users from:

```text
SUPER_ADMIN
SYSTEM_ADMIN
PAYMENT_HANDLER
METER_READER
```

and excludes:

```text
CUSTOMER
```

### Specific role tab

Returns only users belonging to that role.

### Search

Search should match either:

```text
fullName
```

or:

```text
id
```

The search should support partial matching where practical.

The current authenticated user should normally be excluded from the staff discovery results because a direct conversation with oneself is not needed.

Only appropriate user information should be returned.

Do NOT expose:

- passwordHash
- authentication secrets
- JWT information
- activation tokens
- unnecessary private data

---

# 8. Direct conversations only

Phase 1 supports ONE-TO-ONE conversations.

Do NOT implement group conversations.

A conversation has exactly two participants.

Example:

```text
Hansana
   |
   | conversation
   |
Nimal
```

A conversation must not contain three or more users in Phase 1.

---

# 9. Starting a conversation

An authenticated eligible internal user should be able to start a conversation with another eligible internal user.

The request should contain the target user's ID.

Before creating the conversation, the backend must verify:

1. Current user is authenticated.
2. Current user is eligible for internal chat.
3. Target user exists.
4. Target user is eligible for internal chat.
5. Target user is not the current user.
6. Target user's account is active/enabled as appropriate.
7. A direct conversation between the two users does not already exist.

If a conversation already exists:

> Return the existing conversation rather than creating a duplicate.

This is extremely important.

There should only be one direct conversation between two users.

For example, if:

```text
User A ↔ User B
```

already exists, another request to start a conversation between A and B must return the existing conversation.

The implementation should enforce this at the database/business-rule level as much as practical to prevent duplicate conversations caused by concurrent requests.

---

# 10. Conversation list

Authenticated internal users must be able to retrieve their conversations.

A conversation list item should contain enough information for the frontend to display something similar to:

```text
Nimal Perera
Please check meter 1045...
10:35
2 unread
```

The response should include information such as:

- conversation ID
- other participant's ID
- other participant's name
- other participant's role
- latest message preview
- latest message timestamp
- unread message count

Do not expose unnecessary information.

The current user's own identity should not be used as the displayed "other participant".

---

# 11. Role filtering of conversations

The conversation-list endpoint should support role filtering.

For example:

```text
GET /api/internal-chat/conversations
```

can support an optional role parameter.

Conceptually:

```text
role=SUPER_ADMIN
role=SYSTEM_ADMIN
role=PAYMENT_HANDLER
role=METER_READER
```

If no role is provided:

> return conversations with all eligible internal roles.

If a role is provided:

> return only conversations where the OTHER participant has that role.

This is important.

For example:

If Hansana has:

```text
Hansana ↔ Nimal
Nimal = SYSTEM_ADMIN
```

then:

```text
All
```

should contain the conversation.

```text
System Admin
```

should contain the conversation.

```text
Meter Reader
```

should NOT contain it.

---

# 12. Conversation search

The conversation list should also support searching by the other participant's:

- full name
- user ID

For example:

```text
GET /api/internal-chat/conversations?role=SYSTEM_ADMIN&search=abc
```

The search should apply to the other participant, not the current user.

Do not download all conversations and perform the main filtering entirely in Java/React if it can reasonably be performed through the database query.

Use appropriate repository queries.

---

# 13. Message persistence

Create a persistent message entity.

A message should contain at minimum:

```text
id
conversation
sender
content
createdAt
```

Use UUIDs consistently with the existing User ID strategy unless there is a strong project-specific reason not to.

The message content must be text only in Phase 1.

No attachments.

No binary data.

No files.

---

# 14. Conversation persistence

Create a conversation entity suitable for direct conversations.

It should contain at least:

```text
id
createdAt
updatedAt / lastMessageAt if useful
```

Use a separate participant/member entity if appropriate.

A recommended model is:

```text
Conversation
    |
    | 1
    |
    | *
ConversationParticipant
    |
    | *
    |
    | 1
User
```

Each direct conversation should have exactly two participants.

The participant entity should contain at least:

```text
id
conversation
user
joinedAt
lastReadAt
```

`lastReadAt` is important for Phase 1 unread-message calculation.

Do not introduce unnecessary complexity such as a separate MessageRead table unless the existing architecture strongly requires it.

---

# 15. Database constraints

Use database-level constraints where practical.

At minimum:

- a conversation participant should not be duplicated
- the same user should not be added twice to one conversation
- a direct conversation should not have more than two participants through application logic
- appropriate foreign keys should exist
- required fields should be non-null

Think carefully about preventing duplicate direct conversations between the same two users.

Do not rely only on frontend behavior to prevent duplicates.

---

# 16. Message retrieval

Provide an endpoint to retrieve messages belonging to a conversation.

Conceptually:

```text
GET /api/internal-chat/conversations/{conversationId}/messages
```

Before returning anything:

1. Authenticate the user.
2. Verify the user is an eligible internal-chat user.
3. Verify the user is a participant of the conversation.
4. Only then retrieve the messages.

A user who is not a participant must receive an appropriate authorization response.

Do NOT return another user's conversation merely because the conversation ID is known.

---

# 17. Pagination

Messages must be paginated.

Do not return the entire conversation history in one request.

Use Spring Data pagination where appropriate.

The API should support something equivalent to:

```text
page
size
```

The newest messages should be easily retrievable.

Choose a sensible default page size.

The implementation should make it possible for the React frontend to initially load the most recent messages and later retrieve older messages.

---

# 18. Real-time messaging architecture

Use:

```text
Spring WebSocket
+
STOMP
```

on the backend.

Do not use raw WebSocket messaging unless there is a compelling project-specific reason.

Add the required Maven dependencies only if they are not already present.

First inspect the existing `pom.xml`.

Do not add duplicate dependencies.

---

# 19. WebSocket endpoint

Create a WebSocket/STOMP configuration under:

```text
internal_chat.config
```

Use a sensible endpoint such as:

```text
/ws/internal-chat
```

or another clear endpoint under `/ws`.

The endpoint should support the existing frontend origin/CORS configuration appropriately.

Do not modify unrelated WebSocket/messaging infrastructure.

---

# 20. STOMP destination design

Use a clean destination structure.

For example:

```text
/app/internal-chat/send
```

for client-to-server message sending.

For conversation-specific broadcasts, use a destination conceptually similar to:

```text
/topic/internal-chat/conversation/{conversationId}
```

For user-specific events, use Spring's user destination mechanism where appropriate.

The exact destination names can be chosen by the implementation, but they must be:

- consistent
- documented
- clearly separated from the existing customer `messaging` feature

Do not use the existing `messaging` package's destinations.

---

# 21. Sending a message through STOMP

The intended flow is:

```text
React
  |
  | STOMP SEND
  v
Spring WebSocket Controller
  |
  v
Chat Service
  |
  | authenticate sender
  | verify membership
  | validate content
  |
  v
Message Repository
  |
  v
PostgreSQL
  |
  | successful save
  v
STOMP broker
  |
  v
recipient React
```

The message should be persisted before it is broadcast.

This is a critical requirement.

Do NOT broadcast a message first and persist it afterward.

---

# 22. Message validation

Before saving a message:

- conversation must exist
- sender must be authenticated
- sender must be a participant
- sender must be an eligible internal-chat user
- content must not be null
- content must not be blank
- apply a reasonable maximum message length

Do not silently accept invalid content.

Use the project's existing validation/error-handling conventions where possible.

---

# 23. WebSocket message security

The WebSocket layer must not trust:

```text
senderId
```

provided by the client.

The sender must be determined from the authenticated WebSocket principal.

For example:

```text
Authenticated WebSocket user
        ↓
UserPrincipal
        ↓
current User
        ↓
message.sender
```

If the client sends:

```json
{
  "senderId": "some-other-user"
}
```

the backend must ignore/reject that sender ID.

Ideally, the client should not need to send senderId at all.

The server determines the sender from authentication.

---

# 24. Conversation authorization

For every conversation operation:

```text
GET messages
SEND message
MARK read
GET conversation details
```

verify:

```text
currentUser is a participant
```

Do not rely only on:

```text
@PreAuthorize("isAuthenticated()")
```

because being authenticated does not mean the user is allowed to access every conversation.

---

# 25. Read/unread handling

Implement unread-message tracking using:

```text
ConversationParticipant.lastReadAt
```

When the user opens/reads a conversation, provide an operation such as:

```text
POST /api/internal-chat/conversations/{conversationId}/read
```

The backend should:

1. authenticate the current user
2. verify conversation membership
3. update that user's `lastReadAt`

Unread messages can then be determined using the current user's participant record.

Conceptually:

```text
message.createdAt > participant.lastReadAt
```

Messages sent by the current user should not normally be counted as unread for that user.

---

# 26. Real-time read status

Phase 1 should support read status sufficiently for the frontend to know that messages have been read.

When a user marks a conversation as read, consider broadcasting an appropriate read event to the other participant through STOMP.

The event should contain enough information for the sender's UI to update its read state.

Do not create an unnecessarily complicated per-message read model for Phase 1.

Use the two-person conversation model and `lastReadAt`.

---

# 27. REST API proposal

Implement APIs following the existing project's conventions.

A reasonable API structure is:

```text
GET    /api/internal-chat/users
GET    /api/internal-chat/conversations
POST   /api/internal-chat/conversations
GET    /api/internal-chat/conversations/{conversationId}
GET    /api/internal-chat/conversations/{conversationId}/messages
POST   /api/internal-chat/conversations/{conversationId}/read
```

For staff search:

```text
GET /api/internal-chat/users?role=SYSTEM_ADMIN&search=nimal
```

For conversations:

```text
GET /api/internal-chat/conversations?role=SYSTEM_ADMIN&search=nimal
```

For creating a conversation:

```text
POST /api/internal-chat/conversations
```

with the target user's ID.

For messages:

```text
GET /api/internal-chat/conversations/{conversationId}/messages?page=0&size=30
```

The exact DTO/request/response structures should follow the project's existing conventions.

Do not blindly copy these URLs if the existing project has a strong established API naming convention; inspect the existing controllers first and maintain consistency.

---

# 28. DTO requirements

Do not expose JPA entities directly from REST endpoints.

Create appropriate DTOs.

Possible DTOs include:

```text
InternalChatUserResponse
ConversationResponse
ConversationParticipantResponse
MessageResponse
CreateConversationRequest
SendMessageRequest
```

Use records if that matches the project's existing DTO style.

Do not expose:

```text
passwordHash
ActivationToken
security information
```

---

# 29. Repository requirements

Create repositories under:

```text
internal_chat.repository
```

Use Spring Data JPA.

Repository operations will likely be required for:

### Conversation

- find existing direct conversation between two users
- find conversations belonging to a user
- filter conversations by other participant role
- search by other participant name
- search by other participant UUID
- retrieve conversation by ID

### Participant

- find participant by conversation and user
- determine whether a user belongs to a conversation
- retrieve the current user's `lastReadAt`

### Message

- retrieve paginated messages for a conversation
- retrieve latest message
- count unread messages where appropriate

Prefer database-level queries rather than loading huge collections into memory.

---

# 30. Service layer

Business logic should reside in services, not controllers.

Create a service such as:

```text
InternalChatService
```

or an appropriate name consistent with the project.

It should handle:

- eligible-user search
- conversation creation/retrieval
- conversation authorization
- message creation
- message retrieval
- unread count
- marking conversations as read

The WebSocket controller should call the same service layer rather than implementing business logic itself.

---

# 31. REST controller

Create a controller under:

```text
internal_chat.controller
```

Use:

```java
@PreAuthorize(...)
```

where appropriate, following the existing project style.

The controller should obtain the current user through the existing security mechanism, preferably:

```java
@AuthenticationPrincipal UserPrincipal principal
```

if that works consistently with the existing implementation.

Do not create another authentication lookup mechanism.

---

# 32. WebSocket controller

Create a dedicated WebSocket/STOMP controller.

It should:

- receive STOMP messages
- obtain the authenticated principal
- delegate to the service layer
- publish/broadcast the resulting event

It must not:

- directly manipulate repositories
- trust senderId from the client
- bypass authorization
- duplicate business logic

---

# 33. WebSocket configuration

Create a dedicated configuration under:

```text
internal_chat.config
```

Configure:

- STOMP message broker
- application destination prefix
- user destination prefix if needed
- WebSocket endpoint
- STOMP authentication interceptor

The configuration should be isolated from the existing customer `messaging` feature.

If the application already has WebSocket configuration elsewhere, inspect it first and determine whether it can safely be reused.

Do NOT create two competing WebSocket configurations that register the same endpoint.

---

# 34. STOMP authentication interceptor

Implement a channel interceptor specifically for STOMP authentication if required.

The interceptor should:

1. inspect the STOMP CONNECT frame
2. retrieve the JWT from the STOMP native headers
3. validate it with the existing `JwtService`
4. extract NIC
5. load the user with the existing `CustomUserDetailsService`
6. check user status
7. construct an authenticated `UsernamePasswordAuthenticationToken`
8. attach it to the STOMP accessor/security context
9. reject invalid authentication

Do not duplicate JWT parsing logic.

Use the existing `JwtService`.

---

# 35. User status

The existing `User` entity has:

```text
PENDING_ACTIVATION
ACTIVE
INACTIVE
SUSPENDED
```

The internal chat should only be available to appropriate active users.

At minimum, do not allow:

```text
PENDING_ACTIVATION
INACTIVE
SUSPENDED
```

users to participate in chat.

Follow the existing project's definition of enabled/disabled accounts.

Do not invent a new user-status system.

---

# 36. Transaction boundaries

Use `@Transactional` where appropriate.

Especially consider transactions around:

- creating a conversation and its two participants
- sending/persisting a message
- updating `lastReadAt`

Conversation creation should be atomic enough to prevent partially-created conversations.

---

# 37. Concurrency and duplicate conversations

Pay particular attention to this scenario:

```text
Request A:
User 1 starts conversation with User 2

Request B:
User 1 simultaneously starts conversation with User 2
```

Both requests must not create two conversations.

Use appropriate:

- database constraints
- locking
- unique keys
- transactional logic

where appropriate.

Do not assume the frontend will prevent this.

---

# 38. Error handling

Follow the project's existing exception-handling conventions.

Use meaningful exceptions for situations such as:

```text
Conversation not found
User not found
User not eligible for internal chat
Conversation access denied
Invalid message
Unauthenticated WebSocket connection
Invalid WebSocket JWT
```

Do not expose stack traces or sensitive information to clients.

If there is an existing global exception handler, integrate with it rather than creating an unrelated error architecture.

---

# 39. CORS/WebSocket considerations

The existing application currently allows frontend origins through its CORS configuration.

The existing HTTP configuration permits:

```text
/ws/**
```

Do not unnecessarily weaken security further.

Do not use:

```text
allowedOrigins("*")
```

inside a new WebSocket configuration if the project has a known frontend origin configuration that can be reused.

Inspect the existing application configuration first.

Maintain consistency with the existing environment configuration.

---

# 40. Comments — VERY IMPORTANT

I am learning this implementation and need to understand every step.

Therefore:

> Add clear explanatory comments throughout the implementation.

Comments are required for important logic.

Explain:

- why each entity exists
- why each relationship exists
- why `lastReadAt` is used
- how direct conversations are identified
- how duplicate conversations are prevented
- how WebSocket/STOMP authentication works
- why the JWT must be authenticated separately for STOMP
- what each STOMP destination means
- why messages are persisted before broadcasting
- why the server determines the sender
- why conversation membership is checked
- how unread messages are calculated
- why REST and WebSocket are both used
- why the WebSocket endpoint is configured separately
- what each important service method does

Do NOT add meaningless comments such as:

```java
// Create user
userRepository.save(user);
```

Comments should explain the reasoning and architecture.

The code itself should still remain clean and readable.

---

# 41. Do not over-engineer

This is Phase 1.

Do NOT add:

- Redis
- Kafka
- RabbitMQ
- microservices
- separate chat server
- Elasticsearch
- MongoDB
- group chat
- file storage
- push notification infrastructure
- presence tracking
- typing events

unless an existing project dependency makes one unavoidable.

For this university project, a single Spring Boot application with:

```text
PostgreSQL
Spring Data JPA
Spring WebSocket
STOMP
```

is sufficient.

---

# 42. Testing requirements

Add backend tests for the important functionality.

At minimum, cover:

### User discovery

- eligible users returned
- customers excluded
- role filtering works
- name search works
- UUID search works
- current user excluded

### Conversation

- conversation created successfully
- existing direct conversation is reused
- self-conversation rejected
- customer cannot be a participant
- inactive/suspended users cannot participate
- duplicate conversation creation is prevented

### Authorization

- non-participant cannot read messages
- non-participant cannot send messages
- customer cannot access internal chat
- unauthenticated requests are rejected

### Messages

- valid message saved
- blank message rejected
- sender comes from authenticated principal
- client cannot impersonate another sender
- pagination works

### Read status

- `lastReadAt` updates
- unread count is correct
- own messages aren't incorrectly counted as unread

### WebSocket

At least test the critical authentication/authorization behavior if the existing testing setup supports WebSocket/STOMP integration tests.

---

# 43. Do not modify unrelated modules

Before writing code:

1. Inspect the existing project structure.
2. Inspect the existing `pom.xml`.
3. Inspect existing security classes.
4. Inspect existing user repositories/services.
5. Inspect existing exception handling.
6. Inspect any existing WebSocket configuration.
7. Inspect whether JPA naming/migration conventions exist.

Then implement only the necessary files.

Do not rewrite existing authentication.

Do not rewrite `User.java`.

Do not change the current `Role` enum.

Do not modify customer `messaging`.

Do not modify billing, payments, meter reading, reports, predictions, blogs, or other unrelated modules.

---

# 44. Expected backend structure

The final structure should be approximately:

```text
internal_chat/
├── config/
│   ├── InternalChatWebSocketConfig
│   └── StompAuthenticationInterceptor
│
├── controller/
│   ├── InternalChatController
│   └── InternalChatWebSocketController
│
├── dto/
│   ├── CreateConversationRequest
│   ├── SendMessageRequest
│   ├── InternalChatUserResponse
│   ├── ConversationResponse
│   └── MessageResponse
│
├── entity/
│   ├── Conversation
│   ├── ConversationParticipant
│   └── Message
│
├── repository/
│   ├── ConversationRepository
│   ├── ConversationParticipantRepository
│   └── MessageRepository
│
├── service/
│   └── InternalChatService
│
└── exceptions/
    └── appropriate chat exceptions
```

Do not create every possible package/file if it is unnecessary.

Follow the existing project's naming conventions.

---

# 45. Important architecture principle

The architecture should follow:

```text
REST / WebSocket
       ↓
Controller
       ↓
Service
       ↓
Repository
       ↓
PostgreSQL
```

Both REST and WebSocket should reuse the same service/business logic.

For example:

```text
REST Controller
      ↓
InternalChatService
      ↑
WebSocket Controller
```

Do not duplicate message creation logic between REST and WebSocket.

---

# 46. Real-time message flow that the implementation must achieve

The final system should work like this:

```text
User A
  |
  | STOMP message
  ↓
Spring WebSocket
  |
  ↓
Authentication
  |
  ↓
Conversation authorization
  |
  ↓
InternalChatService
  |
  ↓
Save Message
  |
  ↓
PostgreSQL
  |
  ↓
Broadcast saved message
  |
  ↓
STOMP
  |
  ↓
User B
```

User B's browser must receive the message without refreshing the page.

---

# 47. Offline/disconnected recipient

If User B is disconnected:

```text
User A
  ↓
Spring Boot
  ↓
PostgreSQL
```

The message must still be stored.

When User B reconnects/opens the chat:

```text
React
  ↓
REST
  ↓
retrieve persisted messages
```

Therefore:

> PostgreSQL is the source of truth.

WebSocket is responsible for real-time delivery, not permanent storage.

---

# 48. REST versus WebSocket responsibilities

Use REST for:

```text
staff search
conversation list
conversation creation
conversation details
message history
pagination
mark as read
```

Use WebSocket/STOMP for:

```text
new message events
real-time message delivery
real-time read-status events where useful
```

Do not make the entire chat system dependent on WebSocket.

---

# 49. Implementation process

Before making changes:

### Step 1

Inspect the existing project.

### Step 2

Determine whether Spring WebSocket/STOMP dependencies already exist.

### Step 3

Determine whether another WebSocket configuration already exists.

### Step 4

Determine how the current JWT authentication can be reused for STOMP.

### Step 5

Implement entities.

### Step 6

Implement repositories.

### Step 7

Implement DTOs.

### Step 8

Implement service/business logic.

### Step 9

Implement REST controller.

### Step 10

Implement WebSocket/STOMP configuration.

### Step 11

Implement STOMP authentication interceptor.

### Step 12

Implement WebSocket controller.

### Step 13

Add validation and exception handling.

### Step 14

Add tests.

### Step 15

Compile and fix all errors.

### Step 16

Verify that existing application functionality has not been broken.

---

# 50. Important instruction about coding style

Do not blindly generate code based only on this prompt.

First inspect the existing project and follow its conventions for:

- Lombok
- constructors
- DTOs
- repositories
- exception handling
- API response structures
- UUID handling
- validation
- transaction management
- naming
- package structure

If the existing implementation already has a good pattern for something, reuse that pattern.

Do not introduce a competing architecture.

---

# 51. Final acceptance criteria

The implementation is complete only when all of the following are true:

- [ ] Eligible internal users can be searched.
- [ ] Customers cannot appear in internal-chat user search.
- [ ] Search works by full name.
- [ ] Search works by UUID.
- [ ] Role filtering works.
- [ ] Current user is excluded from user discovery.
- [ ] A direct conversation can be created.
- [ ] Duplicate direct conversations are prevented.
- [ ] A user cannot create a conversation with themselves.
- [ ] A user cannot create a conversation with a customer.
- [ ] Conversation lists work.
- [ ] Conversation lists can be filtered by the other participant's role.
- [ ] Conversation search works by participant name.
- [ ] Conversation search works by participant UUID.
- [ ] Messages are persisted in PostgreSQL.
- [ ] Messages are paginated.
- [ ] Users can only retrieve conversations they participate in.
- [ ] Users can only send messages to conversations they participate in.
- [ ] The sender is determined from the authenticated principal.
- [ ] The client cannot impersonate another sender.
- [ ] Blank messages are rejected.
- [ ] WebSocket/STOMP authentication works using the existing JWT.
- [ ] Invalid/expired JWTs cannot establish an authenticated chat session.
- [ ] Real-time messages reach connected recipients without page refresh.
- [ ] Messages are persisted before being broadcast.
- [ ] Disconnected recipients can retrieve missed messages from PostgreSQL later.
- [ ] Read state works.
- [ ] Unread counts work.
- [ ] Existing security/authentication continues to work.
- [ ] Existing customer `messaging` functionality is untouched.
- [ ] No unrelated modules are modified.
- [ ] Important implementation logic contains explanatory comments.
- [ ] Backend tests cover the critical functionality.
- [ ] The project compiles successfully.

After implementation, provide a concise summary of:
1. files created
2. files modified
3. database tables created
4. REST endpoints created
5. WebSocket/STOMP destinations created
6. security/authentication approach
7. tests added
8. any assumptions or issues encountered

Do not implement the frontend yet.