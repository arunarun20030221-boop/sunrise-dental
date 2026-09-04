package com.sunrise.dental.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Console;
import java.io.IOException;
import java.util.Scanner;

/**
 * Menu-driven console client for the Sunrise Dental Clinic system.
 *
 * <p>A separate operating-system process from the server. It reaches the clinic system only
 * through the REST web service - see {@link ApiClient} - which is what makes the application
 * distributed, and it satisfies the brief's requirement for a menu-driven interface literally
 * rather than by analogy with a web navigation bar.</p>
 *
 * <p>Run it with:</p>
 * <pre>
 *   java -jar client/target/client-1.0.0.jar [base-url]
 * </pre>
 */
public class ConsoleClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:9090/sunrise-dental";

    private final Scanner in = new Scanner(System.in);
    private ApiClient api;

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : DEFAULT_BASE_URL;
        new ConsoleClient().run(baseUrl);
    }

    private void run(String baseUrl) {
        banner(baseUrl);

        if (!signIn(baseUrl)) {
            System.out.println("\nUnable to sign in. Exiting.");
            return;
        }

        boolean running = true;
        while (running) {
            showMenu();
            switch (prompt("Choose an option").trim()) {
                case "1" -> viewDiary();
                case "2" -> registerAppointment();
                case "3" -> findAppointment();
                case "4" -> billMenu();
                case "5" -> updateStatus();
                case "6" -> showHelp();
                case "0", "q", "Q" -> running = false;
                default -> System.out.println("  Not a valid option. Enter a number from the menu.");
            }
        }
        System.out.println("\nSigned out. Goodbye.");
    }

    private void banner(String baseUrl) {
        System.out.println("""

                =========================================================
                  Sunrise Dental Clinic - staff console
                =========================================================""");
        System.out.println("  Connecting to: " + baseUrl + "\n");
    }

    /** Authenticates before showing the menu, so an unauthorised user sees nothing at all. */
    private boolean signIn(String baseUrl) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String username = prompt("Username");
            String password = readPassword();

            api = new ApiClient(baseUrl, username, password);
            try {
                if (api.canAuthenticate()) {
                    System.out.println("\n  Signed in as " + username + ".");
                    return true;
                }
                System.out.println("  Invalid username or password. Attempt " + attempt + " of 3.");
            } catch (IOException e) {
                // A connection problem is not a credentials problem: say so, and stop retrying.
                System.out.println("\n  Cannot reach the clinic server at " + baseUrl);
                System.out.println("  Is Tomcat running? (" + e.getMessage() + ")");
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void showMenu() {
        System.out.println("""

                ---------------------------------------------------------
                  1. View appointment diary
                  2. Register a new appointment
                  3. Find an appointment by number
                  4. Bill a patient
                  5. Update an appointment's status
                  6. Help
                  0. Exit
                ---------------------------------------------------------""");
    }

    private void viewDiary() {
        String date = prompt("Date (YYYY-MM-DD, blank for today)").trim();
        call(() -> {
            ApiResult result = api.listAppointments(date);
            if (!result.ok()) {
                System.out.println("  " + result.errorMessage());
                return;
            }
            JsonNode rows = result.body();
            if (rows == null || rows.isEmpty()) {
                System.out.println("  No appointments booked for that date.");
                return;
            }
            System.out.printf("%n  %-18s %-8s %-22s %-16s %-12s%n",
                    "APPOINTMENT", "TIME", "PATIENT", "DENTIST", "STATUS");
            System.out.println("  " + "-".repeat(80));
            rows.forEach(row -> System.out.printf("  %-18s %-8s %-22s %-16s %-12s%n",
                    row.get("appointmentNumber").asText(),
                    row.get("appointmentTime").asText(),
                    truncate(row.get("patientName").asText(), 22),
                    truncate(row.get("dentistName").asText(), 16),
                    row.get("status").asText()));
        });
    }

    private void registerAppointment() {
        System.out.println("\n  New appointment (leave the name blank to cancel)\n");

        String patientName = prompt("  Patient name");
        if (patientName.isBlank()) {
            return;
        }
        String contact = prompt("  Contact number (e.g. 0771234567)");
        String address = prompt("  Address");
        String email = prompt("  Email (optional)");
        String dentistId = prompt("  Dentist id (1=Silva, 2=Fernando, 3=Perera)");
        String treatmentId = prompt("  Treatment id (1..5, see Help)");
        String date = prompt("  Date (YYYY-MM-DD)");
        String time = prompt("  Time (HH:MM)");
        String sessions = prompt("  Sessions [1]");
        String notes = prompt("  Notes (optional)");

        // Built by hand rather than with a shared DTO: the client deliberately does not depend
        // on the server's classes, so the JSON contract is the only thing binding the two.
        String body = """
                {"patientName":"%s","address":"%s","contactNumber":"%s","email":"%s",
                 "dentistId":%s,"treatmentTypeId":%s,"appointmentDate":"%s",
                 "appointmentTime":"%s:00","sessionCount":%s,"notes":"%s"}
                """.formatted(
                escape(patientName), escape(address), escape(contact), escape(email),
                blankToNull(dentistId), blankToNull(treatmentId), date, time,
                sessions.isBlank() ? "1" : sessions.trim(), escape(notes));

        call(() -> {
            ApiResult result = api.registerAppointment(body);
            if (result.ok()) {
                System.out.println("\n  Registered: " + result.text("appointmentNumber"));
                System.out.println("  " + result.text("patientName") + " with "
                        + result.text("dentistName") + " for " + result.text("treatmentType"));
                System.out.println("  " + result.text("appointmentDate") + " at "
                        + result.text("appointmentTime"));
            } else {
                System.out.println("\n  Could not register the appointment:");
                System.out.println("  " + result.errorMessage());
            }
        });
    }

    private void findAppointment() {
        String number = prompt("Appointment number (e.g. APT-2026-000001)").trim();
        if (number.isBlank()) {
            return;
        }
        call(() -> {
            ApiResult result = api.findAppointment(number.toUpperCase());
            if (!result.ok()) {
                System.out.println("  " + result.errorMessage());
                return;
            }
            System.out.println();
            printField("Appointment", result.text("appointmentNumber"));
            printField("Status", result.text("status"));
            printField("Patient", result.text("patientName"));
            printField("Address", result.text("address"));
            printField("Contact", result.text("contactNumber"));
            printField("Dentist", result.text("dentistName"));
            printField("Treatment", result.text("treatmentType"));
            printField("Date", result.text("appointmentDate"));
            printField("Time", result.text("appointmentTime"));
            printField("Sessions", result.text("sessionCount"));
            printField("Notes", result.text("notes"));
        });
    }

    private void billMenu() {
        String number = prompt("Appointment number").trim().toUpperCase();
        if (number.isBlank()) {
            return;
        }

        call(() -> {
            ApiResult preview = api.previewBill(number);
            if (!preview.ok()) {
                System.out.println("  " + preview.errorMessage());
                return;
            }
            System.out.println("\n  Bill preview for " + number);
            printField("Consultation", preview.text("consultationFee"));
            printField("Treatment", preview.text("treatmentCost"));
            if (!"0.00".equals(preview.text("adjustment"))) {
                printField(preview.text("adjustmentReason"), preview.text("adjustment"));
            }
            printField("TOTAL", preview.text("total"));

            if (!prompt("\n  Issue this bill? (y/N)").trim().equalsIgnoreCase("y")) {
                System.out.println("  Not issued.");
                return;
            }

            ApiResult issued = api.issueBill(number);
            if (issued.ok()) {
                System.out.println("  Bill " + issued.text("billNumber")
                        + " issued for " + issued.text("total") + ".");
                System.out.println("  Print the receipt from the web interface: "
                        + "Appointments > " + number + " > View / print receipt");
            } else {
                System.out.println("  " + issued.errorMessage());
            }
        });
    }

    private void updateStatus() {
        String number = prompt("Appointment number").trim().toUpperCase();
        if (number.isBlank()) {
            return;
        }
        System.out.println("    a. Mark attended");
        System.out.println("    c. Cancel appointment");
        String choice = prompt("  Choose").trim().toLowerCase();

        call(() -> {
            ApiResult result = switch (choice) {
                case "a" -> api.markAttended(number);
                case "c" -> api.cancelAppointment(number);
                default -> null;
            };
            if (result == null) {
                System.out.println("  Nothing changed.");
            } else if (result.ok()) {
                System.out.println("  " + number + " is now " + result.text("status") + ".");
            } else {
                System.out.println("  " + result.errorMessage());
            }
        });
    }

    private void showHelp() {
        System.out.println("""

                  HELP
                  ----
                  Dentist ids     1 = Dr. Silva (General)   2 = Dr. Fernando (Orthodontics)
                                  3 = Dr. Perera (Endodontics)

                  Treatment ids   1 = Check-up and Scaling    LKR  3,500  (30 min)
                                  2 = Tooth Extraction        LKR  6,000  (45 min, + anaesthesia)
                                  3 = Root Canal Treatment    LKR 15,000  (60 min, per session)
                                  4 = Teeth Whitening         LKR 12,000  (90 min, + 15% materials)
                                  5 = Composite Filling       LKR  4,500  (40 min)

                  Every bill also includes the LKR 2,000 consultation fee.

                  Clinic hours    08:00 to 18:00. A treatment must finish before closing, so a
                                  90 minute whitening cannot start after 16:30.

                  Bookings are refused if the dentist already has an overlapping appointment.
                  The message names the clashing appointment so you can offer another slot.

                  Contact numbers must be 10 digits starting 0, or +94 then 9 digits.

                  Receipts are printed from the web interface, not from this console.
                """);
    }

    // ------------------------------------------------------------------ helpers

    /** Runs an action that talks to the server, turning transport failures into a message. */
    private void call(ServerAction action) {
        try {
            action.run();
        } catch (IOException e) {
            System.out.println("  Cannot reach the clinic server: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("  Interrupted.");
        }
    }

    private interface ServerAction {
        void run() throws IOException, InterruptedException;
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        System.out.flush();
        return in.hasNextLine() ? in.nextLine() : "";
    }

    /** Uses the console's no-echo reader when available so the password is not shown. */
    private String readPassword() {
        Console console = System.console();
        if (console != null) {
            char[] entered = console.readPassword("Password: ");
            return entered == null ? "" : new String(entered);
        }
        // No attached console (an IDE, or piped input): fall back to a visible read.
        return prompt("Password");
    }

    private void printField(String label, String value) {
        System.out.printf("    %-14s %s%n", label, value);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "...";
    }

    /** Minimal JSON string escaping for the hand-built request bodies. */
    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Sends JSON null rather than an empty string when a numeric field is left blank. */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? "null" : value.trim();
    }
}
