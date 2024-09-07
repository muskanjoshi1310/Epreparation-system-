import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;
 class Epreparation {
    private static final int NULL = 0;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    static Connection con = ConnectionProvider.getConnection(); // Ensure this method is implemented correctly
    static String sql = "";

    public static boolean createAccount(String email, String password) {
        try {
            // Validation
            if (!isValidEmail(email) || password == null || password.isEmpty()) {
                System.out.println("ERR : Invalid Email or Password!");
                return false;
            }

            // Query
            sql = "INSERT INTO customer(email, pass_code) VALUES (?, 1000, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, password);
                
                // Execution
                if (ps.executeUpdate() == 1) {
                    System.out.println("MSG : Account Created Successfully!");
                    return true;
                }
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("ERR : Email Already Registered!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean loginAccount(String email, String password) {
        try {
            // Validation
            if (!isValidEmail(email) || password == null || password.isEmpty()) {
                System.out.println("ERR : Invalid Email or Password!");
                return false;
            }

            // Query
            sql = "SELECT * FROM customer WHERE email = ? AND pass_code = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, password);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int senderAc = rs.getInt("ac_no");
                    int choice;
                    while (true) {
                        System.out.println("Hello, " + rs.getString("email"));
                        System.out.println("1) test courses");
                        System.out.println("2) View result");
                        System.out.println("5) Logout");

                        System.out.print("Enter Choice: ");
                        BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
                        choice = Integer.parseInt(sc.readLine());

                        switch (choice) {
                            case 1:
                                System.out.print("Enter course Name : ");
                                int receiveAc = Integer.parseInt(sc.readLine());
                                System.out.print("Enter Testpaper : ");
                                int amt = Integer.parseInt(sc.readLine());

                                if (transferMoney(senderAc, receiveAc, amt)) {
                                    System.out.println("MSG : test clear Successfully!");
                                } else {
                                    System.out.println("ERR : Failed ");
                                }
                                break;

                            case 2:
                                getBalance(senderAc);
                                break;

                            case 5:
                                return true;

                            default:
                                System.out.println("ERR : Invalid Choice!");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isValidEmail(String email) {
        return email != null && Pattern.matches(EMAIL_REGEX, email);
    }

    public static void getBalance(int acNo) {
        try {
            sql = "SELECT * FROM customer WHERE ac_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, acNo);
                ResultSet rs = ps.executeQuery();

                System.out.println("-----------------------------------------------------------");
                System.out.printf("%12s %10s %10s\n", "Account No", "Name", "Balance");

                while (rs.next()) {
                    System.out.printf("%12d %10s %10d.00\n", rs.getInt("ac_no"), rs.getString("email"), rs.getInt("balance"));
                }
                System.out.println("-----------------------------------------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean transferMoney(int senderAc, int receiverAc, int amount) {
        if (receiverAc == NULL || amount == NULL) {
            System.out.println("ERR : All Fields Required!");
            return false;
        }

        try {
            con.setAutoCommit(false);
            sql = "SELECT balance FROM customer WHERE ac_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, senderAc);
                ResultSet rs = ps.executeQuery();

                if (rs.next() && rs.getInt("balance") < amount) {
                    System.out.println("ERR : Insufficient Balance!");
                    return false;
                }

                // Debit
                sql = "UPDATE customer SET balance = balance - ? WHERE ac_no = ?";
                try (PreparedStatement updatePs = con.prepareStatement(sql)) {
                    updatePs.setInt(1, amount);
                    updatePs.setInt(2, senderAc);
                    updatePs.executeUpdate();
                }

                // Credit
                sql = "UPDATE customer SET balance = balance + ? WHERE ac_no = ?";
                try (PreparedStatement updatePs = con.prepareStatement(sql)) {
                    updatePs.setInt(1, amount);
                    updatePs.setInt(2, receiverAc);
                    updatePs.executeUpdate();
                }

                con.commit();
                return true;
            }
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        }
        return false;
    }
}

