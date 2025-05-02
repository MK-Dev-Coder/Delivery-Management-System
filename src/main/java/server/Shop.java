package main.java.server;

/**
 * Represents a shop client
 */
public class Shop {
    private final String name;
    private final String address;
    private final String city;
    
    /**
     * Constructor
     * 
     * @param name Shop name (unique ID)
     * @param address Shop address
     * @param city Shop city
     */
    public Shop(String name, String address, String city) {
        this.name = name;
        this.address = address;
        this.city = city;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAddress() {
        return address;
    }
    
    public String getCity() {
        return city;
    }
    
    @Override
    public String toString() {
        return "Shop{name='" + name + "', address='" + address + "', city='" + city + "'}";
    }
}