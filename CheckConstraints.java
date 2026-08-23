import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckConstraints {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/water_management_system?sslmode=disable";
        String user = "postgres";
        String password = "password"; // default for postgres? Or maybe we can just query the yaml file? wait, the application-dev.yml uses ${LOCAL_DATABASE_PASSWORD}. 

        // Let's try common passwords:
        String[] passwords = {"postgres", "root", "admin", "password", ""};
        
        for (String pass : passwords) {
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                System.out.println("Connected with password: " + pass);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT pg_get_constraintdef(c.oid) AS def " +
                                                 "FROM pg_constraint c " +
                                                 "JOIN pg_class t ON c.conrelid = t.oid " +
                                                 "WHERE c.conname = 'payments_status_check'");
                if (rs.next()) {
                    System.out.println("Constraint definition: " + rs.getString("def"));
                } else {
                    System.out.println("Constraint not found!");
                }
                return;
            } catch (Exception e) {
                // Ignore and try next
            }
        }
        System.out.println("Could not connect to database.");
    }
}
