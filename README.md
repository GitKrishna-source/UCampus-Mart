# UCampus Mart

A Java-based console marketplace that helps college students buy, sell, and exchange hostel essentials with one another.

UCampus Mart is designed around a simple idea: useful items should find a second home instead of being left unused. Students can publish listings, discover available items, send purchase or exchange offers, and manage incoming requests through a menu-driven command-line application.

## Features

- **Student Handles:** Enter a student handle and switch between students.
- **Create Listings:** Add items with their name, category, condition, age, and transaction type.
- **Flexible Listing Types:** List an item for sale, exchange, or both.
- **Browse and Search:** View available listings or search by item name and category.
- **Purchase Requests:** Send purchase requests to listing owners.
- **Exchange Offers:** Propose swaps using your own available items.
- **Offer Management:** View incoming and outgoing offers, and accept or decline incoming requests.
- **Listing Status Tracking:** Listings are marked as Available, Sold, or Exchanged.
- **Persistent Storage:** Listings and offers remain saved even after closing the application.

## Technologies Used

- Java 17 or later
- Object-Oriented Programming
- Java Records and Enums
- Java Collections Framework
- Java Streams
- BigDecimal for price handling
- File Handling and Properties
- Java NIO and File Locking

No external libraries or database are required.

## Project Structure

| File | Responsibility |
|---|---|
| `UCampusMart.java` | Starts the application, runs the menu, and switches the active student |
| `ConsoleInputOutput.java` | Reads and validates console input and formats listings and offers |
| `MarketplaceMenuActions.java` | Runs the listing, purchase, exchange, and offer review workflows |
| `Listing.java` | Represents an item listing and enforces its validation and status rules |
| `Marketplace.java` | Manages the listing catalog and provides marketplace operations |
| `MarketplaceOffers.java` | Validates purchase and exchange offers and manages their status |
| `MarketplaceStore.java` | Loads and saves the local data file using locks and atomic writes |
| `MarketplacePropertiesCodec.java` | Converts listings and offers to and from the saved Properties format |

## Requirements

- JDK 17 or later
- Terminal or command prompt

Check your Java installation:

```bash
java -version
javac -version
```

## Installation and Execution

**1. Clone the repository**

```bash
git clone https://github.com/GitKrishna-source/UCampus-Mart.git
cd UCampus-Mart
```

**2. Compile the Java files**

```bash
javac -d out *.java
```

**3. Run the application**

```bash
java -cp out UCampusMart
```

To use a custom data-file location:

```bash
java -cp out UCampusMart path/to/marketplace.properties
```

## How to Use

1. Enter a student handle when prompted.
2. Select **Post a Listing** to add an item.
3. Select **Browse or Search Listings** to find available items.
4. Send a purchase request or propose an exchange.
5. Switch to the listing owner's student handle.
6. Select **Review Incoming Offers** to accept or decline requests.
7. Check **My Listings** and **My Offers** to view updated statuses.

## Listing Rules

- Items must be between 12 and 24 months old.
- Sale listings must have a price greater than zero.
- Prices support up to two decimal places.
- Exchange-only listings cannot have a sale price.
- Students cannot send offers for their own listings.
- Only available listings can be included in new offers.

## Data Storage

By default, marketplace data is stored at:

```text
~/.ucampus-mart/marketplace.properties
```

The application creates the data directory when it first saves marketplace changes.

The storage system uses file locking and temporary files to help protect updates and prevent conflicts between application processes.

## Current Limitations

UCampus Mart is an educational console application and a prototype of a student marketplace.

- Student handles are identifiers; password-based authentication is not implemented.
- Payments and physical item handovers are arranged directly between students.
- Data is stored locally in a Properties file rather than a central database.
- The application currently does not provide a graphical interface or network-based multi-user access.

## Future Improvements

- Category filters and price-based sorting
- Listing descriptions and campus pickup locations
- Marketplace dashboard and statistics
- Automated unit testing
- Database integration
- Student authentication

## Project Objective

This project demonstrates how core Java concepts can be combined to develop a practical menu-driven application. It brings together object-oriented programming, data validation, collections, exception handling, and file persistence in a student-focused marketplace.

---

**UCampus Mart — Buy, Sell, and Swap Within Your Campus Community.**
