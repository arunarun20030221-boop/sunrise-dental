package com.sunrise.dental.domain;

/**
 * A patient of the clinic. The brief requires a new patient to be registered with a name,
 * address and contact number; those are held here rather than repeated on every appointment
 * so that a returning patient is stored once.
 */
public class Patient {
    private Long id;
    private String name;
    private String address;
    private String contactNumber;
    private String email;

    public Patient() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public Patient(String name, String address, String contactNumber, String email) {
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
