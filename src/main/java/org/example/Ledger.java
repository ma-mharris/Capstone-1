package org.example;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Simplified Ledger class:
 * - reads/writes transactions to transactions.csv
 * - skips header row safely
 * - provides reports: MTD, previous month, YTD, previous year, vendor search, custom search
 * - addExpense / addPayment / displayBalance implemented
 */
public class Ledger {

    private static final String FILE_PATH = "transactions.csv";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ISO_LOCAL_TIME;

    // transaction record
    public record Transaction(LocalDate date, LocalTime time, String description, String vendor, double amount) {}

    // ---------- Home screen ----------
    public static void homeScreen(Scanner scanner) {
        while (true) {
            System.out.print("""
                    
                    ===== Ledger Menu =====
                    [A] Show all entries
                    [E] Show only expenses
                    [I] Show only income
                    [R] Reports
                    [H] Home
                    Enter choice: """);

            String choice = scanner.nextLine().trim().toUpperCase();
            switch (choice) {
                case "A" -> showFiltered("ALL");
                case "E" -> showFiltered("EXPENSE");
                case "I" -> showFiltered("INCOME");
                case "R" -> showReports(scanner);
                case "H" -> { return; }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    // ---------- Read CSV (skips header safely) ----------
    private static List<Transaction> readTransactions() {
        ensureCsvExists();
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // skip header if present (case-insensitive)
                    String lower = line.trim().toLowerCase();
                    if (lower.startsWith("date") || lower.startsWith("date|")) {
                        continue;
                    }
                    // if first line is not header we should still parse it
                }

                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;

                try {
                    LocalDate date = LocalDate.parse(parts[0].trim(), DATE_FMT);
                    LocalTime time = LocalTime.parse(parts[1].trim(), TIME_FMT);
                    String desc = parts[2].trim();
                    String vendor = parts[3].trim();
                    double amount = Double.parseDouble(parts[4].trim());
                    transactions.add(new Transaction(date, time, desc, vendor, amount));
                } catch (DateTimeParseException | NumberFormatException ex) {
                    // skip invalid rows but keep running
                    System.out.println("Skipping invalid row: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠️ Error reading file: " + e.getMessage());
        }
        return transactions;
    }

    // ensure file exists and has header
    private static void ensureCsvExists() {
        File f = new File(FILE_PATH);
        if (!f.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(f, false))) {
                pw.println("date|time|description|vendor|amount");
            } catch (IOException e) {
                System.out.println("Could not create transactions file: " + e.getMessage());
            }
        }
    }

    // ---------- Display ----------
    private static void showTransactions(List<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("\nNo transactions found.");
            return;
        }
        System.out.println("\n========= TRANSACTIONS =========");
        for (Transaction t : list) {
            System.out.printf("""
                    ------------------------------
                    Date:        %s
                    Time:        %s
                    Description: %s
                    Vendor:      %s
                    Amount:      %.2f
                    """, t.date(), t.time(), t.description(), t.vendor(), t.amount());
        }
        System.out.println("================================\n");
    }

    private static void showFiltered(String type) {
        List<Transaction> data = readTransactions();
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : data) {
            switch (type) {
                case "EXPENSE" -> { if (t.amount() < 0) out.add(t); }
                case "INCOME" -> { if (t.amount() > 0) out.add(t); }
                default -> out.add(t);
            }
        }
        showTransactions(out);
    }

    // ---------- Add / write ----------
    public static void addExpense(String[] info, Scanner scanner) {
        addTransaction(info, scanner, true);
    }

    public static void addPayment(String[] info, Scanner scanner) {
        addTransaction(info, scanner, false);
    }

    private static void addTransaction(String[] info, Scanner scanner, boolean isExpense) {
        ensureCsvExists();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0);
        String description = info[0];
        String vendor = info[1];
        double amount;
        try {
            amount = Double.parseDouble(info[2]);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid amount format, transaction cancelled.");
            return;
        }
        if (isExpense) amount = -Math.abs(amount);
        else amount = Math.abs(amount);

        String line = String.format("%s|%s|%s|%s|%.2f%n", date.format(DATE_FMT), time.format(TIME_FMT), description.replace("|", " "), vendor.replace("|", " "), amount);
        try (FileWriter fw = new FileWriter(FILE_PATH, true)) {
            fw.write(line);
            System.out.println("\nTransaction saved.");
            System.out.println("Press ENTER to continue...");
            scanner.nextLine();
        } catch (IOException e) {
            System.out.println("Error writing transaction: " + e.getMessage());
        }
    }

    // ---------- Balance ----------
    public static void displayBalance() {
        List<Transaction> transactions = readTransactions();
        double income = 0;
        double expenses = 0;
        for (Transaction t : transactions) {
            if (t.amount() > 0) income += t.amount();
            else expenses += Math.abs(t.amount());
        }
        double net = income - expenses;
        System.out.printf("""
                
                ===== BALANCE =====
                Total Income:   $%.2f
                Total Expenses: $%.2f
                Net Balance:    $%.2f
                ===================
                """, income, expenses, net);
    }

    // ---------- Reports ----------
    private static void showReports(Scanner scanner) {
        while (true) {
            System.out.print("""
                    
                    ===== Reports =====
                    [1] Month to date
                    [2] Previous month
                    [3] Year to date
                    [4] Previous year
                    [5] Search by vendor
                    [6] Custom search (date range, description, vendor, amount)
                    [0] Back
                    Enter choice: """);
            String input = scanner.nextLine().trim();
            List<Transaction> transactions = readTransactions();
            List<Transaction> results;
            LocalDate today = LocalDate.now();

            switch (input) {
                case "1" -> {
                    results = filterByDate(transactions, today.withDayOfMonth(1), today);
                    showTransactions(results);
                }
                case "2" -> {
                    LocalDate start = today.minusMonths(1).withDayOfMonth(1);
                    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                    results = filterByDate(transactions, start, end);
                    showTransactions(results);
                }
                case "3" -> {
                    results = filterByDate(transactions, LocalDate.of(today.getYear(), 1, 1), today);
                    showTransactions(results);
                }
                case "4" -> {
                    int prevYear = today.getYear() - 1;
                    results = filterByDate(transactions, LocalDate.of(prevYear, 1, 1), LocalDate.of(prevYear, 12, 31));
                    showTransactions(results);
                }
                case "5" -> {
                    System.out.print("Enter vendor (partial match): ");
                    String v = scanner.nextLine().trim().toLowerCase();
                    results = searchByVendor(transactions, v);
                    showTransactions(results);
                }
                case "6" -> {
                    results = runCustomSearch(scanner, transactions);
                    showTransactions(results);
                }
                case "0" -> { return; }
                default -> System.out.println("Invalid choice, try again.");
            }
        }
    }

    private static List<Transaction> filterByDate(List<Transaction> transactions, LocalDate start, LocalDate end) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : transactions) {
            if ((t.date().isEqual(start) || t.date().isAfter(start)) && (t.date().isEqual(end) || t.date().isBefore(end))) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<Transaction> searchByVendor(List<Transaction> transactions, String vendorLower) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t.vendor().toLowerCase().contains(vendorLower)) out.add(t);
        }
        return out;
    }

    // custom search: prompts user for optional filters and applies them
    private static List<Transaction> runCustomSearch(Scanner scanner, List<Transaction> data) {
        System.out.println("\nLeave blank to skip a filter.");
        System.out.print("Start date (yyyy-MM-dd) or blank: ");
        String startStr = scanner.nextLine().trim();
        System.out.print("End date (yyyy-MM-dd) or blank: ");
        String endStr = scanner.nextLine().trim();
        System.out.print("Description contains or blank: ");
        String desc = scanner.nextLine().trim().toLowerCase();
        System.out.print("Vendor contains or blank: ");
        String vendor = scanner.nextLine().trim().toLowerCase();
        System.out.print("Amount equals (number) or blank: ");
        String amtStr = scanner.nextLine().trim();

        LocalDate start = null;
        LocalDate end = null;
        try {
            if (!startStr.isEmpty()) start = LocalDate.parse(startStr, DATE_FMT);
        } catch (DateTimeParseException ignored) { System.out.println("Bad start date — ignoring."); }
        try {
            if (!endStr.isEmpty()) end = LocalDate.parse(endStr, DATE_FMT);
        } catch (DateTimeParseException ignored) { System.out.println("Bad end date — ignoring."); }

        List<Transaction> out = new ArrayList<>();
        for (Transaction t : data) {
            if (start != null && t.date().isBefore(start)) continue;
            if (end != null && t.date().isAfter(end)) continue;
            if (!desc.isEmpty() && !t.description().toLowerCase().contains(desc)) continue;
            if (!vendor.isEmpty() && !t.vendor().toLowerCase().contains(vendor)) continue;
            if (!amtStr.isEmpty()) {
                try {
                    double a = Double.parseDouble(amtStr);
                    if (Math.abs(t.amount() - a) > 0.0001) continue;
                } catch (NumberFormatException ex) {
                    System.out.println("Bad amount filter — ignoring amount filter.");
                }
            }
            out.add(t);
        }
        return out;
    }
}
