package org.example.Simulation;

import org.example.Model.ItemType;
import org.example.Model.Location;

import java.util.*;

import static org.example.Main.*;

public class ECNPSimulation {

    private record Bid(TransportAgent ta, int value) {}

    public SimulationResult run(Scenario scenario) {
        System.out.println("\n" + "#".repeat(65));
        System.out.println("eCNP Simulation: " + scenario.getName());
        System.out.println("#".repeat(65));

        List<WarehouseAgent> was = scenario.createWarehouseAgents();
        List<TransportAgent> tas = scenario.createTransportAgents();
        CNPSimulation.printInitialState(was, tas);

        int round = 0;
        boolean outerProgress = true;

        while ( outerProgress) {
            round++;
            outerProgress = false;
            System.out.println(ANSI_CYAN + "\n--- eCNP ROUND " + round + " ---" + ANSI_RESET);

            //BROADCAST CFPs
            Map<ItemType, List<WarehouseAgent>> available = new EnumMap<>(ItemType.class);
            for (WarehouseAgent wa : was){
                for (ItemType type : ItemType.values())
                {
                    if (wa.hasItem(type)){
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

            // Per-TA state for the provisional sub-loop

            // Warehouses where TA must not bid at again (WA already rejected it)
            Map<TransportAgent, Map<ItemType, Set<WarehouseAgent>>>exclusions = new LinkedHashMap<>();
            for (TransportAgent ta : tas) exclusions.put(ta, new EnumMap<>(ItemType.class));

            // Current provisional accept: for each TA, for each item, which WA accepted it
            Map<TransportAgent, Map<ItemType, WarehouseAgent>> provAccepted = new LinkedHashMap<>();
            for (TransportAgent ta : tas) provAccepted.put(ta, new EnumMap<>(ItemType.class));

            // For each (WA, item): the sorted list of TAs currently holding provisional accepts
            Map<WarehouseAgent, Map<ItemType, List<Bid>>>waAcceptedBids = new LinkedHashMap<>();

            // PROVISIONAL SUB-LOOP
            boolean provisionalChanged = true;
            while (provisionalChanged) {
                provisionalChanged = false;

                // Each TA bids on items it still needs AND doesn't yet have a provisional accept for
                Map<WarehouseAgent, Map<ItemType, List<Bid>>> newBids = new LinkedHashMap<>();

                for (TransportAgent ta : tas)
                {
                    Set<ItemType> stillNeeded = new LinkedHashSet<>(ta.getRemainingNeeds());
                    if (stillNeeded.isEmpty()) continue;

                    // Base tour: already contracted stops from previous outer round
                    List<Location> baseTour = new ArrayList<>(ta.getAcquiredItems().values());
                    // Also include stops already provisionally accepted this round
                    Map<ItemType, WarehouseAgent> myAccepts = provAccepted.get(ta);
                    for (WarehouseAgent acceptedWA : myAccepts.values()) {
                        baseTour.add(acceptedWA.getLocation());
                    }

                    for (ItemType needed : stillNeeded) {
                        // Skip if we already have a provisional accept for this item this round
                        if (myAccepts.containsKey(needed)) continue;

                        // build candidate list excluding warehouses that already rejected us
                        Set<WarehouseAgent> excluded = exclusions.get(ta).getOrDefault(needed, Collections.emptySet());
                        List<WarehouseAgent> candidates = new ArrayList<>(available.getOrDefault(needed, List.of()));
                        candidates.removeAll(excluded);
                        if (candidates.isEmpty()) continue;

                        // Pick cheapestt warehouse using full joint-tour cost
                        WarehouseAgent bestWA = null;
                        int lowestBid = Integer.MAX_VALUE;
                        for (WarehouseAgent wa : candidates) {
                            List<Location> testTour = new ArrayList<>(baseTour);
                            testTour.add(wa.getLocation());
                            int cost = RoutePlanner.calculateTourLength(ta.getFactory(), testTour);
                            if (cost < lowestBid) { lowestBid = cost; bestWA = wa; }
                        }

                        if (bestWA != null) {
                            System.out.printf("PROV_PROPOSE: %s -> %s  item=%s  bid=%d%n",
                                    ta.getId(), bestWA.getId(), needed, lowestBid);
                            newBids.computeIfAbsent(bestWA, k -> new LinkedHashMap<>())
                                    .computeIfAbsent(needed, k -> new ArrayList<>())
                                    .add(new Bid(ta, lowestBid));
                        }
                    }
                }

                if (newBids.isEmpty()) break; // stable — no new bids

                // Each LA merges incoming bids with its currently accepted set,
                // reranks, and issues new accepts/rejects
                for (WarehouseAgent wa : was)
                {
                    Map<ItemType, List<Bid>> incoming = newBids.getOrDefault(wa, Map.of());
                    for (Map.Entry<ItemType, List<Bid>> entry : incoming.entrySet()) {
                        ItemType type = entry.getKey();
                        int slots = wa.getItemCount(type); // remaining stock (not yet definitively awarded)

                        // Merge new bids with whatever this WA currently has accepted
                        List<Bid> currentlyAccepted = waAcceptedBids
                                .computeIfAbsent(wa, k -> new LinkedHashMap<>())
                                .getOrDefault(type, new ArrayList<>());
                        List<Bid> combined = new ArrayList<>(currentlyAccepted);
                        combined.addAll(entry.getValue());
                        combined.sort(Comparator.comparingInt(Bid::value));

                        Set<TransportAgent> previouslyAcceptedTAs = new HashSet<>();
                        for (Bid b : currentlyAccepted) previouslyAcceptedTAs.add(b.ta());

                        List<Bid> nowAccepted = new ArrayList<>();
                        List<Bid> nowRejected = new ArrayList<>();
                        for (int i = 0; i < combined.size(); i++) {
                            if (i < slots) nowAccepted.add(combined.get(i));
                            else nowRejected.add(combined.get(i));
                        }

                        // Update WAs accepted-bid record
                        waAcceptedBids.get(wa).put(type, nowAccepted);

                        // Issue provisional accepts to newly accepted TAs
                        for (Bid bid : nowAccepted) {
                            if (!previouslyAcceptedTAs.contains(bid.ta())) {
                                System.out.printf(ANSI_YELLOW +
                                                "  PROV_ACCEPT : %s -> %s  item=%s  bid=%d%n" + ANSI_RESET,
                                        wa.getId(), bid.ta().getId(), type, bid.value());
                                provAccepted.get(bid.ta()).put(type, wa);
                                provisionalChanged = true;
                            }
                        }

                        // Issue provisional rejects, displaced TAs lose their accept too
                        for (Bid bid : nowRejected) {
                            boolean wasAccepted = previouslyAcceptedTAs.contains(bid.ta());
                            System.out.printf(ANSI_RED +
                                            "  PROV_REJECT : %s -> %s  item=%s  bid=%d%n" + ANSI_RESET,
                                    wa.getId(), bid.ta().getId(), type, bid.value());
                            if (wasAccepted) {
                                // Displaced by a better bid and remove provisional accept
                                provAccepted.get(bid.ta()).remove(type);
                                provisionalChanged = true;
                            }
                            exclusions.get(bid.ta())
                                    .computeIfAbsent(type, k -> new HashSet<>())
                                    .add(wa);
                        }
                    }
                }
            }

            // DEFINITIVE PHASE
            Map<WarehouseAgent, Map<ItemType, List<Bid>>> definitiveBids = new LinkedHashMap<>();

            for (TransportAgent ta : tas) {
                Map<ItemType,WarehouseAgent>myAccepts = provAccepted.get(ta);
                if (myAccepts.isEmpty()) continue;

                // Full tour = already-contracted stops + all provisionally accepted stops
                List<Location> fullTour = new ArrayList<>(ta.getAcquiredItems().values());
                for (WarehouseAgent wa:myAccepts.values()) fullTour.add(wa.getLocation());

                // One definitive bid cost for the whole joint tour
                int jointCost = RoutePlanner.calculateTourLength(ta.getFactory(), fullTour);

                for (Map.Entry<ItemType, WarehouseAgent> e : myAccepts.entrySet()) {
                    System.out.printf("DEF_PROPOSE: %s -> %s  item=%s  bid=%d%n",
                            ta.getId(), e.getValue().getId(), e.getKey(), jointCost);
                    definitiveBids
                            .computeIfAbsent(e.getValue(), k -> new LinkedHashMap<>())
                            .computeIfAbsent(e.getKey(), k -> new ArrayList<>())
                            .add(new Bid(ta, jointCost));
                }
            }

            // Each LA definitively accepts the cheapest bidder(s) up to its stock
            for (WarehouseAgent wa :was){
                Map<ItemType, List<Bid>> defBids = definitiveBids.getOrDefault(wa, Map.of());

                for (Map.Entry<ItemType, List<Bid>> entry : defBids.entrySet())
                {
                    ItemType type = entry.getKey();
                    List<Bid> bids = new ArrayList<>(entry.getValue());
                    bids.sort(Comparator.comparingInt(Bid::value));

                    int slots= wa.getItemCount(type);
                    int awarded = 0;
                    for (Bid bid : bids) {
                        if (awarded < slots && wa.awardItem(type)){
                            bid.ta().contractWon(type, wa.getLocation());
                            System.out.printf(ANSI_GREEN + "  DEF_ACCEPT : %s -> %s  item=%s  bid=%d%n" + ANSI_RESET, wa.getId(), bid.ta().getId(), type, bid.value());
                            awarded++;
                            outerProgress = true;
                        } else
                        {
                            System.out.printf(ANSI_RED + "  DEF_REJECT : %s -> %s  item=%s  bid=%d%n" + ANSI_RESET, wa.getId(), bid.ta().getId(), type, bid.value());
                        }
                    }
                }
            }

            System.out.println();
            for (TransportAgent ta : tas)
            {
                System.out.printf("[%s] acquired=%s remaining=%s route=%d%n", ta.getId(), ta.getAcquiredItems().keySet(), ta.getRemainingNeeds(), ta.getTotalRouteLength());
            }
        }

        SimulationResult result = new SimulationResult("eCNP", scenario.getName(), tas, scenario.getPenalty(), round);
        result.printSummary();
        return result;
    }
}