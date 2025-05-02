package main.java.client;

import main.java.common.Protocol;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * ShopClient.java
 * 
 * This class implements the shop client component of the Delivery Management System.
 * It allows shops to register with the server, create delivery requests, and
 * view the status of their requests.
 * 
 * Features:
 * - Connection handling with the server
 * - Command-line interface for user interaction
 * - Local logging of delivery requests
 * - Error handling and input validation
 * 
 * @author Your Group Names
 * @version 1.0
 */
public class ShopClient {
    // Constants
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final Logger LOGGER = Logger.getLogger(ShopClient.class.getName());
    
    // Instance variables
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private String shopName;
    private String shopAddress;
    private String shopCity;
    private boolean connected = false;
    
    /**
     * Main method to start the shop client
     */
    public static void main(String[] args) {
        ShopClient client = new ShopClient();
        client.start();
    }
    
    /**
     * Initialize and start the client
     */
    public void start() {
        setupLogger();
        scanner = new Scanner(System.in);
        
        System.out.println("=================================================");
        System.out.println("Welcome to the Delivery Management System - Shop Client");
        System.out.println("=================================================");
        
        // Register the shop
        if (!registerShop()) {
            System.out.println("Failed to register. Exiting...");
            return;
        }
        
        // Process user commands
        processUserCommands();
        
        // Clean up resources
        cleanup();
    }
    
    /**
     * Set up the logger configuration
     */
    private void setupLogger() {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }
            
            // Create file handler for logging to file
            FileHandler fileHandler = new FileHandler("logs/shop_log.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Create console handler for logging to console
            ConsoleHandler consoleHandler = new ConsoleHandler();
            
            // Configure the root logger
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);
            
            // Remove existing handlers
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }
            
            // Add our handlers
            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
        } catch (IOException e) {
            System.err.println("Failed to set up logger: " + e.getMessage());
        }
    }
    
    /**
     * Connect to the server
     * 
     * @return true if connection successful, false otherwise
     */
    private boolean connectToServer() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Read the welcome message
            String response = in.readLine();
            System.out.println("Server: " + response);
            
            connected = true;
            return true;
        } catch (UnknownHostException e) {
            LOGGER.severe("Unknown host: " + SERVER_HOST);
            return false;
        } catch (IOException e) {
            LOGGER.severe("I/O error connecting to server: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Register the shop with the server
     * 
     * @return true if registration successful, false otherwise
     */
    private boolean registerShop() {
        // Get shop details from user
        System.out.print("Enter shop name: ");
        shopName = scanner.nextLine().trim();
        
        System.out.print("Enter shop address: ");
        shopAddress = scanner.nextLine().trim();
        
        System.out.print("Enter shop city: ");
        shopCity = scanner.nextLine().trim();
        
        // Validate input
        if (shopName.isEmpty() || shopAddress.isEmpty() || shopCity.isEmpty()) {
            System.out.println("Error: All fields are required.");
            return false;
        }
        
        if (shopName.contains(Protocol.DELIMITER) || shopAddress.contains(Protocol.DELIMITER) || shopCity.contains(Protocol.DELIMITER)) {
            System.out.println("Error: Input cannot contain the '|' character.");
            return false;
        }
        
        // Connect to the server
        if (!connectToServer()) {
            System.out.println("Failed to connect to server. Please try again later.");
            return false;
        }
        
        try {
            // Send registration request
            String registrationMessage = String.format("%s%s%s%s%s%s%s", 
                Protocol.SHOP, Protocol.DELIMITER, shopName, Protocol.DELIMITER, 
                shopAddress, Protocol.DELIMITER, shopCity);
            out.println(registrationMessage);
            
            // Wait for response
            String response = in.readLine();
            
            if (response.startsWith(Protocol.SUCCESS)) {
                System.out.println("Successfully registered with the server.");
                LOGGER.info("Shop registered: " + shopName);
                return true;
            } else {
                System.out.println("Registration failed: " + response);
                return false;
            }
        } catch (IOException e) {
            LOGGER.severe("Error during registration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Process user commands via command line interface
     */
    private void processUserCommands() {
        boolean exit = false;
        
        while (!exit && connected) {
            try {
                displayMenu();
                System.out.print("Enter your choice: ");
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        addDelivery();
                        break;
                    case "2":
                        listDeliveries();
                        break;
                    case "3":
                        exit = true;
                        sendExitCommand();
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                        break;
                }
            } catch (IOException e) {
                LOGGER.severe("Error processing command: " + e.getMessage());
                System.out.println("Error communicating with server. Please try again.");
                connected = false;
            }
        }
    }
    
    /**
     * Display the menu options
     */
    private void displayMenu() {
        System.out.println("\n--- Shop Menu ---");
        System.out.println("1. Add new delivery request");
        System.out.println("2. List my delivery requests");
        System.out.println("3. Exit");
    }
    
    /**
     * Add a new delivery request
     */
    private void addDelivery() throws IOException {
        System.out.println("\n--- Add Delivery Request ---");
        
        System.out.print("Enter customer address: ");
        String customerAddress = scanner.nextLine().trim();
        
        System.out.print("Enter customer city: ");
        String customerCity = scanner.nextLine().trim();
        
        System.out.print("Enter description of goods: ");
        String description = scanner.nextLine().trim();
        
        // Validate input
        if (customerAddress.isEmpty() || customerCity.isEmpty() || description.isEmpty()) {
            System.out.println("Error: All fields are required.");
            return;
        }
        
        if (customerAddress.contains(Protocol.DELIMITER) || customerCity.contains(Protocol.DELIMITER) || description.contains(Protocol.DELIMITER)) {
            System.out.println("Error: Input cannot contain the '|' character.");
            return;
        }
        
        // Send request to server
        String requestMessage = String.format("%s%s%s%s%s%s%s", 
            Protocol.ADD_DELIVERY, Protocol.DELIMITER, customerAddress, Protocol.DELIMITER, 
            customerCity, Protocol.DELIMITER, description);
        out.println(requestMessage);
        
        // Process response
        String response = in.readLine();
        System.out.println("Server response: " + response);
        
        if (response.startsWith(Protocol.SUCCESS)) {
            // Extract delivery ID from response
            String[] parts = response.split("\\" + Protocol.DELIMITER);
            if (parts.length >= 2) {
                String deliveryId = parts[1];
                
                // Log the successful delivery request
                logDeliveryRequest(deliveryId, customerAddress, customerCity, description);
                
                System.out.println("Delivery request added successfully with ID: " + deliveryId);
            }
        }
    }
    
    /**
     * List all delivery requests for this shop
     */
    private void listDeliveries() throws IOException {
        System.out.println("\n--- My Delivery Requests ---");
        
        // Send request to server
        out.println(Protocol.LIST_MY_DELIVERIES);
        
        // Process response
        String response = in.readLine();
        
        if (response.startsWith(Protocol.DELIVERY_LIST)) {
            String[] parts = response.split("\\" + Protocol.DELIMITER);
            int count = Integer.parseInt(parts[1]);
            
            if (count == 0) {
                System.out.println("You have no delivery requests.");
                return;
            }
            
            System.out.println("Found " + count + " delivery requests:");
            System.out.println("-------------------------------------------------------");
            System.out.printf("%-5s %-20s %-15s %-10s\n", "ID", "Customer Address", "City", "Status");
            System.out.println("-------------------------------------------------------");
            
            for (int i = 0; i < count; i++) {
                String deliveryData = in.readLine();
                displayDelivery(deliveryData);
            }
            
            System.out.println("-------------------------------------------------------");
        } else if (response.startsWith(Protocol.INFO)) {
            String[] parts = response.split("\\" + Protocol.DELIMITER);
            if (parts.length >= 2) {
                System.out.println(parts[1]);
            }
        } else {
            System.out.println("Error retrieving deliveries: " + response);
        }
    }
    
    /**
     * Display a delivery in formatted output
     */
    private void displayDelivery(String deliveryData) {
        String[] parts = deliveryData.split("\\" + Protocol.DELIMITER);
        
        if (parts.length >= 9 && parts[0].equals(Protocol.DELIVERY)) {
            String id = parts[1];
            String customerAddress = parts[5];
            String customerCity = parts[6];
            String status = parts[8].equals(Protocol.NONE) ? parts[7] : parts[7] + " by " + parts[8];
            
            System.out.printf("%-5s %-20s %-15s %-10s\n", id, customerAddress, customerCity, status);
        }
    }
    
    /**
     * Log a delivery request to a local file
     */
    private void logDeliveryRequest(String deliveryId, String customerAddress, String customerCity, String description) {
        try {
            // Create a log directory if it doesn't exist
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            
            // Create a log file for this shop if it doesn't exist
            String logFileName = "logs/" + shopName + "_deliveries.log";
            File logFile = new File(logFileName);
            
            // Format date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            // Prepare log entry
            String logEntry = String.format("[%s] Delivery ID: %s, Customer: %s, %s, Description: %s\n",
                    timestamp, deliveryId, customerAddress, customerCity, description);
            
            // Write to log file (append mode)
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                pw.println(logEntry);
            }
            
            LOGGER.info("Logged delivery request: " + deliveryId);
        } catch (IOException e) {
            LOGGER.warning("Failed to log delivery request: " + e.getMessage());
        }
    }
    
    /**
     * Send exit command to server
     */
    private void sendExitCommand() {
        out.println(Protocol.EXIT);
        try {
            String response = in.readLine();
            System.out.println("Server: " + response);
        } catch (IOException e) {
            LOGGER.warning("Error reading server response on exit: " + e.getMessage());
        }
    }
    
    /**
     * Clean up resources
     */
    private void cleanup() {
        try {
            if (scanner != null) {
                scanner.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            LOGGER.info("Shop client shutdown complete");
        } catch (IOException e) {
            LOGGER.warning("Error closing resources: " + e.getMessage());
        }
    }
}