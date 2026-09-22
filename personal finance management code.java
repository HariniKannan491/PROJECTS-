import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Scanner;

public class PersonalFinanceManager {

    private JFrame frame;
    private JTextField dateField;
    private JTextField categoryField;
    private JTextField descriptionField;
    private JComboBox<String> typeCombo;
    private JTextField amountField;
    private DefaultTableModel tableModel;
    private JLabel totalIncomeLabel;
    private JLabel totalExpenseLabel;
    private JLabel balanceLabel;
    private DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

    public PersonalFinanceManager() {
        initUI();
    }

    private void initUI() {
        frame = new JFrame("Personal Finance Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 560);
        frame.setLocationRelativeTo(null);

        // Top input panel
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add Transaction (enter text in the fields)"));

        dateField = new JTextField(10);
        categoryField = new JTextField(10);
        descriptionField = new JTextField(18);
        typeCombo = new JComboBox<>(new String[] { "Expense", "Income" });
        amountField = new JTextField(10);

        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        inputPanel.add(dateField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryField);
        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Type:"));
        inputPanel.add(typeCombo);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);

        JButton addButton = new JButton("Add");
        inputPanel.add(addButton);

        // Table in center
        String[] cols = { "Date", "Category", "Description", "Type", "Amount" };
        tableModel = new DefaultTableModel(cols, 0) {
            // make Amount column non-editable formatting-wise, but allow editing if desired
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 4 ? true : true; // allow editing for all (you can change)
            }
        };
        JTable table = new JTable(tableModel);
        JScrollPane tablePane = new JScrollPane(table);

        // Right-side controls & summary
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout(10, 10));

        JPanel buttonsPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        JButton removeButton = new JButton("Remove Selected");
        JButton saveButton = new JButton("Save CSV");
        JButton loadButton = new JButton("Load CSV");
        JButton clearAllButton = new JButton("Clear All");
        buttonsPanel.add(removeButton);
        buttonsPanel.add(saveButton);
        buttonsPanel.add(loadButton);
        buttonsPanel.add(clearAllButton);

        JPanel summaryPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Summary"));
        totalIncomeLabel = new JLabel("Total Income: 0.00");
        totalExpenseLabel = new JLabel("Total Expense: 0.00");
        balanceLabel = new JLabel("Balance: 0.00");
        summaryPanel.add(totalIncomeLabel);
        summaryPanel.add(totalExpenseLabel);
        summaryPanel.add(balanceLabel);

        rightPanel.add(buttonsPanel, BorderLayout.NORTH);
        rightPanel.add(summaryPanel, BorderLayout.SOUTH);

        // Bottom info help
        JLabel helpLabel = new JLabel("Tip: Enter date, category, description, select type and amount (numbers). Use Save/Load to persist.");
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(helpLabel, BorderLayout.CENTER);

        // Layout main frame
        frame.setLayout(new BorderLayout());
        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(tablePane, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Button actions
        addButton.addActionListener(e -> addTransaction());
        removeButton.addActionListener(e -> {
            int[] sel = table.getSelectedRows();
            if (sel.length == 0) {
                JOptionPane.showMessageDialog(frame, "No rows selected", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // remove from last to first to keep indices valid
            for (int i = sel.length - 1; i >= 0; i--) {
                tableModel.removeRow(sel[i]);
            }
            updateSummary();
        });

        clearAllButton.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(frame, "Clear all transactions?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                tableModel.setRowCount(0);
                updateSummary();
            }
        });

        saveButton.addActionListener(e -> saveCsv());
        loadButton.addActionListener(e -> loadCsv());

        // allow pressing Enter in amount field to add
        amountField.addActionListener(e -> addTransaction());

        frame.setVisible(true);
    }

    private void addTransaction() {
        String date = dateField.getText().trim();
        String category = categoryField.getText().trim();
        String desc = descriptionField.getText().trim();
        String type = (String) typeCombo.getSelectedItem();
        String amtText = amountField.getText().trim();

        if (date.isEmpty() || category.isEmpty() || amtText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill at least Date, Category and Amount.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double amount;
        try {
            // allow comma or plain number
            amtText = amtText.replace(",", "");
            amount = Double.parseDouble(amtText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount. Enter numeric value.", "Validation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // For presentation, format amount to 2 decimals
        tableModel.addRow(new Object[] { date, category, desc, type, moneyFormat.format(amount) });

        // Clear input fields (except category maybe)
        dateField.setText("");
        descriptionField.setText("");
        amountField.setText("");
        // keep category for convenience

        updateSummary();
    }

    private void updateSummary() {
        double totalIncome = 0.0;
        double totalExpense = 0.0;

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object typeObj = tableModel.getValueAt(r, 3);
            Object amountObj = tableModel.getValueAt(r, 4);
            if (amountObj == null) continue;
            double amt;
            try {
                String amtStr = amountObj.toString().replace(",", "");
                amt = Double.parseDouble(amtStr);
            } catch (Exception ex) {
                continue;
            }
            if ("Income".equalsIgnoreCase(typeObj.toString())) {
                totalIncome += amt;
            } else {
                totalExpense += amt;
            }
        }

        double balance = totalIncome - totalExpense;
        totalIncomeLabel.setText("Total Income: " + moneyFormat.format(totalIncome));
        totalExpenseLabel.setText("Total Expense: " + moneyFormat.format(totalExpense));
        balanceLabel.setText("Balance: " + moneyFormat.format(balance));
    }

    private void saveCsv() {
        FileDialog fd = new FileDialog(frame, "Save transactions as CSV", FileDialog.SAVE);
        fd.setFile("transactions.csv");
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;

        File outFile = new File(dir, file);
        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile))) {
            // header
            pw.println("Date,Category,Description,Type,Amount");
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                String date = safeCsv(tableModel.getValueAt(r, 0));
                String cat = safeCsv(tableModel.getValueAt(r, 1));
                String desc = safeCsv(tableModel.getValueAt(r, 2));
                String type = safeCsv(tableModel.getValueAt(r, 3));
                String amt = safeCsv(tableModel.getValueAt(r, 4));
                pw.printf("%s,%s,%s,%s,%s%n", date, cat, desc, type, amt);
            }
            JOptionPane.showMessageDialog(frame, "Saved to " + outFile.getAbsolutePath(), "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String safeCsv(Object o) {
        if (o == null) return "";
        String s = o.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    private void loadCsv() {
        FileDialog fd = new FileDialog(frame, "Open transactions CSV", FileDialog.LOAD);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;

        File inFile = new File(dir, file);
        try (Scanner scanner = new Scanner(inFile)) {
            tableModel.setRowCount(0);
            boolean first = true;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // skip header if present
                if (first && line.toLowerCase().startsWith("date,")) {
                    first = false;
                    continue;
                }
                first = false;
                String[] parts = parseCsvLine(line);
                if (parts.length >= 5) {
                    // normalize amount
                    String amt = parts[4];
                    // try to parse and reformat
                    try {
                        double v = Double.parseDouble(amt.replace(",", ""));
                        amt = moneyFormat.format(v);
                    } catch (Exception ignored) {}
                    tableModel.addRow(new Object[] { parts[0], parts[1], parts[2], parts[3], amt });
                }
            }
            updateSummary();
            JOptionPane.showMessageDialog(frame, "Loaded " + inFile.getAbsolutePath(), "Loaded", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Very simple CSV parser that handles quoted fields
    private String[] parseCsvLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // check for escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    public static void main(String[] args) {
        // start on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new PersonalFinanceManager());
    }
}
