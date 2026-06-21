package org.example.Simulation;

import org.example.Model.ItemType;
import org.example.Model.Location;

import java.util.*;

public class Scenario {
    public  record WarehouseConfig(String id, Location location, Map<ItemType, Integer> inventory){};
    public record SiteConfig(String id, Location location, Set<ItemType> neededItems){};
    private String name;
    private List<WarehouseConfig> warehouses;
    private List<SiteConfig> sites;
    private int penalty;
    private Scenario(Builder b) {
        this.name       = b.name;
        this.warehouses = List.copyOf(b.warehouses);
        this.sites      = List.copyOf(b.sites);
        this.penalty    = b.penalty;
    }

    public String getName(){
        return name;
    }
    public List<WarehouseConfig> getWarehouses() {
        return warehouses;
    }
    public List<SiteConfig> getSites(){
        return sites;
    }
    public int getPenalty(){
        return penalty;
    }

    public List<WarehouseAgent> createWarehouseAgents() {
        List<WarehouseAgent> agents = new ArrayList<>();
        for (WarehouseConfig w : warehouses) {
            agents.add(new WarehouseAgent(w.id(), w.location(), w.inventory()));
        }
        return agents;
    }

    public List<TransportAgent> createTransportAgents() {
        List<TransportAgent> agents = new ArrayList<>();
        for (SiteConfig s : sites) {
            agents.add(new TransportAgent(s.id(), s.location(), s.neededItems()));
        }
        return agents;
    }

    public static Builder builder(String name) { return new Builder(name); }

    public static final class Builder {
        private final String name;
        private final List<WarehouseConfig> warehouses = new ArrayList<>();
        private final List<SiteConfig> sites = new ArrayList<>();
        private int penalty = 100;

        private Builder(String name) { this.name = name; }

        public Builder warehouse(String id, Location loc, Map<ItemType, Integer> inv) {
            warehouses.add(new WarehouseConfig(id, loc, Map.copyOf(inv)));
            return this;
        }

        public Builder site(String id, Location loc, ItemType... items) {
            Set<ItemType> set = items.length == 0
                    ? EnumSet.noneOf(ItemType.class)
                    : EnumSet.copyOf(Arrays.asList(items));
            sites.add(new SiteConfig(id, loc, set));
            return this;
        }

        public Builder penalty(int p){
            this.penalty = p; return this;
        }

        public Scenario build(){
            return new Scenario(this);
        }
    }


}

