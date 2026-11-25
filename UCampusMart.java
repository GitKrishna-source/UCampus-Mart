import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;

public final class UCampusMart {
    private final ConsoleInputOutput console;
    private final MarketplaceMenuActions actions;

    private UCampusMart(MarketplaceStore store, ConsoleInputOutput console) {
        this.console = console;
        actions = new MarketplaceMenuActions(store, console);
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: java UCampusMart [data-file | --mysql]");
            System.exit(2);
        }
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println("Usage: java UCampusMart [data-file | --mysql]");
            System.out.println("The default file is .ucampus-mart/marketplace.properties in your home directory.");
            System.out.println("Use --mysql for local MySQL. The default user is root and the app prompts for a password.");
            return;
        }

        var terminal = System.console();
        try (Scanner input = terminal == null ? new Scanner(System.in) : new Scanner(terminal.reader())) {
            ConsoleInputOutput console = new ConsoleInputOutput(input, terminal);
            new UCampusMart(selectStore(args, console), console).run();
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(2);
        } catch (IOException exception) {
            System.err.println("Could not open MySQL storage. " + exception.getMessage());
            System.exit(2);
        } catch (NoSuchElementException exception) {
            System.out.println("\nGoodbye.");
        }
    }

    private static MarketplaceStore selectStore(String[] args, ConsoleInputOutput console) throws IOException {
        if (args.length == 1 && args[0].equals("--mysql")) {
            MarketplaceStore database = DBConnection.fromEnvironment(console::readMySqlPassword);
            database.load();
            return database;
        }
        Path path = args.length == 1
                ? Path.of(args[0])
                : Path.of(System.getProperty("user.home"), ".ucampus-mart", "marketplace.properties");
        return MarketplaceStore.forFile(path);
    }

    private void run() {
        System.out.println("UCampus Mart");
        System.out.println("Buy, sell, and swap hostel essentials aged 12 to 24 months.");
        String student = console.chooseStudent();
        while (!student.isEmpty()) {
            System.out.println("\n--- Current Student: " + student + " ---");
            System.out.print("""
                    1. Browse or search listings
                    2. Post a listing
                    3. Request a purchase
                    4. Propose an exchange
                    5. My listings
                    6. My offers
                    7. Review incoming offers
                    8. Switch student
                    9. Remove a listing
                    0. Exit
                    """);
            try {
                switch (console.prompt("Choose an option > ")) {
                    case "1" -> actions.browse();
                    case "2" -> actions.postListing(student);
                    case "3" -> actions.requestPurchase(student);
                    case "4" -> actions.proposeExchange(student);
                    case "5" -> actions.showMyListings(student);
                    case "6" -> actions.showMyOffers(student);
                    case "7" -> actions.reviewOffers(student);
                    case "8" -> student = console.chooseStudent();
                    case "9" -> actions.removeListing(student);
                    case "0" -> student = "";
                    default -> System.out.println("Choose a number from 0 to 9.");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            } catch (IOException exception) {
                System.out.println("Could not access marketplace data. " + exception.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }
}
