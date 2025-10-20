package org.example;

import java.util.Scanner;
import java.time.LocalDateTime;
public class Main {
   public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {

            boolean running = true;


            while (running) {
                printHomeMenu();
                String choice = scanner.nextLine().trim().toUpperCase();

                switch (choice) {
                    case "E": // Add Expense
                        handleAdd(scanner, "expense");
                        break;
                    case "I": // Add Income
                        handleAdd(scanner, "income");
                        break;
                    case "L": // Ledger
                        System.out.println("\nAccessing Ledger...");
                        Ledger.homeScreen(scanner);
                        break;
                    case "B": // Balance
                        System.out.println("\nCalculating balance...");
                        Ledger.displayBalance();
                        System.out.println("Press ENTER to continue...");
                        scanner.nextLine();
                        break;
                    case "X": // Exit
                        System.out.println("\nExiting. Goodbye!");
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid input — please try E, I, L, B, or X.");
                }


            }
        }

    }

    // Print the main menu (keeps main() tidy)
    private static void printHomeMenu() {
        System.out.println("\n\nWelcome to the Account Ledger Application");
        System.out.println("Goal: track expenses and income.");
        System.out.println("\n[E] Add an Expense");
        System.out.println("[I] Add Income");
        System.out.println("[L] Access Ledger");
        System.out.println("[B] View Balance");
        System.out.println("[X] Exit");
        System.out.print("\nEnter Choice: ");
    }

    // Shared handler for adding expense or income
    private static void handleAdd(Scanner scanner, String type) {
        String[] info = collectInfo(scanner, type);
        if (info == null) {
            System.out.println("Cancelled. Returning to main menu.");
            return;
        }

        if ("expense".equalsIgnoreCase(type)) {
            Ledger.addExpense(info, scanner);
        } else { // income
            Ledger.addPayment(info, scanner);
        }
    }

    /**
     * Collects description, vendor/payer, and amount from the user.
     * Entering "X" (case-insensitive) at any prompt cancels and returns null.
     * Returns String[]{description, vendor, amountString}
     */
    public static String[] collectInfo(Scanner scanner, String type) {
        System.out.println("\n(Enter 'X' at any prompt to cancel)\n");

        String promptDesc = "Enter " + ("expense".equalsIgnoreCase(type) ? "item description" : "description of your income") + ": ";
        System.out.print(promptDesc);
        String description = scanner.nextLine().trim();
        if (isCancel(description)) return null;

        String promptVendor = "Enter " + ("expense".equalsIgnoreCase(type) ? "vendor" : "payer") + ": ";
        System.out.print(promptVendor);
        String vendor = scanner.nextLine().trim();
        if (isCancel(vendor)) return null;

        // Read amount as a line and parse it (keeps scanner simple and consistent)
        System.out.print("Enter amount: $");
        String amountLine = scanner.nextLine().trim();
        if (isCancel(amountLine)) return null;

        double amount;

        try {
            amount = Double.parseDouble(amountLine);
            amount = Math.abs(amount); // always store positive; Ledger will handle sign if needed
        } catch (NumberFormatException ex) {
            System.out.println("Invalid amount. Please enter a numeric value (e.g. 123.45).");
            return null;

        }

        // return array of strings (Ledger expects strings in your original code)
        return new String[]{description, vendor, String.format("%.2f", amount)};
    }

    private static boolean isCancel(String s) {
        return s == null || s.equalsIgnoreCase("x");
    }
}

