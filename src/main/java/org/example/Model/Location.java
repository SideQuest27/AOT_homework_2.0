package org.example.Model;

public class Location {

    int x;
    int y;

    public Location(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int distanceTo(Location otherLocation){
        return Math.abs(this.x - otherLocation.x) + Math.abs(this.y - otherLocation.y);
    }

    @Override
    public String toString() {
        return "("+x+","+y+")";
    }
}
