# RentSphere REST API Reference

Base URL: `https://rentsphere-api.onrender.com/api/v1`
Auth: All endpoints (except starred ★) require `Authorization: Bearer <token>`

---

## Authentication  `/auth`

| Method | Endpoint          | Body                          | Description          |
|--------|-------------------|-------------------------------|----------------------|
| POST   | /auth/register ★  | RegisterRequest               | Register new user    |
| POST   | /auth/login    ★  | LoginRequest                  | Login, receive JWT   |

### RegisterRequest fields
`firstName`, `lastName`, `email`, `password`, `phone` (optional), `role`

Role values: `ADMIN`, `PROPERTY_OWNER`, `PROPERTY_MANAGER`, `TENANT`, `MAINTENANCE_STAFF`

---

## Properties  `/properties`

| Method | Endpoint                       | Role Required                    | Description                  |
|--------|--------------------------------|----------------------------------|------------------------------|
| GET    | /properties ★                  | Public                           | List all (paginated)         |
| GET    | /properties/{id} ★             | Public                           | Get by ID                    |
| GET    | /properties/search ★           | Public                           | Search & filter              |
| GET    | /properties/cities ★           | Public                           | All distinct cities          |
| GET    | /properties/my                 | OWNER, ADMIN                     | My properties                |
| POST   | /properties                    | OWNER, ADMIN                     | Create property              |
| PUT    | /properties/{id}               | OWNER, MANAGER, ADMIN            | Update property              |
| DELETE | /properties/{id}               | OWNER, ADMIN                     | Delete property              |
| PATCH  | /properties/{id}/status        | OWNER, MANAGER, ADMIN            | Update availability          |
| POST   | /properties/{id}/images        | OWNER, MANAGER, ADMIN            | Upload images (multipart)    |
| DELETE | /properties/{id}/images/{imgId}| OWNER, MANAGER, ADMIN            | Delete image                 |

### Search Query Params
`name`, `city`, `type`, `status`, `minRent`, `maxRent`, `sortBy` (rentAsc|rentDesc|newest), `page`, `size`

---

## Tenants  `/tenants`

| Method | Endpoint           | Role Required           | Description            |
|--------|--------------------|-------------------------|------------------------|
| GET    | /tenants           | OWNER, MANAGER, ADMIN   | List all tenants       |
| GET    | /tenants/search    | OWNER, MANAGER, ADMIN   | Search by name/email   |
| GET    | /tenants/my-owner  | OWNER                   | My tenants             |
| GET    | /tenants/me        | TENANT                  | My profile             |
| GET    | /tenants/{id}      | OWNER, MANAGER, ADMIN   | Get by ID              |
| PUT    | /tenants/{id}      | TENANT, ADMIN           | Update profile         |
| DELETE | /tenants/{id}      | ADMIN                   | Deactivate tenant      |

---

## Rental Agreements  `/agreements`

| Method | Endpoint                        | Role Required           | Description            |
|--------|---------------------------------|-------------------------|------------------------|
| POST   | /agreements                     | OWNER, MANAGER, ADMIN   | Create agreement       |
| GET    | /agreements/{id}                | All roles               | Get by ID              |
| GET    | /agreements/my                  | OWNER, ADMIN            | Owner's agreements     |
| GET    | /agreements/property/{propId}   | OWNER, MANAGER, ADMIN   | By property            |
| GET    | /agreements/tenant/{tenantId}   | All roles               | By tenant              |
| POST   | /agreements/{id}/document       | OWNER, MANAGER, ADMIN   | Upload document PDF    |
| PATCH  | /agreements/{id}/terminate      | OWNER, MANAGER, ADMIN   | Terminate agreement    |
| POST   | /agreements/{id}/renew          | OWNER, MANAGER, ADMIN   | Renew agreement        |

---

## Payments  `/payments`

| Method | Endpoint                          | Role Required           | Description                |
|--------|-----------------------------------|-------------------------|----------------------------|
| POST   | /payments/generate/{agreementId}  | OWNER, MANAGER, ADMIN   | Generate monthly payment   |
| POST   | /payments/generate-bulk           | ADMIN                   | Bulk generate for all      |
| PATCH  | /payments/{id}/record             | OWNER, MANAGER, ADMIN   | Record payment (mark paid) |
| GET    | /payments/{id}                    | All roles               | Get by ID                  |
| GET    | /payments/my                      | OWNER, ADMIN            | Owner's payments           |
| GET    | /payments/tenant/{tenantId}       | All roles               | Tenant payment history     |
| GET    | /payments/agreement/{agreeId}     | All roles               | Agreement payments         |
| GET    | /payments/status/{status}         | OWNER, MANAGER, ADMIN   | Filter by status           |

Query params for generate: `month` (1-12), `year` (≥2020)

---

## Maintenance  `/maintenance`

| Method | Endpoint                    | Role Required                          | Description              |
|--------|-----------------------------|----------------------------------------|--------------------------|
| POST   | /maintenance                | TENANT                                 | Raise request            |
| GET    | /maintenance/{id}           | All roles                              | Get by ID                |
| GET    | /maintenance/my             | TENANT                                 | My requests              |
| GET    | /maintenance/owner          | OWNER                                  | Owner's property requests|
| GET    | /maintenance/property/{id}  | OWNER, MANAGER, ADMIN                  | By property              |
| GET    | /maintenance/status/{status}| OWNER, MANAGER, ADMIN, MAINTENANCE_STAFF| By status               |
| GET    | /maintenance/assigned       | MAINTENANCE_STAFF                      | My assigned requests     |
| POST   | /maintenance/{id}/assign    | OWNER, MANAGER, ADMIN                  | Assign to staff          |
| PATCH  | /maintenance/{id}/status    | OWNER, MANAGER, ADMIN, MAINTENANCE_STAFF| Update status           |
| PATCH  | /maintenance/{id}/close     | OWNER, MANAGER, ADMIN, MAINTENANCE_STAFF| Close request           |
| POST   | /maintenance/{id}/image     | TENANT                                 | Upload image (multipart) |

Status values: `OPEN` → `ASSIGNED` → `IN_PROGRESS` → `COMPLETED`

---

## Facilities  `/facilities`

| Method | Endpoint                                     | Role Required           | Description             |
|--------|----------------------------------------------|-------------------------|-------------------------|
| POST   | /facilities/property/{propertyId}            | OWNER, MANAGER, ADMIN   | Add facility            |
| PUT    | /facilities/{facilityId}                     | OWNER, MANAGER, ADMIN   | Update facility         |
| GET    | /facilities/property/{propertyId}            | All roles               | List by property        |
| POST   | /facilities/complaints                       | TENANT                  | Raise complaint         |
| PATCH  | /facilities/complaints/{id}/resolve          | OWNER, MANAGER, ADMIN   | Resolve complaint       |
| GET    | /facilities/complaints/property/{propertyId} | OWNER, MANAGER, ADMIN   | Property complaints     |
| GET    | /facilities/complaints/my                    | TENANT                  | My complaints           |

Facility types: `PARKING`, `SECURITY`, `WATER_SUPPLY`, `ELECTRICITY`, `HOUSEKEEPING`

---

## Dashboard  `/dashboard`

All dashboard endpoints return role-specific KPI snapshots. The caller's identity is resolved
from the JWT — no `userId` param is needed.

| Method | Endpoint                  | Role Required              | Description                                    |
|--------|---------------------------|----------------------------|------------------------------------------------|
| GET    | /dashboard/admin          | ADMIN                      | Platform-wide KPIs (users, properties, revenue)|
| GET    | /dashboard/admin/analytics| ADMIN                      | Revenue trend, payment breakdown, top properties|
| GET    | /dashboard/owner          | OWNER, ADMIN               | Portfolio KPIs (properties, occupancy, revenue)|
| GET    | /dashboard/owner/analytics| OWNER, ADMIN               | Owner revenue trend, breakdowns, top properties|
| GET    | /dashboard/manager        | PROPERTY_MANAGER, ADMIN    | Managed portfolio KPIs (properties, maintenance, payments)|
| GET    | /dashboard/tenant         | TENANT, ADMIN              | Active lease, upcoming payments, maintenance summary|
| GET    | /dashboard/staff          | MAINTENANCE_STAFF, ADMIN   | Assigned request counts broken down by status and priority|

### Response shapes (summary)

**Admin KPIs** — `totalUsers`, `totalProperties`, `totalTenants`, `pendingPayments`, `currentMonthRevenue`

**Admin Analytics** — `revenueTrend[]` (month/year/revenue), `paymentStatusBreakdown`, `topProperties[]`

**Owner KPIs** — `totalProperties`, `occupiedProperties`, `vacantProperties`, `pendingPayments`, `currentMonthRevenue`, `yearToDateRevenue`

**Owner Analytics** — `revenueTrend[]`, `paymentStatusBreakdown`, `topProperties[]`, `maintenancePriorityBreakdown`

**Manager KPIs** — `managedProperties`, `occupiedProperties`, `openMaintenance`, `pendingPayments`

**Tenant KPIs** — `activeAgreement` (property, start/end dates), `pendingPaymentsCount`, `amountDue`, `openMaintenanceCount`

**Staff KPIs** — `totalAssigned`, `openCount`, `inProgressCount`, `completedCount`, `priorityBreakdown` (low/medium/high/urgent)

---

## Notifications  `/notifications`

| Method | Endpoint                   | Role Required | Description                    |
|--------|----------------------------|---------------|--------------------------------|
| GET    | /notifications             | All           | My notifications (paginated)   |
| GET    | /notifications/unread-count| All           | Unread count (integer)         |
| PATCH  | /notifications/{id}/read   | All           | Mark one as read               |
| PATCH  | /notifications/read-all    | All           | Mark all as read               |

Notification types: `PAYMENT`, `MAINTENANCE`, `AGREEMENT`, `SYSTEM`

---

## Reports  `/reports`

| Method | Endpoint                | Role Required   | Query Params              | Response         |
|--------|-------------------------|-----------------|---------------------------|------------------|
| POST   | /reports/revenue        | OWNER, ADMIN    | startDate, endDate, format| File download    |
| POST   | /reports/occupancy      | OWNER, ADMIN    | format                    | File download    |
| POST   | /reports/rent-collection| OWNER, ADMIN    | month, year, format       | File download    |
| POST   | /reports/tenant         | OWNER, ADMIN    | format                    | File download    |
| POST   | /reports/maintenance    | OWNER, ADMIN    | startDate, endDate, format| File download    |
| GET    | /reports/history        | OWNER, ADMIN    | page, size                | Paginated list   |

Format values: `PDF`, `EXCEL`
Date format for params: `yyyy-MM-dd`

---

## Admin  `/admin`

| Method | Endpoint                        | Role Required | Description            |
|--------|---------------------------------|---------------|------------------------|
| GET    | /admin/users                    | ADMIN         | List all users         |
| GET    | /admin/users/search             | ADMIN         | Search users (`?q=`)   |
| GET    | /admin/users/role/{role}        | ADMIN         | Filter by role         |
| GET    | /admin/users/{id}               | ADMIN         | Get user by ID         |
| PATCH  | /admin/users/{id}/activate      | ADMIN         | Activate user          |
| PATCH  | /admin/users/{id}/deactivate    | ADMIN         | Deactivate user        |

---

## Pagination

All paginated endpoints accept:
- `page` (default: 0)
- `size` (default: 10)

Response format:
```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

## Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable error message",
  "fieldErrors": {
    "rentAmount": "must be greater than 0"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Swagger UI

Available at: `http://localhost:8080/swagger-ui.html`
API Docs:     `http://localhost:8080/api-docs`
