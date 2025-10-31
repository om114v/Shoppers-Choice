package com.shopper.repositories;

import com.shopper.models.ShopProfile;
import com.shopper.services.DatabaseService;

/**
 * Repository for managing shop profile data.
 */
public class ProfileRepository {

    private final DatabaseService dbService;

    public ProfileRepository() {
        this.dbService = DatabaseService.getInstance();
    }

    /**
     * Retrieves the shop profile from the database.
     * @return the shop profile or null if not found
     */
    public ShopProfile getShopProfile() {
        return dbService.getShopProfile();
    }

    /**
     * Saves the shop profile to the database.
     * Inserts if new, updates if existing.
     * @param profile the shop profile to save
     */
    public void saveShopProfile(ShopProfile profile) {
        if (profile.getId() > 0) {
            dbService.updateShopProfile(profile);
        } else {
            dbService.insertShopProfile(profile);
        }
    }

    /**
     * Deletes the shop profile from the database.
     */
    public void deleteShopProfile() {
        dbService.deleteShopProfile();
    }
}