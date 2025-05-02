package main.java.server;

/**
 * Represents a delivery request
 */
public class DeliveryRequest {
    private final int id;
    private final String shopName;
    private final String shopAddress;
    private final String shopCity;
    private final String customerAddress;
    private final String customerCity;
    private final String description;
    private final long creationTime;
    private boolean selected;
    private String courierName;
    private long selectionTime;
    
    /**
     * Constructor
     * 
     * @param id Unique delivery ID
     * @param shopName Name of the shop
     * @param shopAddress Address of the shop
     * @param shopCity City of the shop
     * @param customerAddress Address of the customer
     * @param customerCity City of the customer
     * @param description Description of the goods
     * @param creationTime Time when the request was created
     */
    public DeliveryRequest(int id, String shopName, String shopAddress, String shopCity,
                          String customerAddress, String customerCity, String description,
                          long creationTime) {
        this.id = id;
        this.shopName = shopName;
        this.shopAddress = shopAddress;
        this.shopCity = shopCity;
        this.customerAddress = customerAddress;
        this.customerCity = customerCity;
        this.description = description;
        this.creationTime = creationTime;
        this.selected = false;
        this.courierName = null;
        this.selectionTime = 0;
    }
    
    public int getId() {
        return id;
    }
    
    public String getShopName() {
        return shopName;
    }
    
    public String getShopAddress() {
        return shopAddress;
    }
    
    public String getShopCity() {
        return shopCity;
    }
    
    public String getCustomerAddress() {
        return customerAddress;
    }
    
    public String getCustomerCity() {
        return customerCity;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public String getCourierName() {
        return courierName;
    }
    
    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }
    
    public long getSelectionTime() {
        return selectionTime;
    }
    
    public void setSelectionTime(long selectionTime) {
        this.selectionTime = selectionTime;
    }
    
    @Override
    public String toString() {
        return "DeliveryRequest{" +
                "id=" + id +
                ", shopName='" + shopName + '\'' +
                ", customerAddress='" + customerAddress + '\'' +
                ", customerCity='" + customerCity + '\'' +
                ", description='" + description + '\'' +
                ", selected=" + selected +
                ", courierName='" + courierName + '\'' +
                '}';
    }
}