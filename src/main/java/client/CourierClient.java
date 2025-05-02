package main.java.client;

import main.java.common.Protocol;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * CourierClient.java
 * 
 * This class implements the courier client component of the Delivery Management System.
 * It allows couriers to register with the server, view available delivery requests,
 * select deliveries to fulfill, and view their selected deliveries.
 * 
 * Features:
 * - Connection handling with the server
 * - Command-line interface for user interaction
 * - Local logging of selected deliveries
 * - Error handling and input validation
 * 
 * @author Your Group Names
 * @version 1.0
 */
public class CourierClient {
    // Constants
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    private static final Logger LOGGER = Logger.getLogger(CourierClient.class.getName());
    
    // Instance variables
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private String courierName;
    private String courierPhone;
    private String courierCity;
    private boolean connected = false;
    
    /**
     * Main method to start the courier client
     */
    public static void main(String[] args) {
        CourierClient client = new CourierClient();
        client.start();
    }
    
    /**
     * Initialize and start the client
     */
    public void start() {
        setupLogger();
        scanner = new Scanner(System.in);
        
        System.out.println("=================================================");
        System.out.println("Welcome to the Delivery Management System - Courier Client");
        System.out.println("=================================================");
        
        // Register the courier
        if (!registerCourier()) {
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
            FileHandler fileHandler = new FileHandler("logs/courier_log.txt", true);
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
     * Register the courier with the server
     * 
     * @return true if registration successful, false otherwise
     */
    private boolean registerCourier() {
        // Get courier details from user
        System.out.print("Enter courier name: ");
        courierName = scanner.nextLine().trim();
        
        System.out.print("Enter phone number: ");
        courierPhone = scanner.nextLine().trim();
        
        System.out.print("Enter city: ");
        courierCity = scanner.nextLine().trim();
        
        // Validate input
        if (courierName.isEmpty() || courierPhone.isEmpty() || courierCity.isEmpty()) {
            System.out.println("Error: All fields are required.");
            return false;
        }
        
        if (courierName.contains(Protocol.DELIMITER) || courierPhone.contains(Protocol.DELIMITER) || courierCity.contains(Protocol.DELIMITER)) {
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
                Protocol.COURIER, Protocol.DELIMITER, courierName, Protocol.DELIMITER, 
                courierPhone, Protocol.DELIMITER, courierCity);
            out.println(registrationMessage);
            
            // Wait for response
            String response = in.readLine();
            
            if (response.startsWith(Protocol.SUCCESS)) {
                System.out.println("Successfully registered with the server.");
                LOGGER.info("Courier registered: " + courierName);
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
                        listAvailableDeliveries();
                        break;
                    case "2":
                        selectDelivery();
                        break;
                    case "3":
                        listMyDeliveries();
                        break;
                    case "4":
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
        System.out.println("\n--- Courier Menu ---");
        System.out.println("1. List available delivery requests");
        System.out.println("2. Select a delivery request");
        System.out.println("3. List my selected deliveries");
        System.out.println("4. Exit");
    }
    
    /**
     * List all available delivery requests
     */
    private void listAvailableDeliveries() throws IOException {
        System.out.println("\n--- Available Delivery Requests ---");
        
        // Send request to server
        out.println(Protocol.LIST_AVAILABLE);
        
        // Process response
        String response = in.readLine();
        
        if (response.startsWith(Protocol.DELIVERY_LIST)) {
            String[] parts = response.split("\\" + Protocol.DELIMITER);
            int count = Integer.parseInt(parts[1]);
            
            if (count == 0) {
                System.out.println("No available delivery requests found.");
                return;
            }
            
            System.out.println("Found " + count + " available delivery requests:");
            System.out.println("------------------------------------------------------------------------------");
            System.out.printf("%-5s %-15s %-20s %-20s %-15s\n", "ID", "Shop", "From", "To", "Description");
            System.out.println("------------------------------------------------------------------------------");
            
            for (int i = 0; i < count; i++) {
                String deliveryData = in.readLine();
                displayDelivery(deliveryData);
            }
            
            System.out.println("------------------------------------------------------------------------------");
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
     * Select a delivery request to fulfill
     */
    private void selectDelivery() throws IOException {
        System.out.println("\n--- Select Delivery Request ---");
        System.out.print("Enter delivery ID to select: ");
        String deliveryId = scanner.nextLine().trim();
        
        // Validate input
        if (deliveryId.isEmpty()) {
            System.out.println("Error: Delivery ID is required.");
            return;
        }
        
        try {
            Integer.parseInt(deliveryId);
        } catch (NumberFormatException e) {
            System.out.println("Error: Delivery ID must be a number.");
            return;
        }
        
        // Send request to server
        String requestMessage = String.format("%s%s%s", 
            Protocol.SELECT_DELIVERY, Protocol.DELIMITER, deliveryId);
        out.println(requestMessage);
        
        // Process response
        String response = in.readLine();
        System.out.println("Server response: " + response);
        
        if (response.startsWith(Protocol.SUCCESS)) {
            // Log the selected delivery
            logSelectedDelivery(deliveryId);
            
            System.out.println("Delivery request selected successfully.");
        }
    }
    
    /**
     * List all deliveries selected by this courier
     */
    private void listMyDeliveries() throws IOException {
        System.out.println("\n--- My Selected Deliveries ---");
        
        // Send request to server
        out.println(Protocol.LIST_MY_DELIVERIES);
        
        // Process response
        String response = in.readLine();
        
        if (response.startsWith(Protocol.DELIVERY_LIST)) {
            String[] parts = response.split("\\" + Protocol.DELIMITER);
            int count = Integer.parseInt(parts[1]);
            
            if (count == 0) {
                System.out.println("You have not selected any delivery requests yet.");
                return;
            }
            
            System.out.println("Found " + count + " selected delivery requests:");
            System.out.println("------------------------------------------------------------------------------");
            System.out.printf("%-5s %-15s %-20s %-20s %-15s\n", "ID", "Shop", "From", "To", "Description");
            System.out.println("------------------------------------------------------------------------------");
            
            for (int i = 0; i < count; i++) {
                String deliveryData = in.readLine();
                displayDelivery(deliveryData);
            }
            
            System.out.println("------------------------------------------------------------------------------");
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
            String shopName = parts[2];
            String fromAddress = parts[3] + ", " + parts[4];
            String toAddress = parts[5] + ", " + parts[6];
            String description = parts[7];
            
            System.out.printf("%-5s %-15s %-20s %-20s %-15s\n", id, shopName, fromAddress, toAddress, description);
        }
    }
    
    /**
     * Log a selected delivery to a local file
     */
    private void logSelectedDelivery(String deliveryId) {
        try {
            // Create a log directory if it doesn't exist
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            
            // Create a log file for this courier if it doesn't exist
            String logFileName = "logs/" + courierName + "_deliveries.log";
            File logFile = new File(logFileName);
            
            // Format date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            // Prepare log entry
            String logEntry = String.format("[%s] Selected delivery ID: %s", timestamp, deliveryId);
            
            // Write to log file (append mode)
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                pw.println(logEntry);
            }
            
            LOGGER.info("Logged selected delivery: " + deliveryId);
        } catch (IOException e) {
            LOGGER.warning("Failed to log selected delivery: " + e.getMessage());
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
            
            LOGGER.info("Courier client shutdown complete");
        } catch (IOException e) {
            LOGGER.warning("Error closing resources: " + e.getMessage());
        }
    }
}