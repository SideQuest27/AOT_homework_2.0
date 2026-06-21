package org.example.Simulation;

import org.example.Main;
import org.example.Model.ItemType;
import org.example.Model.Location;

import java.util.*;

import static org.example.Main.*;

public class CNPSimulation {

    private record Bid(TransportAgent ta, int value){}

    public SimulationResult run(Scenario scenario){
        System.out.println("\n" + "#".repeat(65));
        System.out.println("CNP Simulation: "+ scenario.getName());
        System.out.println("#".repeat(65));

        List<WarehouseAgent> was = scenario.createWarehouseAgents();
        List<TransportAgent> tas =scenario.createTransportAgents() ;

        printInitialState(was,tas);

        int round = 0;
        boolean progress = true;

        while (progress)
        {
            round++ ;
            progress = false;
            System.out.println(  ANSI_CYAN+"--- CNP ROUND "+ round+" ---"+ANSI_RESET);

            //BROADCAST CFPs
            Map<ItemType, List<WarehouseAgent>> available= new EnumMap<>(ItemType.class);

            for (WarehouseAgent wa : was)
            {
                for (ItemType type : ItemType.values()){
                    if(wa.hasItem(type))
                    {
                        available.computeIfAbsent(type, k -> new ArrayList<>()).add(wa);
                        System.out.printf("CFP   : %s broadcasts for item %s%n", wa.getId(), type);
                    }
                }
            }

            if (available.isEmpty())
            {
                System.out.println("No items left in any warehouse - STOPPING");
                break;
            }

            //CALCULATE AND SUBMIT PROPOSALS
            Map<WarehouseAgent , Map<ItemType, List<Bid>>> bidsPerWA = new LinkedHashMap<>();

            for (TransportAgent ta : tas)
            {
                //Track the joint-route progressively as the agent builds its bids
                List<Location> assumedTour = new ArrayList<>(ta.getAcquiredItems().values());

                for (ItemType needed : ta.getRemainingNeeds()){
                    List<WarehouseAgent> candidates = available.getOrDefault(needed, List.of());
                    if (candidates.isEmpty()) continue;

                    WarehouseAgent bestWA = null;
                    int lowestBid = Integer.MAX_VALUE;

                    // Calculate the Combined Total Tour Cost to add this warehouse
                    for (WarehouseAgent wa : candidates){
                        List<Location> testTour = new ArrayList<>(assumedTour);
                        testTour.add(wa.getLocation());
                        int testBid = RoutePlanner.calculateTourLength(ta.getFactory(), testTour);

                        if (testBid < lowestBid){
                            lowestBid = testBid;
                            bestWA = wa;
                        }
                    }

                    if (bestWA != null)
                    {
                        //Commit to this assumed path for the remaining bids in this round
                        assumedTour.add(bestWA.getLocation());

                        System.out.printf("PROPOSE: %s -> %s  item=%s  bid=%d%n", ta.getId(), bestWA.getId(), needed, lowestBid);

                        bidsPerWA
                                .computeIfAbsent(bestWA,x -> new LinkedHashMap<>())
                                .computeIfAbsent(needed, x-> new ArrayList<>())
                                .add(new Bid(ta, lowestBid));
                    }
                }
            }

            if(bidsPerWA.isEmpty()){
                System.out.println("No proposals submitted - stopping");
                break;
            }

            //EVALUATE BIDS AND AWARD
            for (WarehouseAgent wa : was){
                Map<ItemType, List<Bid>> typeBids = bidsPerWA.getOrDefault(wa, Map.of());

                for (Map.Entry<ItemType, List<Bid>> entry : typeBids.entrySet()){
                    ItemType type = entry.getKey();
                    List<Bid> bids = new ArrayList<>(entry.getValue());

                    // Award the contract to the agent with the lowest total joint-route cost
                    bids.sort(Comparator.comparingInt(Bid::value));

                    int slots = wa.getItemCount(type);
                    int awardedItems = 0;

                    for(Bid bid : bids){
                        if(awardedItems < slots && wa.awardItem(type)){
                            bid.ta().contractWon(type, wa.getLocation());
                            System.out.printf(ANSI_GREEN+"  ACCEPT : %s -> %s  item=%s  bid=%d%n"+ANSI_RESET, wa.getId(), bid.ta().getId(), type, bid.value());
                            awardedItems++;
                            progress = true;
                        } else{
                            System.out.printf(ANSI_RED+"  REJECT :%s -> %s  item=%s  bid=%d%n"+ANSI_RESET, wa.getId(), bid.ta().getId(), type, bid.value() );
                        }
                    }
                }
            }

            System.out.println();
            for (TransportAgent ta : tas){
                System.out.printf("[%s] acquired=%s remaining=%s route=%d%n",
                        ta.getId(),
                        ta.getAcquiredItems().keySet(),
                        ta.getRemainingNeeds(),
                        ta.getTotalRouteLength());
            }
        }

        SimulationResult result= new SimulationResult("CNP", scenario.getName(), tas, scenario.getPenalty(), round);
        result.printSummary();
        return result;
    }

    public static void printInitialState(List<WarehouseAgent> las, List<TransportAgent> tas) {
        System.out.println("\nInitial warehouses:");
        las.forEach(la-> System.out.println("  " + la));
        System.out.println("Initial transport agents:");
        tas.forEach(ta -> System.out.println("  " + ta));
    }
}