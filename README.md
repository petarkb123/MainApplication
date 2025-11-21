# Fitness Application

A Spring Boot fitness tracking application that helps you manage workouts, track exercises, and analyze your progress. Built with modern Java technologies and a clean architecture.

## What's This About?

This is a full-stack fitness application where users can create workout templates, track their sessions, manage custom exercises, and view detailed analytics. It includes user management, subscription tiers (Basic and Pro), and an admin panel. The app uses a microservices architecture with a separate analytics service for advanced statistics.

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 3.4.5
- Spring MVC with Thymeleaf for the frontend
- Spring Data JPA for database access
- Spring Security for authentication
- Spring Cloud OpenFeign for microservice communication
- Maven for building

**Frontend:**
- Thymeleaf templates
- Custom CSS (responsive design)
- Vanilla JavaScript (ES6+)

**Database:**
- MySQL 8.0

The main application runs on port 9090, and there's a separate analytics microservice on port 1010 that handles advanced statistics.

## Getting Started

### What You'll Need

- Java 17 installed
- MySQL 8.0 running locally
- Maven (or use the included `mvnw` wrapper)
- IntelliJ IDEA or your favorite Java IDE

### Setting Up the Database

The application will create the database automatically when you first run it, but you can also create it manually:

```sql
CREATE DATABASE IF NOT EXISTS fitness_main_soft_uni;
```

### Configuring Database Credentials

Since we don't store credentials in the repository (for security reasons), you'll need to set them up yourself. You have two options:

**Option 1: Environment Variables**

Set these before running the app:

On macOS/Linux:
```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
```

On Windows (PowerShell):
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```

**Option 2: Local Properties File (Easiest for IntelliJ)**

1. Create a file called `application-local.properties` in `src/main/resources/`
2. Add your MySQL credentials:
```properties
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

3. In IntelliJ, go to **Run → Edit Configurations**, then either:
   - Add `local` to **Active profiles**, or
   - Add `-Dspring.profiles.active=local` to **VM options**

Don't worry - this file is already in `.gitignore`, so it won't be committed to the repository.

### Running the Application

**In IntelliJ:**
1. Open the project
2. Wait for Maven to download dependencies
3. Find `FitnessApplication.java` and run it

**From Command Line:**
```bash
./mvnw spring-boot:run
```

Once it's running, head to http://localhost:9090 and register a new account to get started!

## Features

### User Management
- User registration and login with remember-me support
- Profile editing with avatar uploads
- Admin panel for user management
- Role-based access (USER/ADMIN)

### Exercise Management
- Create your own custom exercises
   - Filter by muscle group and equipment
   - Built-in exercise library
- Delete exercises you've created

### Workout Templates
- Create reusable workout templates
   - Add exercises with target sets
- Pro users get drop set support
- Clone and edit existing templates

### Workout Sessions
- Start and track live workout sessions
- Add exercises on the fly
- Record sets, reps, and weights
- View complete workout history
- Quick finish option for convenience

### Statistics & Analytics
- Weekly workout summaries
- Session count and volume tracking
- Advanced analytics via the microservice:
   - Training frequency analysis
   - Volume trends by exercise
   - Progressive overload tracking
   - Personal records (PRs)
   - Milestone tracking

### Subscriptions
   - Basic and Pro tiers
- Pro features include drop sets and advanced analytics
- Admin can manage user subscriptions

## Project Structure

The application follows a clean, layered architecture:

- **Controllers** handle HTTP requests and render views
- **Services** contain all business logic
- **Repositories** handle database operations
- **Models** represent domain entities

Everything is organized by feature (user, exercise, workout, template, analytics) rather than by technical layer, which makes the codebase easier to navigate.

### Main Entities

- **User** - Accounts with roles and subscriptions
- **Exercise** - Custom and built-in exercises
- **WorkoutSession** - Individual workout sessions
- **WorkoutSet** - Sets within a session
- **WorkoutTemplate** - Reusable workout plans
- **TemplateItem** - Exercises in a template
- **Subscription** - User subscription metadata

### Analytics Microservice

There's a separate Spring Boot application that handles advanced analytics. It has its own database (`fitness_analytics_db`) and provides REST endpoints for things like weekly statistics, training frequency, volume trends, and personal records. The main app communicates with it using Feign clients.

## Security

The app uses Spring Security with:
- Form-based authentication
- BCrypt password hashing
- CSRF protection
- Remember-me functionality (14-day validity)
- Role-based authorization
- Session management

Public endpoints: home, login, register, terms, privacy
Authenticated endpoints: everything else
Admin-only: `/admin/**` routes

## Database

The main application uses MySQL with:
- Database: `fitness_main_soft_uni`
- UUID primary keys for all entities
- JPA relationships between entities
- Automatic schema updates (Hibernate DDL auto)

The analytics microservice uses a separate database: `fitness_analytics_db`

## Testing

The project has comprehensive test coverage (85%+ for the main app, 90%+ for analytics):
- Unit tests for services
- Integration tests for repositories
- API tests for controllers
- Error handling and edge case tests

Run tests with:
```bash
./mvnw test
```

Generate coverage reports:
```bash
./mvnw test jacoco:report
```

## Scheduled Tasks

The app runs a few background jobs:
- **Daily at 2 AM**: Cleans up abandoned workouts (older than 7 days still in progress)
- **Daily at 9 AM**: Processes subscription renewals
- **Every 14 days**: Notifies Basic users about Pro features

## Caching

We use Spring's caching to improve performance:
- Exercises, templates, and workout sessions are cached
- Cache is automatically evicted when data changes
- Simple in-memory cache (can be upgraded to Redis for production)

## Error Handling

There's a global exception handler that catches errors and shows user-friendly messages via toast notifications. Custom exceptions handle things like empty templates, invalid avatar URLs, and trying to finish already-finished workouts.

## Code Quality

The codebase follows Java best practices:
- Proper naming conventions
- Layered architecture (no business logic in controllers)
- Feature-based package structure
- No dead code or unused imports
- Consistent formatting
- Proper encapsulation

## API Endpoints

The analytics microservice exposes REST endpoints at `http://localhost:1010/api/analytics`:
- `GET /weekly` - Weekly statistics
- `GET /sessions` - Session summaries
- `GET /training-frequency` - Training frequency analysis
- `GET /volume-trends` - Exercise volume trends
- `GET /progressive-overload` - Progressive overload tracking
- `GET /personal-records` - Personal records
- `GET /milestones` - User milestones
- `POST /milestones` - Create milestone
- `PUT /milestones/{id}` - Update milestone
- `DELETE /milestones/{id}` - Delete milestone

All analytics endpoints require an `X-User-Id` header for user context.

## Troubleshooting

**Can't connect to database?**
- Make sure MySQL is running
- Check your credentials (environment variables or local properties file)
- Verify the database exists or let the app create it automatically

**Port 9090 already in use?**
- Change `server.port` in `application.properties`

**Maven dependencies not downloading?**
- Check your internet connection
- Try `./mvnw clean install`
- In IntelliJ: right-click `pom.xml` → Maven → Reload project

## License

This project was developed for educational purposes as part of a fitness tracking application.

Last updated: November 2025
