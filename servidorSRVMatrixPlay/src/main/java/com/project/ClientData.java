package com.project;

import org.json.JSONObject;

public class ClientData {
    public String name;
    public String color;
    public int palaX; 
    public int palaY; 
    public int punts; 

    public ClientData(String name, String color) {
        this(name, color, 0, 0, 0);
    }

    public ClientData(String name, String color, int palaX, int palaY, int punts) {
        this.name = name;
        this.color = color;
        this.palaX = palaX;
        this.palaY = palaY;
        this.punts = punts;
    }

    
    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("color", color);
        obj.put("palaX", palaX);
        obj.put("palaY", palaY);
        obj.put("punts", punts);
        return obj;
    }


    public static ClientData fromJSON(JSONObject obj) {
        String name = obj.optString("name", "Unknown");
        String color = obj.optString("color", "gray");
        int palaX = obj.optInt("palaX", 0);
        int palaY = obj.optInt("palaY", 0);
        int punts = obj.optInt("punts", 0);
        return new ClientData(name, color, palaX, palaY, punts);
    }
}
