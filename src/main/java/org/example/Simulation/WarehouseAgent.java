package org.example.Simulation;

import org.example.Main;
import org.example.Model.ItemType;
import org.example.Model.Location;

import java.util.HashMap;
import java.util.Map;

public class WarehouseAgent {
    private String id;
    private Location location;
    private Map<ItemType, Integer> inventory;

    public WarehouseAgent(String id, Location location, Map<ItemType, Integer> inventory) {
        this.id = id;
        this.location = location;
        this.inventory = new HashMap<>(inventory);
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public Map<ItemType, Integer> getInventory() {
        return inventory;
    }

    public boolean hasItem(ItemType type){
        return inventory.getOrDefault(type, 0) > 0;
    }

    public int getItemCount(ItemType type){
        return inventory.getOrDefault(type, 0);
    }

    public boolean awardItem(ItemType type){
        int count = getItemCount(type);
        if (count > 0){
            inventory.put(type, count-1);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[" + id + " @" + location + " inv=" + inventory + "]";
    }
}
