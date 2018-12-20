package com.basava.carzonrenttest.database;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

@Table(name = "UserLocation")
public class UserLocation extends Model {
    // This is the unique id given by the server
    @Column(name = "location_id", unique = true, onUniqueConflict = Column.ConflictAction.IGNORE)
    public long locationId;
    // This is a regular field
    @Column(name = "latitude")
    public double latitude;
    @Column(name = "longitude")
    public double longitude;

    // Make sure to have a default constructor for every ActiveAndroid model
    public UserLocation() {
        super();
    }

    public UserLocation(int locationId, double latitude, double longitude) {
        super();
        this.locationId = locationId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static List<UserLocation> getUserLocations() {
        return new Select().from(UserLocation.class).execute();
    }
}
