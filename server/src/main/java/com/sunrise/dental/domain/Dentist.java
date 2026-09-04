package com.sunrise.dental.domain;

/**
 * A dentist who can be assigned to appointments.
 */
public class Dentist {
    private Long id;
    private String name;
    private String speciality;
    private boolean active = true;

    public Dentist() {
        // JavaBean constructor, used by the DAO layer when building an object from a ResultSet
    }

    public Dentist(String name, String speciality) {
        this.name = name;
        this.speciality = speciality;
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

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
