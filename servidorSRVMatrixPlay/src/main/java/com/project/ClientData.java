package com.project;

import org.json.JSONObject;

public class ClientData {
    public String name;
    public String color;
    public int mouseX;
    public int mouseY;


    public ClientData(String name, String color) {
        this.name = name;
        this.color = color;
        this.mouseX = -1;
        this.mouseY = -1;

    }

    public ClientData(String name, String color, int mouseX, int mouseY) {
        this.name = name;
        this.color = color;
        this.mouseX = mouseX;
        this.mouseY = mouseY;

    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("color", color);
        obj.put("mouseX", mouseX);
        obj.put("mouseY", mouseY);

        return obj;
    }

    // Crea un ClientData a partir de JSON
    public static ClientData fromJSON(JSONObject obj) {
        String name = obj.optString("name", null);
        String color = obj.optString("color", null);

        ClientData cd = new ClientData(name, color);
        cd.mouseX = obj.optInt("mouseX", -1);
        cd.mouseY = obj.optInt("mouseY", -1);

        return cd;
    }
}
