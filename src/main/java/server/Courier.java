package main.java.server;

/**
 * Represents a courier client
 */
public class Courier {
    private final String name;
    private final String phone;
    private final String city;
    
    /**
     * Constructor
     * 
     * @param name Courier name (unique ID)
     * @param phone Courier phone number
     * @param city Courier city
     */
    public Courier(String name, String phone, String city) {
        this.name = name;
        this.phone = phone;
        this.city = city;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getCity() {
        return city;
    }
    
    @Override
    public String toString() {
        return "Courier{name='" + name + "', phone='" + phone + "', city='" + city + "'}";
    }
}