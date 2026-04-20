package com.example.flightbooking.models;

public class Destination {
    private String city;
    private String country;
    private String price;
    private String imageUrl;
    private String imageBase64;
    private float rating;

    public Destination() {
        // Required for Firestore
    }

    public Destination(String city, String country, String price, String imageUrl, float rating) {
        this.city = city;
        this.country = country;
        this.price = price;
        this.imageUrl = imageUrl;
        this.rating = rating;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}
