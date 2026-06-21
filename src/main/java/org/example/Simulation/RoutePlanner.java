package org.example.Simulation;

import org.example.Model.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class RoutePlanner {

    public static int calculateTourLength(Location factory, List<Location> waypoints){
        if(waypoints.isEmpty()) return 0;

        int n = waypoints.size();
        int[] indices = IntStream.range(0, n).toArray();
        int min = Integer.MAX_VALUE;

        do{
            int dist = factory.distanceTo(waypoints.get(indices[0]));
            for (int i = 0; i < n - 1; i++){
                dist += waypoints.get(indices[i]).distanceTo(waypoints.get(indices[i+1]));
            }

            if (dist < min) min = dist;
        } while (nextPermutation(indices));

        return min;
    }

    private static boolean nextPermutation(int[] a) {
        int n = a.length;
        int i = n - 2;
        while (i >= 0 && a[i] >= a[i + 1]) i--;
        if (i < 0) return false;
        int j = n - 1;
        while (a[j] <= a[i]) j--;
        swap(a, i, j);
        reverse(a, i + 1, n - 1);
        return true;
    }

    public static int marginalCost(Location factory, List<Location> existing, Location newPoint){
        List<Location> extended = new ArrayList<>(existing);
        extended.add(newPoint);
        return calculateTourLength(factory, extended) - calculateTourLength(factory, existing);
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }

    private static void reverse(int[] a, int lo, int hi) {
        while (lo < hi) swap(a, lo++, hi--);
    }
}