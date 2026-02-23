import java.io.*;
import java.util.*;
import java.time.LocalDateTime;

public class LibraryBookTracker {
    private static int validRecCount = 0;
    private static int searchResultCount = 0;
    private static int booksaddedCount = 0;
    private static int errorsCount = 0;

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                throw new InsufficientArgumentsException("must be 2 arguments");
            }
            String filename = args[0];
            String operation = args[1];

            if (!filename.endsWith(".txt")) {
                throw new InvalidFileNameException("the file should end with .txt");
            }

            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            List<Book> books = loadCatalog(file);

            if (operation.matches("\\d{13}")) {
                searchByISBN(books, operation);
            } else if (operation.contains(":")) {
                addBook(books, operation, file);
            } else {
                searchByTitle(books, operation);
            }

        } catch (BookCatalogException e) {
            System.out.println("Error occured: " + e.getMessage());
            errorsCount++;
        } catch (Exception e) {
            System.out.println("Unexpected error.");
            errorsCount++;
        } finally {
            System.out.println("Thank you for using the Library Book Tracker.");
            System.out.println("\nValid records Found: " + validRecCount);
            System.out.println("Search results: " + searchResultCount);
            System.out.println("Total Books added: " + booksaddedCount);
            System.out.println("Errors occured: " + errorsCount);
        }
    }

    static List<Book> loadCatalog(File file) {
        List<Book> books = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                try {
                    Book b = parseBook(line);
                    books.add(b);
                    validRecCount++;
                } catch (BookCatalogException e) {
                    logError(file, line, e.getMessage());
                    errorsCount++;
                }
            }
            scanner.close();
        } catch (Exception e) {
            System.out.println("Error reading file.");
            errorsCount++;
        }
        return books;
    }

    static Book parseBook(String line) throws BookCatalogException {
        String[] fields = line.split(":");
        if (fields.length != 4) {
            throw new MalformedBookEntryException("Wrong format.");
        }

        String title = fields[0].trim();
        String author = fields[1].trim();
        String isbn = fields[2].trim();
        String copystr = fields[3].trim();

        if (title.equals("") || author.equals("")) {
            throw new MalformedBookEntryException("Title or Author empty.");
        }

        if (!isbn.matches("\\d{13}")) {
            throw new InvalidISBNException("ISBN must be exactly 13 digits.");
        }

        int copies;
        try {
            copies = Integer.parseInt(copystr);
            if (copies <= 0) {
                throw new MalformedBookEntryException("Copies must be positive.");
            }
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies must be a number.");
        }

        return new Book(title, author, isbn, copies);
    }

    static void searchByTitle(List<Book> books, String keyword) {
        printHeader();
        for (Book b : books) {
            if (b.title.toLowerCase().contains(keyword.toLowerCase())) {
                printBook(b);
                searchResultCount++;
            }
        }
    }

    static void searchByISBN(List<Book> books, String isbn) throws DuplicateISBNException {
        int count = 0;
        Book found = null;
        for (Book b : books) {
            if (b.isbn.equals(isbn)) {
                count++;
                found = b;
            }
        }
        if (count > 1) {
            throw new DuplicateISBNException("Duplicate ISBN found.");
        }
        printHeader();
        if (found != null) {
            printBook(found);
            searchResultCount = 1;
        }
    }

    static void addBook(List<Book> books, String record, File file) throws BookCatalogException {
        
    Book newBook = parseBook(record);
        books.add(newBook);

        Collections.sort(books, new Comparator<Book>() {
            public int compare(Book b1, Book b2) {
                return b1.title.compareToIgnoreCase(b2.title);
            }
        });

        try {
            PrintWriter writer = new PrintWriter(file);
            for (Book b : books) {
                writer.println(b.title + ":" + b.author + ":" + b.isbn + ":" + b.copies);
            }
            writer.close();
        } catch (Exception e) {
            throw new BookCatalogException("Error writing to file.");
        }

        printHeader();
        printBook(newBook);

        booksaddedCount = 1;
    }

    static void printHeader() {
        System.out.printf("%-30s %-20s %-15s %5s\n", "Title", "Author", "ISBN", "Copies");
        System.out.println("--------------------------------------------------------------");
    }

    static void printBook(Book b) {
        System.out.printf("%-30s %-20s %-15s %5d\n", b.title, b.author, b.isbn, b.copies);
    }

    static void logError(File file, String line, String message) {
        try {
            File errorFile;
            if (file.getParent() == null) {
                errorFile = new File("errors.log");
            } else {
                errorFile = new File(file.getParent(), "errors.log");
            }
            PrintWriter writer = new PrintWriter(new FileWriter(errorFile, true));
            writer.println("[" + LocalDateTime.now() + "] INVALID: \"" + line + "\" - " + message);
            writer.close();
        } catch (Exception e) {
            System.out.println("Failed to write error log.");
        }
    }

    static class Book {
        String title;
        String author;
        String isbn;
        int copies;

        Book(String t, String a, String i, int c) {
            title = t;
            author = a;
            isbn = i;
            copies = c;
        }
    }
}

class BookCatalogException extends Exception {
    BookCatalogException(String message) {
        super(message);
    }
}

class InvalidISBNException extends BookCatalogException {
    InvalidISBNException(String message) {
        super(message);
    }
}

class DuplicateISBNException extends BookCatalogException {
    DuplicateISBNException(String message) {
        super(message);
    }
}

class MalformedBookEntryException extends BookCatalogException {
    MalformedBookEntryException(String message) {
        super(message);
    }
}

class InsufficientArgumentsException extends BookCatalogException {
    InsufficientArgumentsException(String message) {
        super(message);
    }
}

class InvalidFileNameException extends BookCatalogException {
    InvalidFileNameException(String message) {
        super(message);
    }
}
