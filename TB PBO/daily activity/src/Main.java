import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Scanner;

interface ActivityManager {
    void addActivity(Scanner scanner);
    void viewActivities();
    void updateActivity(Scanner scanner);
    void deleteActivity(Scanner scanner);
}

// Base class for shared attributes
class Activity {
    protected String id;
    protected String name;
    protected String description;
    protected Date date;
    protected Time startTime;
    protected Time endTime;
    protected String role;

    public Activity(String id, String name, String description, Date date, Time startTime, Time endTime, String role) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.role = role;
    }

    public long calculateDuration() {
        return (endTime.getTime() - startTime.getTime()) / (1000 * 60); // Return duration in minutes
    }
}

// Subclass for specific behavior
class DailyActivity extends Activity {
    public DailyActivity(String id, String name, String description, Date date, Time startTime, Time endTime, String role) {
        super(id, name, description, date, startTime, endTime, role);
    }

    public String formatDetails() {
        return String.format("ID: %s, Name: %s, Role: %s, Duration: %d minutes", id, name, role, calculateDuration());
    }
}

public class Main implements ActivityManager {
    private final Connection connection;

    public Main(Connection connection) {
        this.connection = connection;
    }

    public static void main(String[] args) {
        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/DB_dailyactivity", "postgres", "12345678");
            connection.setAutoCommit(false); // Enable transaction management
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found: " + e.getMessage());
            return;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return;
        }

        Main manager = new Main(connection);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Daily Routine Manager ===");
            System.out.println("1. Add Activity");
            System.out.println("2. View Activities");
            System.out.println("3. Update Activity");
            System.out.println("4. Delete Activity");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    manager.addActivity(scanner);
                    break;
                case 2:
                    manager.viewActivities();
                    break;
                case 3:
                    manager.updateActivity(scanner);
                    break;
                case 4:
                    manager.deleteActivity(scanner);
                    break;
                case 5:
                    System.out.println("Exiting program...");
                    scanner.close();
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing connection: " + e.getMessage());
                    }
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private Date convertToDate(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            java.util.Date utilDate = sdf.parse(dateString);
            return new Date(utilDate.getTime());
        } catch (Exception e) {
            System.err.println("Invalid date format. Please use yyyy-MM-dd format.");
            return null;
        }
    }

    private String getRoleFromChoice(int choice) {
        switch (choice) {
            case 1: return "WORK";
            case 2: return "LEISURE";
            case 3: return "HEALTH";
            case 4: return "EDUCATION";
            case 5: return "OTHER";
            default: throw new IllegalArgumentException("Invalid choice for Role.");
        }
    }

    @Override
    public void addActivity(Scanner scanner) {
        System.out.print("Enter ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter name: ");
        String name = scanner.nextLine();
        System.out.print("Enter description: ");
        String description = scanner.nextLine();
        System.out.print("Enter date (yyyy-MM-dd): ");
        String dateInput = scanner.nextLine();
        Date date = convertToDate(dateInput);
        if (date == null) return;

        System.out.print("Enter start time (HH:mm): ");
        String startTime = scanner.nextLine() + ":00";
        System.out.print("Enter end time (HH:mm): ");
        String endTime = scanner.nextLine() + ":00";

        System.out.println("Select Role:");
        System.out.println("1. Work");
        System.out.println("2. Leisure");
        System.out.println("3. Health");
        System.out.println("4. Education");
        System.out.println("5. Other");
        System.out.print("Enter your choice: ");
        int roleChoice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        String role = getRoleFromChoice(roleChoice);

        String sql = "INSERT INTO activities (id, name, description, date, start_time, end_time, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            // Start the transaction
            connection.setAutoCommit(false); // Disable auto-commit

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setString(2, name);
                stmt.setString(3, description);
                stmt.setDate(4, date);
                stmt.setTime(5, Time.valueOf(startTime));
                stmt.setTime(6, Time.valueOf(endTime));
                stmt.setString(7, role);
                stmt.executeUpdate();
                
                // Commit the transaction
                connection.commit();
                System.out.println("Activity added successfully.");
            } catch (SQLException e) {
                // Rollback the transaction in case of an error
                connection.rollback();
                System.err.println("Failed to add activity: " + e.getMessage());
            } finally {
                // Reset the auto-commit to true after the transaction is done
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Failed to add activity: " + e.getMessage());
        }
    }

    @Override
    public void viewActivities() {
        String sql = "SELECT * FROM activities";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-10s %-20s %-30s %-15s %-10s %-10s %-10s\n", "ID", "Name", "Description", "Date", "Start Time", "End Time", "Role");
            System.out.println("--------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-10s %-20s %-30s %-15s %-10s %-10s %-10s\n",
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("date"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("role"));
            }
        } catch (SQLException e) {
            System.err.println("Failed to retrieve activities: " + e.getMessage());
        }
    }

    @Override
    public void updateActivity(Scanner scanner) {
        System.out.print("Enter ID to update: ");
        String id = scanner.nextLine();
        System.out.print("Enter new name: ");
        String name = scanner.nextLine();
        System.out.print("Enter new description: ");
        String description = scanner.nextLine();
        System.out.print("Enter new date (yyyy-MM-dd): ");
        String dateInput = scanner.nextLine();
        Date date = convertToDate(dateInput);
        if (date == null) return;

        System.out.print("Enter new start time (HH:mm): ");
        String startTime = scanner.nextLine() + ":00";
        System.out.print("Enter new end time (HH:mm): ");
        String endTime = scanner.nextLine() + ":00";

        System.out.println("Select Role:");
        System.out.println("1. Work");
        System.out.println("2. Leisure");
        System.out.println("3. Health");
        System.out.println("4. Education");
        System.out.println("5. Other");
        System.out.print("Enter your choice: ");
        int roleChoice = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        String role = getRoleFromChoice(roleChoice);

        String sql = "UPDATE activities SET name = ?, description = ?, date = ?, start_time = ?, end_time = ?, role = ? WHERE id = ?";
        try {
            // Disable auto-commit for this transaction
            connection.setAutoCommit(false);
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setDate(3, date);
            stmt.setTime(4, Time.valueOf(startTime));
            stmt.setTime(5, Time.valueOf(endTime));
            stmt.setString(6, role);
            stmt.setString(7, id);
            stmt.executeUpdate();
            connection.commit();
            System.out.println("Activity updated successfully.");
        } catch (SQLException e) {
            // Rollback the transaction in case of an error
            connection.rollback();
            System.err.println("Failed to update activity: " + e.getMessage());
        } finally {
            // Reset auto-commit back to true after the transaction is done
            connection.setAutoCommit(true);
        }
    } catch (SQLException e) {
        System.err.println("Failed to update activity: " + e.getMessage());
    }
}

@Override
public void deleteActivity(Scanner scanner) {
    System.out.print("Enter ID to delete: ");
    String id = scanner.nextLine();

    String sql = "DELETE FROM activities WHERE id = ?";
    try {
        // Disable auto-commit for this transaction
        connection.setAutoCommit(false);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();

            // Commit the transaction
            connection.commit();
            System.out.println("Activity deleted successfully.");
        } catch (SQLException e) {
            // Rollback the transaction in case of an error
            connection.rollback();
            System.err.println("Failed to delete activity: " + e.getMessage());
        } finally {
            // Reset auto-commit back to true after the transaction is done
            connection.setAutoCommit(true);
        }
    } catch (SQLException e) {
        System.err.println("Failed to delete activity: " + e.getMessage());
    }
}
}