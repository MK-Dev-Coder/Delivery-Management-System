package main.java.common;

/**
 * Protocol.java
 * 
 * This class defines the communication protocol constants shared between
 * server and clients in the Delivery Management System.
 * 
 * @author Your Group Names
 * @version 1.0
 */
public class Protocol {
    // Server response types
    public static final String SUCCESS = "SUCCESS";
    public static final String ERROR = "ERROR";
    public static final String INFO = "INFO";
    public static final String DELIVERY_LIST = "DELIVERY_LIST";
    public static final String DELIVERY = "DELIVERY";
    public static final String GOODBYE = "GOODBYE";
    
    // Client types
    public static final String SHOP = "SHOP";
    public static final String COURIER = "COURIER";
    
    // Client commands
    public static final String ADD_DELIVERY = "ADD_DELIVERY";
    public static final String LIST_MY_DELIVERIES = "LIST_MY_DELIVERIES";
    public static final String LIST_AVAILABLE = "LIST_AVAILABLE";
    public static final String SELECT_DELIVERY = "SELECT_DELIVERY";
    public static final String EXIT = "EXIT";
    
    // Status values
    public static final String AVAILABLE = "AVAILABLE";
    public static final String SELECTED = "SELECTED";
    public static final String NONE = "NONE";
    
    // Field delimiter
    public static final String DELIMITER = "|";
    
    // Server welcome message
    public static final String WELCOME_MESSAGE = "WELCOME TO DELIVERY MANAGEMENT SYSTEM";
}