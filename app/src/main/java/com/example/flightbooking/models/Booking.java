package com.example.flightbooking.models;

public class Booking {
    private String bookingId;
    /** Firebase Auth UID of the customer who owns this booking */
    private String userId;
    private Flight flight;
    private String passengerName;
    private int age;
    private String gender;
    private String travelClass;
    private String status;
    private String date; // Booking creation date
    private String flightDate;
    private String gate;
    private String seat;
    private String pnrCode;

    // Real-world airline fields
    private String dob; 
    private String emailAddress;
    private String phoneNumber;
    private String totalAmount;
    private String nationality;
    private String passportNumber;
    private String passportExpiry;

    // Legacy fields for backward compatibility with older Firestore records
    private String fromCode;
    private String toCode;
    private String price;
    private String airlineName;
    private String flightNumber;
    private String fromTime;
    private String toTime;

    // No-argument constructor for Firestore
    public Booking() {}

    public Booking(String bookingId, Flight flight, String passengerName, int age, String gender, 
                   String travelClass, String status, String date, String flightDate, 
                   String gate, String seat, String pnrCode) {
        this.bookingId = bookingId;
        this.flight = flight;
        this.passengerName = passengerName;
        this.age = age;
        this.gender = gender;
        this.travelClass = travelClass;
        this.status = status;
        this.date = date;
        this.flightDate = flightDate;
        this.gate = gate;
        this.seat = seat;
        this.pnrCode = pnrCode;
    }

    // Legacy constructor for backward compatibility
    public Booking(String bookingId, Flight flight, String passengerName, String status, String date) {
        this(bookingId, flight, passengerName, 25, "Male", "Economy", status, date, "2026-05-15", "B-12", "14A", "ET829W");
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Flight getFlight() { return flight; }
    public void setFlight(Flight flight) { this.flight = flight; }
    
    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getTravelClass() { return travelClass; }
    public void setTravelClass(String travelClass) { this.travelClass = travelClass; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getFlightDate() { return flightDate; }
    public void setFlightDate(String flightDate) { this.flightDate = flightDate; }
    
    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
    
    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }
    
    public String getPnrCode() { return pnrCode; }
    public void setPnrCode(String pnrCode) { this.pnrCode = pnrCode; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getTotalAmount() { return totalAmount; }
    public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getPassportExpiry() { return passportExpiry; }
    public void setPassportExpiry(String passportExpiry) { this.passportExpiry = passportExpiry; }

    // Legacy Getters and Setters
    public String getFromCode() { return fromCode; }
    public void setFromCode(String fromCode) { this.fromCode = fromCode; }

    public String getToCode() { return toCode; }
    public void setToCode(String toCode) { this.toCode = toCode; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getFromTime() { return fromTime; }
    public void setFromTime(String fromTime) { this.fromTime = fromTime; }

    public String getToTime() { return toTime; }
    public void setToTime(String toTime) { this.toTime = toTime; }
}
