# Fitness Application

A Spring Boot web application for tracking workouts, managing exercises, and analyzing fitness progress.

## What It Does

This is a fitness tracking app where users can:
- Create and manage workout templates
- Track live workout sessions with sets, reps, and weights
- Manage custom exercises
- View statistics and analytics
- Use subscription tiers (Basic and Pro) with different features

The app uses a microservices setup - the main app runs on port 9090, and there's a separate analytics service on port 1010 for advanced statistics.

## Tech Stack

- Java 17
- Spring Boot 3.3.5
- MySQL 8.0
- Thymeleaf for frontend
- Spring Security for authentication
- Maven for building

## How to Run

### Prerequisites

You need:
- Java 17
- MySQL 8.0 running on your computer
- Maven (or use the `mvnw` wrapper that comes with the project)

### Setup Steps

1. **Clone or pull the project**

2. **Set up database credentials**

   The app needs your MySQL username and password. Create a file called `application-local.properties` in `src/main/resources/` and add:

   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```

   Then in IntelliJ, go to **Run → Edit Configurations** and add `local` to **Active profiles**.

   Alternatively, you can set environment variables:
   ```bash
   export DB_USERNAME=root
   export DB_PASSWORD=your_mysql_password
   ```

3. **Start MySQL**

   Make sure MySQL is running on `localhost:3306`.

4. **Run the application**

   In IntelliJ: Just run `FitnessApplication.java`

   Or from command line:
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Open your browser**

   Go to http://localhost:9090 and register a new account.

The database `fitness_app` will be created automatically when you first run the app.

## Features

- **User Management**: Registration, login, profile editing, admin panel
- **Exercises**: Create custom exercises, use built-in library, filter by muscle group
- **Workout Templates**: Create reusable workout plans with exercises and sets
- **Workout Sessions**: Track live workouts, record sets/reps/weights, view history
- **Statistics**: Weekly summaries, training frequency, volume trends, personal records
- **Subscriptions**: Basic and Pro tiers (Pro gets advanced analytics and drop sets)

## Project Structure

The code is organized by feature:
- `user/` - User management and authentication
- `exercise/` - Exercise management
- `workout/` - Workout sessions and sets
- `template/` - Workout templates
- `analytics/` - Integration with analytics microservice
- `web/` - Controllers and web layer
- `common/` - Shared utilities, exceptions, schedulers

## Analytics Microservice

There's a separate Spring Boot app that handles advanced statistics. It runs on port 1010 and has its own database (`fitness_analytics_db`). The main app connects to it using Feign clients.

The app works fine without the analytics service, but you'll need it running for advanced statistics features.

## Testing

Run tests with:
```bash
./mvnw test
```

## Troubleshooting

**Can't connect to database?**
- Make sure MySQL is running
- Check your credentials in `application-local.properties` or environment variables

**Port 9090 already in use?**
- Change `server.port` in `application.properties`

**Maven issues?**
- Try `./mvnw clean install`
- In IntelliJ: right-click `pom.xml` → Maven → Reload project
