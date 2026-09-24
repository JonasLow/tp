package seedu.duke;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Handles all reading from and writing to the user.
 *
 * Keeping every {@code System.out.println} in this one class means the rest of the system can be
 * unit-tested without capturing console output, and the wording of a message only ever
 * changes in one place.
 */
public class Ui {
    /**
     * Column layout of one catalogue line: name padded to 18 characters, then price padded to 8.
     */
    private static final String PRODUCT_LINE_FORMAT = "   %-18s%-8sstock %d";
    private static final String INDENT = "    ";
    private static final String DIVIDER = INDENT + "_".repeat(60);
    private static final String banner = "       ____        _        \n"
            + "      |  _ \\ _   _| | _____ \n"
            + "      | | | | | | | |/ / _ \\\n"
            + "      | |_| | |_| |   <  __/\n"
            + "      |____/ \\__,_|_|\\_\\___|\n";

    private final Scanner scanner;
    private final PrintStream out;

    /**
     * Constructs a Ui that reads from standard input and writes to standard output.
     */
    public Ui() {
        this(System.in, System.out);
    }

    /**
     * Constructs a Ui that reads from and writes to the given streams.
     *
     * Tests use this constructor to feed in scripted input and inspect the exact output.
     *
     * @param in  the stream commands are read from
     * @param out the stream messages are written to
     */
    public Ui(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    /**
     * Returns the next line typed by the user, or null if there is no more input.
     *
     * Null signals end of input (for example, the user pressed Ctrl-D or input was piped in),
     * which lets the main loop stop instead of throwing.
     */
    public String readCommand() {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }

    public void showWelcome() {
        printBanner();
        printIndent("Welcome to the management system.");
        printIndent("Type a command, or \"bye\" to exit.");
    }

    private void printBanner() {
        out.println(banner);
    }

    public void printDivider() {
        out.println(DIVIDER);
    }

    private void printIndent(String line) {
        out.println(line);
    }

    public void showGoodbye() {
        printDivider();
        out.println("Goodbye!");
    }

    /**
     * Shows the confirmation for a newly added product.
     *
     * @param product the product that was just added
     */
    public void showProductAdded(Product product) {
        printIndent("Added product " + product.getName());
        printIndent("Price: " + product.getPriceString());
        printIndent("Stock: " + product.getStock());
    }

    /**
     * Shows the catalogue, one product per line, in the order given.
     *
     * @param products the products to display, already sorted by the caller
     */
    public void showProducts(List<Product> products) {
        printIndent("Products (" + products.size() + "):");
        if (products.isEmpty()) {
            printIndent("Nothing here yet. Add one with: product add p/PRODUCT a/AMOUNT");
        } else {
            for (Product product : products) {
                printIndent(String.format(PRODUCT_LINE_FORMAT,
                        product.getName(), product.getPriceString(), product.getStock()));
            }
        }
    }

    /**
     * Shows the confirmation for a newly created order as a table: one row per product, then a
     * total row.
     *
     * @param order the order that was just created
     */
    public void showOrderCreated(Order order) {
        printIndent("Order " + order.getId() + " created for " + order.getCustomer() + ".");
        printOrderTable(order, "Qty");
    }

    /**
     * Shows the confirmation for a cancelled order, including the stock restored
     * and the amount excluded from recorded sales.
     *
     * @param order the order that was cancelled
     */
    public void showOrderCancelled(Order order) {
        printIndent("Order " + order.getId() + " for " + order.getCustomer() + " cancelled.");
        printIndent("Stock restored:");
        printOrderTable(order, "Restored");
        printIndent("Amount excluded from recorded sales: " + formatMoney(order.getTotal()));
    }
    /**
     * Shows every order as a table: number, customer, units bought, total and status.
     *
     * @param orders the orders to display, oldest first
     */
    public void showOrders(List<Order> orders) {
        printIndent("Orders (" + orders.size() + "):");
        if (orders.isEmpty()) {
            printIndent("Nothing here yet. Create one with: order add c/CUSTOMER p/PRODUCT q/QUANTITY");
            return;
        }

        List<String[]> rows = new ArrayList<>();
        for (Order order : orders) {
            int units = 0;
            for (OrderItem item : order.getItems()) {
                units += item.getQuantity();
            }
            rows.add(new String[] {String.valueOf(order.getId()), order.getCustomer(), String.valueOf(units),
                formatMoney(order.getTotal()), order.isCancelled() ? "Cancelled" : "Active"});
        }
        String[] headers = {"No.", "Customer", "Units", "Total", "Status"};
        boolean[] isLeftAligned = {false, true, false, false, true};
        printTable(headers, rows, null, isLeftAligned);
    }

    /**
     * Prints an order's lines as a table with Product, quantity, Unit price and Subtotal columns,
     * followed by a total row.
     *
     * @param order          the order to print
     * @param quantityHeader the heading of the quantity column, e.g. "Qty" or "Restored"
     */
    private void printOrderTable(Order order, String quantityHeader) {
        List<String[]> rows = new ArrayList<>();
        int totalUnits = 0;
        for (OrderItem item : order.getItems()) {
            rows.add(new String[] {item.getProduct().getName(), String.valueOf(item.getQuantity()),
                formatMoney(item.getUnitPrice()), formatMoney(item.getSubtotal())});
            totalUnits += item.getQuantity();
        }
        String[] headers = {"Product", quantityHeader, "Unit price", "Subtotal"};
        String[] totalRow = {"Total", String.valueOf(totalUnits), "", formatMoney(order.getTotal())};
        boolean[] isLeftAligned = {true, false, false, false};
        printTable(headers, rows, totalRow, isLeftAligned);
    }

    /**
     * Prints a bordered table.
     *
     * Column widths are worked out from the longest value in each column, so long names or large
     * amounts widen the table instead of pushing columns out of line. Numbers are right-aligned so
     * that the decimal points of amounts line up.
     *
     * @param headers       the column headings
     * @param rows          the body rows, each with one cell per column
     * @param footer        a final row set off by a border, such as a total, or null for none
     * @param isLeftAligned for each column, true to left-align it (text), false to right-align (numbers)
     */
    private void printTable(String[] headers, List<String[]> rows, String[] footer, boolean[] isLeftAligned) {
        int[] widths = new int[headers.length];
        for (int column = 0; column < headers.length; column++) {
            widths[column] = headers[column].length();
            for (String[] row : rows) {
                widths[column] = Math.max(widths[column], row[column].length());
            }
            if (footer != null) {
                widths[column] = Math.max(widths[column], footer[column].length());
            }
        }

        String border = tableBorder(widths);
        printIndent(border);
        printIndent(tableRow(headers, widths, isLeftAligned));
        printIndent(border);
        for (String[] row : rows) {
            printIndent(tableRow(row, widths, isLeftAligned));
        }
        printIndent(border);
        if (footer != null) {
            printIndent(tableRow(footer, widths, isLeftAligned));
            printIndent(border);
        }
    }

    /**
     * Returns a border line such as "+---------+-----+", sized to the given column widths.
     */
    private static String tableBorder(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            border.append("-".repeat(width + 2)).append("+");
        }
        return border.toString();
    }

    /**
     * Returns one table row, padding each cell to its column width on the side given by its alignment.
     */
    private static String tableRow(String[] cells, int[] widths, boolean[] isLeftAligned) {
        StringBuilder row = new StringBuilder("|");
        for (int column = 0; column < cells.length; column++) {
            String alignment = isLeftAligned[column] ? "-" : "";
            row.append(" ").append(String.format("%" + alignment + widths[column] + "s", cells[column])).append(" |");
        }
        return row.toString();
    }

    public void showError(String message) {
        printIndent("Sorry! " + message);
    }

    /**
     * Returns an amount in dollars with two decimals, e.g. "$6.38".
     */
    private static String formatMoney(double amount) {
        return String.format("$%.2f", amount);
    }
}
