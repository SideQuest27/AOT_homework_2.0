package org.example.Simulation;

import org.example.Model.ItemType;
import org.example.Model.Location;

import java.util.*;

public class TransportAgent {

    private String id;
    private Location factory;
    private Set<ItemType> neededItems;
    private Map<ItemType, Location> acquiredItems = new EnumMap<>(ItemType.class);

    public TransportAgent(String id, Location factory, Set<ItemType> neededItems){
        this.id= id;
        this.factory = factory;
        this.neededItems = neededItems.isEmpty() ? EnumSet.noneOf(ItemType.class) : EnumSet.copyOf(neededItems);
    }

    public String getId() {
        return id;
    }
    public Location getFactory() {
        return factory;
    }
    public Set<ItemType> getNeededItems() {
        return neededItems;
    }
    public Map<ItemType, Location> getAcquiredItems() {
        return acquiredItems;
    }
    public Set<ItemType> getRemainingNeeds(){
        Set<ItemType> remaining = new LinkedHashSet<>(neededItems);
        remaining.removeAll(acquiredItems.keySet());
        return remaining;
    }
    public int getUnfulfilledOrderCount(){
        return getRemainingNeeds().size();
    }
    public int getPenaltyCost(int penaltyPerOrder){
        return getUnfulfilledOrderCount() * penaltyPerOrder;
    }


    public Location contractWon(ItemType item, Location location){
        return acquiredItems.put(item, location);
    }

    public int calculateBid(Location warehouseLocation){
        List<Location> current = new ArrayList<>(acquiredItems.values());
        return RoutePlanner.marginalCost(factory, current, warehouseLocation);
    }

    public int getTotalRouteLength() {
        return RoutePlanner.calculateTourLength(factory, new ArrayList<>(acquiredItems.values()));
    }

    @Override
    public String toString() {
        return "[" + id + " @" + factory + " needs=" + neededItems + " acquired=" + acquiredItems.keySet() + " route=" + getTotalRouteLength() + "]";
    }

}

