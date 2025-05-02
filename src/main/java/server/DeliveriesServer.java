package main.java.server;

import main.java.common.Protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

/**
 * DeliveriesServer.java
 * 
 * This class implements the server component of the Delivery Management System.
 * It functions as a bulletin board for delivery requests, handling connections
 * from both shop and courier clients, and managing the delivery lifecycle.
 * 
 * Features:
 * - Multi-threaded server handling concurrent client connections
 * - Thread-safe delivery request management
 * - Proper error handling and logging
 * - Protocol implementation for client-server communication
 * 
 * @author Your Group Names
 * @version 1.0
 */
public class DeliveriesServer {
    // Constants
    private static final int PORT = 8888;
    private static final Logger LOGGER = Logger.getLogger(DeliveriesServer.class.getName());
    
    // Thread-safe collections for storing connected clients and delivery requests
    private final ConcurrentHashMap<String, Shop> connectedShops = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Courier> connectedCouriers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, DeliveryRequest> deliveryRequests = new ConcurrentHashMap<>();
    
    // Atomic counter for generating unique delivery IDs
    private final AtomicInteger deliveryIdCounter = new AtomicInteger(1);
    
    // Lock for synchronizing access to shared resources
    private final Object lock = new Object();
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        DeliveriesServer server = new DeliveriesServer();
        server.startServer();
    }
    
    /**
     * Initializes and starts the server
     */
    public void startServer() {
        setupLogger();
        
        LOGGER.info("Deliveries Server starting up...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Server listening on port " + PORT);
            
            while (true) {
                try {
                    // Wait for client connections
                    Socket clientSocket = serverSocket.accept();
                    
                    // Create a new thread to handle this client
                    ClientHandler handler = new ClientHandler(clientSocket);
                    new Thread(handler).start();
                    
                    LOGGER.info("New client connected from " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not start server on port " + PORT, e);
            System.exit(1);
        }
    }
    
    /**
     * Set up the logger configuration
     */
    private void setupLogger() {
        try {
            // Create file handler for logging to file
            FileHandler fileHandler = new FileHandler("logs/server_log.txt", true);
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
     * Add a new delivery request to the bulletin board
     * 
     * @param shop The shop making the request
     * @param customerAddress The address of the customer
     * @param customerCity The city of the customer
     * @param description Description of the goods to deliver
     * @return The assigned delivery ID
     */
    public int addDeliveryRequest(Shop shop, String customerAddress, String customerCity, String description) {
        synchronized (lock) {
            int deliveryId = deliveryIdCounter.getAndIncrement();
            
            DeliveryRequest request = new DeliveryRequest(
                deliveryId,
                shop.getName(),
                shop.getAddress(),
                shop.getCity(),
                customerAddress,
                customerCity,
                description,
                System.currentTimeMillis()
            );
            
            deliveryRequests.put(deliveryId, request);
            LOGGER.info("New delivery request added: " + request);
            
            return deliveryId;
        }
    }
    
    /**
     * Mark a delivery as selected by a courier
     * 
     * @param deliveryId The ID of the delivery to mark
     * @param courier The courier selecting the delivery
     * @return true if successfully marked, false otherwise
     */
    public boolean markDeliveryAsSelected(int deliveryId, Courier courier) {
        synchronized (lock) {
            DeliveryRequest request = deliveryRequests.get(deliveryId);
            
            if (request == null) {
                LOGGER.warning("Attempt to select non-existent delivery ID: " + deliveryId);
                return false;
            }
            
            if (request.isSelected()) {
                LOGGER.warning("Attempt to select already selected delivery ID: " + deliveryId);
                return false;
            }
            
            request.setSelected(true);
            request.setCourierName(courier.getName());
            request.setSelectionTime(System.currentTimeMillis());
            
            LOGGER.info("Delivery ID " + deliveryId + " selected by courier: " + courier.getName());
            return true;
        }
    }
    
    /**
     * Get a list of all available (unselected) delivery requests
     * 
     * @return List of available delivery requests
     */
    public List<DeliveryRequest> getAvailableDeliveries() {
        List<DeliveryRequest> available = new ArrayList<>();
        
        for (DeliveryRequest request : deliveryRequests.values()) {
            if (!request.isSelected()) {
                available.add(request);
            }
        }
        
        return available;
    }
    
    /**
     * Get all delivery requests (both available and selected)
     * 
     * @return List of all delivery requests
     */
    public List<DeliveryRequest> getAllDeliveries() {
        return new ArrayList<>(deliveryRequests.values());
    }
    
    /**
     * Get all delivery requests for a specific shop
     * 
     * @param shopName The name of the shop
     * @return List of delivery requests from that shop
     */
    public List<DeliveryRequest> getShopDeliveries(String shopName) {
        List<DeliveryRequest> shopDeliveries = new ArrayList<>();
        
        for (DeliveryRequest request : deliveryRequests.values()) {
            if (request.getShopName().equals(shopName)) {
                shopDeliveries.add(request);
            }
        }
        
        return shopDeliveries;
    }
    
    /**
     * Get all delivery requests selected by a specific courier
     * 
     * @param courierName The name of the courier
     * @return List of delivery requests selected by that courier
     */
    public List<DeliveryRequest> getCourierDeliveries(String courierName) {
        List<DeliveryRequest> courierDeliveries = new ArrayList<>();
        
        for (DeliveryRequest request : deliveryRequests.values()) {
            if (request.isSelected() && request.getCourierName().equals(courierName)) {
                courierDeliveries.add(request);
            }
        }
        
        return courierDeliveries;
    }
    
    /**
     * Register a shop client in the system
     * 
     * @param shop The shop to register
     * @return true if registration successful, false if name already exists
     */
    public boolean registerShop(Shop shop) {
        synchronized (lock) {
            if (connectedShops.containsKey(shop.getName())) {
                LOGGER.warning("Shop name already exists: " + shop.getName());
                return false;
            }
            
            connectedShops.put(shop.getName(), shop);
            LOGGER.info("Shop registered: " + shop);
            return true;
        }
    }
    
    /**
     * Register a courier client in the system
     * 
     * @param courier The courier to register
     * @return true if registration successful, false if name already exists
     */
    public boolean registerCourier(Courier courier) {
        synchronized (lock) {
            if (connectedCouriers.containsKey(courier.getName())) {
                LOGGER.warning("Courier name already exists: " + courier.getName());
                return false;
            }
            
            connectedCouriers.put(courier.getName(), courier);
            LOGGER.info("Courier registered: " + courier);
            return true;
        }
    }
    
    /**
     * Inner class to handle client connections
     */
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientType = "UNKNOWN";
        private String clientId = "UNKNOWN";
        
        /**
         * Constructor
         * 
         * @param socket The client socket
         */
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try {
                // Set up communication channels
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                
                // Send welcome message
                out.println(Protocol.WELCOME_MESSAGE);
                
                // Process client messages
                processClientMessages();
                
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error handling client: " + e.getMessage());
            } finally {
                cleanUp();
            }
        }
        
        /**
         * Process messages from the client
         */
        private void processClientMessages() throws IOException {
            String inputLine;
            
            // First message should identify the client type
            inputLine = in.readLine();
            if (inputLine != null) {
                String[] parts = inputLine.split("\\" + Protocol.DELIMITER);
                
                if (parts.length >= 2) {
                    if (parts[0].equals(Protocol.SHOP)) {
                        clientType = Protocol.SHOP;
                        handleShopRegistration(parts);
                    } else if (parts[0].equals(Protocol.COURIER)) {
                        clientType = Protocol.COURIER;
                        handleCourierRegistration(parts);
                    } else {
                        out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid client type. Must be SHOP or COURIER");
                        return;
                    }
                } else {
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid registration format");
                    return;
                }
            }
            
            // Continue processing client commands
            while ((inputLine = in.readLine()) != null) {
                try {
                    String[] parts = inputLine.split("\\" + Protocol.DELIMITER);
                    
                    if (parts.length == 0) {
                        out.println(Protocol.ERROR + Protocol.DELIMITER + "Empty command");
                        continue;
                    }
                    
                    String command = parts[0];
                    
                    if (clientType.equals(Protocol.SHOP)) {
                        processShopCommand(command, parts);
                    } else if (clientType.equals(Protocol.COURIER)) {
                        processCourierCommand(command, parts);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing command: " + inputLine, e);
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Internal server error: " + e.getMessage());
                }
            }
        }
        
        /**
         * Handle shop client registration
         */
        private void handleShopRegistration(String[] parts) {
            if (parts.length < 4) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid shop registration format. Use: SHOP|name|address|city");
                return;
            }
            
            String name = parts[1];
            String address = parts[2];
            String city = parts[3];
            
            Shop shop = new Shop(name, address, city);
            
            if (registerShop(shop)) {
                clientId = name;
                out.println(Protocol.SUCCESS + Protocol.DELIMITER + "Registered shop: " + name);
            } else {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Shop name already exists: " + name);
            }
        }
        
        /**
         * Handle courier client registration
         */
        private void handleCourierRegistration(String[] parts) {
            if (parts.length < 4) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid courier registration format. Use: COURIER|name|phone|city");
                return;
            }
            
            String name = parts[1];
            String phone = parts[2];
            String city = parts[3];
            
            Courier courier = new Courier(name, phone, city);
            
            if (registerCourier(courier)) {
                clientId = name;
                out.println(Protocol.SUCCESS + Protocol.DELIMITER + "Registered courier: " + name);
            } else {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Courier name already exists: " + name);
            }
        }
        
        /**
         * Process commands from shop clients
         */
        private void processShopCommand(String command, String[] parts) {
            switch (command) {
                case Protocol.ADD_DELIVERY:
                    handleAddDelivery(parts);
                    break;
                case Protocol.LIST_MY_DELIVERIES:
                    handleListShopDeliveries();
                    break;
                case Protocol.EXIT:
                    out.println(Protocol.GOODBYE + Protocol.DELIMITER + "Thank you for using the Delivery Management System");
                    break;
                default:
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Unknown command: " + command);
                    break;
            }
        }
        
        /**
         * Process commands from courier clients
         */
        private void processCourierCommand(String command, String[] parts) {
            switch (command) {
                case Protocol.LIST_AVAILABLE:
                    handleListAvailable();
                    break;
                case Protocol.SELECT_DELIVERY:
                    handleSelectDelivery(parts);
                    break;
                case Protocol.LIST_MY_DELIVERIES:
                    handleListCourierDeliveries();
                    break;
                case Protocol.EXIT:
                    out.println(Protocol.GOODBYE + Protocol.DELIMITER + "Thank you for using the Delivery Management System");
                    break;
                default:
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Unknown command: " + command);
                    break;
            }
        }
        
        /**
         * Handle a shop's request to add a delivery
         */
        private void handleAddDelivery(String[] parts) {
            if (parts.length < 4) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid delivery format. Use: ADD_DELIVERY|customerAddress|customerCity|description");
                return;
            }
            
            String customerAddress = parts[1];
            String customerCity = parts[2];
            String description = parts[3];
            
            Shop shop = connectedShops.get(clientId);
            if (shop == null) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Shop not found. Please register first.");
                return;
            }
            
            int deliveryId = addDeliveryRequest(shop, customerAddress, customerCity, description);
            out.println(Protocol.SUCCESS + Protocol.DELIMITER + "Delivery added with ID: " + deliveryId);
        }
        
        /**
         * Handle a shop's request to list its deliveries
         */
        private void handleListShopDeliveries() {
            List<DeliveryRequest> shopDeliveries = getShopDeliveries(clientId);
            
            if (shopDeliveries.isEmpty()) {
                out.println(Protocol.INFO + Protocol.DELIMITER + "No deliveries found for your shop");
                return;
            }
            
            out.println(Protocol.DELIVERY_LIST + Protocol.DELIMITER + shopDeliveries.size());
            
            for (DeliveryRequest delivery : shopDeliveries) {
                out.println(formatDeliveryForClient(delivery));
            }
        }
        
        /**
         * Handle a courier's request to list available deliveries
         */
        private void handleListAvailable() {
            List<DeliveryRequest> available = getAvailableDeliveries();
            
            if (available.isEmpty()) {
                out.println(Protocol.INFO + Protocol.DELIMITER + "No available deliveries found");
                return;
            }
            
            out.println(Protocol.DELIVERY_LIST + Protocol.DELIMITER + available.size());
            
            for (DeliveryRequest delivery : available) {
                out.println(formatDeliveryForClient(delivery));
            }
        }
        
        /**
         * Handle a courier's request to select a delivery
         */
        private void handleSelectDelivery(String[] parts) {
            if (parts.length < 2) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid format. Use: SELECT_DELIVERY|deliveryId");
                return;
            }
            
            try {
                int deliveryId = Integer.parseInt(parts[1]);
                
                Courier courier = connectedCouriers.get(clientId);
                if (courier == null) {
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Courier not found. Please register first.");
                    return;
                }
                
                if (markDeliveryAsSelected(deliveryId, courier)) {
                    out.println(Protocol.SUCCESS + Protocol.DELIMITER + "Delivery selected: " + deliveryId);
                    
                    // Notify the shop that their delivery has been selected
                    DeliveryRequest request = deliveryRequests.get(deliveryId);
                    Shop shop = connectedShops.get(request.getShopName());
                    if (shop != null) {
                        // In a real implementation, we would have a way to notify the shop
                        // This would require maintaining active connections or using a message queue
                        LOGGER.info("Shop " + shop.getName() + " should be notified about delivery " + deliveryId);
                    }
                } else {
                    out.println(Protocol.ERROR + Protocol.DELIMITER + "Could not select delivery. It may not exist or is already selected.");
                }
            } catch (NumberFormatException e) {
                out.println(Protocol.ERROR + Protocol.DELIMITER + "Invalid delivery ID format");
            }
        }
        
        /**
         * Handle a courier's request to list their selected deliveries
         */
        private void handleListCourierDeliveries() {
            List<DeliveryRequest> courierDeliveries = getCourierDeliveries(clientId);
            
            if (courierDeliveries.isEmpty()) {
                out.println(Protocol.INFO + Protocol.DELIMITER + "No deliveries selected by you");
                return;
            }
            
            out.println(Protocol.DELIVERY_LIST + Protocol.DELIMITER + courierDeliveries.size());
            
            for (DeliveryRequest delivery : courierDeliveries) {
                out.println(formatDeliveryForClient(delivery));
            }
        }
        
        /**
         * Format a delivery request for sending to clients
         */
        private String formatDeliveryForClient(DeliveryRequest delivery) {
            StringBuilder sb = new StringBuilder();
            
            sb.append(Protocol.DELIVERY).append(Protocol.DELIMITER);
            sb.append(delivery.getId()).append(Protocol.DELIMITER);
            sb.append(delivery.getShopName()).append(Protocol.DELIMITER);
            sb.append(delivery.getShopAddress()).append(Protocol.DELIMITER);
            sb.append(delivery.getShopCity()).append(Protocol.DELIMITER);
            sb.append(delivery.getCustomerAddress()).append(Protocol.DELIMITER);
            sb.append(delivery.getCustomerCity()).append(Protocol.DELIMITER);
            sb.append(delivery.getDescription()).append(Protocol.DELIMITER);
            sb.append(delivery.isSelected() ? Protocol.SELECTED : Protocol.AVAILABLE).append(Protocol.DELIMITER);
            
            if (delivery.isSelected()) {
                sb.append(delivery.getCourierName());
            } else {
                sb.append(Protocol.NONE);
            }
            
            return sb.toString();
        }
        
        /**
         * Clean up resources
         */
        private void cleanUp() {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                clientSocket.close();
                
                LOGGER.info("Client disconnected: " + clientType + " " + clientId);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error cleaning up client resources", e);
            }
        }
    }
}