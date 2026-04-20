package com.example.flightbooking.models;

public class Flight implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String airlineName;
    private String airlineLogo;
    private String flightNumber;
    private String fromCode;
    private String toCode;
    private String fromTime;
    private String toTime;
    private String price;
    private String status; // "Active" or "Inactive"
    private String duration;
    private String direct;
    private String businessPrice;
    private boolean isRefundable;
    private String departureDate;
    private String arrivalDate;
    private int totalCapacity;
    private int bookedSeats;

    // Real-world airline fields
    private String stops; // "Direct", "1 Stop", etc.
    private String baggageAllowance; // e.g., "1 x 23kg Checked Bag"
    private String terminalFrom;
    private String terminalTo;
    private String boardingTime;
    private String gate;

    // No-argument constructor required for Firestore
    public Flight() {}

    public Flight(String id, String airlineName, String airlineLogo, String flightNumber, String fromCode, String toCode, 
                  String fromTime, String toTime, String price, String status, String duration, String direct, 
                  String businessPrice, boolean isRefundable, String departureDate, String arrivalDate, int totalCapacity, int bookedSeats) {
        this.id = id;
        this.airlineName = airlineName;
        this.airlineLogo = airlineLogo;
        this.flightNumber = flightNumber;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.price = price;
        this.status = status;
        this.duration = duration;
        this.direct = direct;
        this.businessPrice = businessPrice;
        this.isRefundable = isRefundable;
        this.departureDate = departureDate;
        this.arrivalDate = arrivalDate;
        this.totalCapacity = totalCapacity;
        this.bookedSeats = bookedSeats;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAirlineLogo() { return airlineLogo; }
    public void setAirlineLogo(String airlineLogo) { this.airlineLogo = airlineLogo; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getFromCode() { return fromCode; }
    public void setFromCode(String fromCode) { this.fromCode = fromCode; }

    public String getToCode() { return toCode; }
    public void setToCode(String toCode) { this.toCode = toCode; }

    public String getFromTime() { return fromTime; }
    public void setFromTime(String fromTime) { this.fromTime = fromTime; }

    public String getToTime() { return toTime; }
    public void setToTime(String toTime) { this.toTime = toTime; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getDirect() { return direct; }
    public void setDirect(String direct) { this.direct = direct; }

    public String getBusinessPrice() { return businessPrice; }
    public void setBusinessPrice(String businessPrice) { this.businessPrice = businessPrice; }

    public boolean getIsRefundable() { return isRefundable; }
    public void setIsRefundable(boolean isRefundable) { this.isRefundable = isRefundable; }

    public String getDepartureDate() { return departureDate; }
    public void setDepartureDate(String departureDate) { this.departureDate = departureDate; }

    public String getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(String arrivalDate) { this.arrivalDate = arrivalDate; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public int getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(int bookedSeats) { this.bookedSeats = bookedSeats; }

    public String getStops() { return stops; }
    public void setStops(String stops) { this.stops = stops; }

    public String getBaggageAllowance() { return baggageAllowance; }
    public void setBaggageAllowance(String baggageAllowance) { this.baggageAllowance = baggageAllowance; }

    public String getTerminalFrom() { return terminalFrom; }
    public void setTerminalFrom(String terminalFrom) { this.terminalFrom = terminalFrom; }

    public String getTerminalTo() { return terminalTo; }
    public void setTerminalTo(String terminalTo) { this.terminalTo = terminalTo; }

    public String getBoardingTime() { return boardingTime; }
    public void setBoardingTime(String boardingTime) { this.boardingTime = boardingTime; }

    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
}
