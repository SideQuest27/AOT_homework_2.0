package org.example;

import org.example.Model.ItemType;
import org.example.Model.Location;
import org.example.Simulation.CNPSimulation;
import org.example.Simulation.ECNPSimulation;
import org.example.Simulation.Scenario;

import java.util.Map;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

    public static void main(String[] args) {

        CNPSimulation cnp  = new CNPSimulation();
        ECNPSimulation ecnp  = new ECNPSimulation();

        Scenario s1= buildScenario1();
        cnp.run(s1);

        Scenario s2= buildScenario1();
        ecnp.run(s2);

        Scenario s3= buildScenario2();
        cnp.run(s3);

        Scenario s4= buildScenario2();
        ecnp.run(s4);

    }

    private static Scenario buildScenario1() {
        return Scenario.builder("Scenario-2 (cascade displacement)")
                .warehouse("LA1", new Location(1, 1), Map.of(ItemType.A, 1))
                .warehouse("LA2", new Location(4, 1), Map.of(ItemType.A, 1))
                .warehouse("LA3", new Location(9, 9), Map.of(ItemType.A, 3))
                .site("TA1", new Location(1, 1), ItemType.A)
                .site("TA2", new Location(2, 1), ItemType.A)
                .site("TA3", new Location(7, 1), ItemType.A)
                .build(); // Better for eCNP
    }

    private static Scenario buildScenario2() {
        return Scenario.builder("Scenario-1 (lock-in effect)")
                .warehouse("LA1", new Location(1, 1), Map.of(ItemType.A, 1))
                .warehouse("LA2", new Location(2, 1), Map.of(ItemType.B, 1))
                .warehouse("LA3", new Location(9, 5), Map.of(ItemType.A, 1, ItemType.B, 1, ItemType.C, 2))
                .warehouse("LA4", new Location(1, 9), Map.of(ItemType.C, 1))
                .site("TA1", new Location(1, 1), ItemType.A, ItemType.B, ItemType.C)
                .site("TA2", new Location(1, 2), ItemType.A, ItemType.B, ItemType.C)
                .penalty(100)
                .build();
    } // Better For CNP shows the weakness of the eCNP implementation
}