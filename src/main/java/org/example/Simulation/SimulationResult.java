package org.example.Simulation;

import java.util.List;

public class SimulationResult {
    private final String protocol;
    private final String scenario;
    private final List<TransportAgent> agents;
    private final int penalty;
    private final int rounds;

    public SimulationResult(String protocol, String scenario, List<TransportAgent> agents, int penalty, int rounds) {
        this.protocol= protocol;
        this.scenario = scenario;
        this.agents = List.copyOf(agents);
        this.penalty = penalty;
        this.rounds = rounds;
    }

    public int getTotalRouteCost() {
        return agents.stream().mapToInt(TransportAgent::getTotalRouteLength).sum();
    }
    public int getUnfulfilledOrderCount() {
        return agents.stream().mapToInt(TransportAgent::getUnfulfilledOrderCount).sum();
    }
    public int getTotalPenalty() {
        return agents.stream().mapToInt(ta -> ta.getPenaltyCost(penalty)).sum();
    }
    public int getTotalCost() {
        return getTotalRouteCost() + getTotalPenalty();
    }

    public void printSummary() {
        String sep = "=".repeat(70);
        System.out.println();
        System.out.println(sep);
        System.out.printf("  RESULT  |  %s  |  Scenario: %s%n", protocol, scenario);
        System.out.println(sep);
        for (TransportAgent ta : agents) {
            int unfulfilled = ta.getUnfulfilledOrderCount();
            String status = unfulfilled == 0
                    ? "OK"
                    : String.format("PENALTY %d unfulfilled x %d EE = %d EE",
                    unfulfilled, penalty, ta.getPenaltyCost(penalty));
            System.out.printf("  %-6s  route=%3d EE  acquired=%s  unfulfilled=%s  %s%n",
                    ta.getId(),
                    ta.getTotalRouteLength(),
                    ta.getAcquiredItems().keySet(),
                    ta.getRemainingNeeds(),
                    status);
        }
        System.out.println(sep);
        System.out.printf("  Total route cost : %d EE (Bewegungsschritte)%n", getTotalRouteCost());
        System.out.printf("  Total penalty    : %d EE (%d unfulfilled orders x %d EE)%n",
                getTotalPenalty(), getUnfulfilledOrderCount(), penalty);
        System.out.printf("  Total cost       : %d EE%n", getTotalCost());
        System.out.printf("  Rounds           : %d%n", rounds);
        System.out.println(sep);
    }
}
