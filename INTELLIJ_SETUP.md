# IntelliJ Setup Instructions

## To Fix Database Connection Error

The error `Access denied for user 'petarbozhkov'@'localhost'` means the application is not loading your database credentials.

### Solution 1: Activate the 'local' Profile (Recommended)

1. In IntelliJ, go to **Run → Edit Configurations...**
2. Select your **FitnessApplication** configuration
3. In the **Environment** section:
   - Find **Active profiles** field
   - Enter: `local`
4. Click **OK** and run the application again

This will load `application-local.properties` which contains your database credentials.

### Solution 2: Set Environment Variables

1. In IntelliJ, go to **Run → Edit Configurations...**
2. Select your **FitnessApplication** configuration
3. In the **Environment variables** field, click the folder icon
4. Add:
   - `DB_USERNAME=root`
   - `DB_PASSWORD=babati1528`
5. Click **OK** and run the application again

### Verify Configuration

After setting up, you should see in the logs:
- Database connection successful
- No "Access denied" errors
- Tables being created/updated

If you still see errors, make sure:
- MySQL is running on localhost:3306
- The credentials in `application-local.properties` are correct
- The database `fitness_main_soft_uni` can be created (or already exists)

