# UCampus Mart

A Java-based console marketplace that helps college students buy, sell, and exchange hostel essentials with one another.

UCampus Mart is designed around a simple idea: useful items should find a second home instead of being left unused. Students can publish listings, discover available items, send purchase or exchange offers, and manage incoming requests through a menu-driven command-line application.

## Features

- **Student Handles:** Enter a student handle and switch between students.
- **Create Listings:** Add items with their name, category, condition, age, and transaction type.
- **Flexible Listing Types:** List an item for sale, exchange, or both.
- **Browse and Search:** View available listings or search by item name and category.
- **Purchase Requests:** One buyer can have a pending purchase request per listing.
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
- Optional JDBC storage with MySQL Connector/J

## Project Structure

| File | Responsibility |
|---|---|
| `UCampusMart.java` | Starts the application, runs the menu, and switches the active student |
| `ConsoleInputOutput.java` | Reads and validates console input and formats listings and offers |
| `MarketplaceMenuActions.java` | Runs the listing, purchase, exchange, and offer review workflows |
| `Listing.java` | Represents an item listing and enforces its validation and status rules |
| `Marketplace.java` | Manages the listing catalog and provides marketplace operations |
| `MarketplaceOffers.java` | Validates purchase and exchange offers and manages their status |
| `MarketplaceStore.java` | Defines the storage operations and implements local Properties file storage |
| `DBConnection.java` | Opens MySQL JDBC connections and stores listings and offers in the database |

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

## Run with MySQL

1. Start MySQL and create the database and tables from the repository root. Use a MySQL account with permission to create the schema.

   ```powershell
   mysql --host=127.0.0.1 --user=root --password --execute="source database/schema.sql"
   ```

2. The repository includes Connector/J 9.7.0 in `lib/` for MySQL Server 8.0. Connector/J 26.10.0 supports MySQL Server 8.4 and later. Keep one Connector/J JAR in `lib/` when switching driver versions. [Download Connector/J 9.7.0](https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar) · [9.7.0 release notes](https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-7-0.html) · [26.10.0 release notes](https://dev.mysql.com/doc/relnotes/connector-j/en/news-26-10-0.html)

3. Set `UCAMPUS_DB_USER` and `UCAMPUS_DB_PASSWORD` for an account with `SELECT`, `INSERT`, and `UPDATE` privileges on `ucampus_mart.*`. The app defaults to `jdbc:mysql://127.0.0.1:3306/ucampus_mart`. Set `UCAMPUS_DB_URL` to use a different host, port, or database. Supply credentials through the environment and keep them out of Git.

   ```powershell
   $env:UCAMPUS_DB_USER = 'your_mysql_user'
   $mysqlPassword = Read-Host 'MySQL password' -AsSecureString
   $env:UCAMPUS_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $mysqlPassword).Password
   ```

4. Compile with `javac -d out *.java`, then run in PowerShell.

   ```powershell
   java -cp "out;lib/*" UCampusMart --mysql
   ```

   On Linux and macOS, use `java -cp "out:lib/*" UCampusMart --mysql`.

MySQL uses its own listings and offers tables. To open data you previously saved to a Properties file, use the file-mode command above.

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
- While a purchase request is pending, the app rejects additional purchase requests for that listing. Declining the request frees the listing for another buyer. Accepting it marks the listing as sold.
- Owners can still review competing requests saved by earlier versions. New purchase requests are rejected while any of those requests are pending.

## Data Storage

By default, marketplace data is stored at:

```text
~/.ucampus-mart/marketplace.properties
```

The application creates the data directory when it first saves marketplace changes.

File mode uses Java synchronization for threads and a file lock for separate processes. MySQL mode uses a database transaction and a row lock. Both modes check for a pending purchase and save a new request within the same locked update.

## Current Limitations

UCampus Mart is an educational console application and a prototype of a student marketplace.

- Students can switch to any handle in the console.
- Payments and physical item handovers are arranged directly between students.

## Future Improvements

- Category filters and price-based sorting
- Listing descriptions and campus pickup locations
- Marketplace dashboard and statistics
- Automated unit testing
- Student authentication

## Project Objective

This project brings together object-oriented programming, data validation, collections, exception handling, and persistence through local Properties files or JDBC in a student-focused marketplace.

---

**UCampus Mart — Buy, Sell, and Swap Within Your Campus Community.**

