package com.project;

import org.json.JSONObject;

public class GameObject {
    public String id;
    public int x;
    public int y;
    public int ancho;
    public int alto;
    public String color; 
    public GameObject(String id, int x, int y, int ancho, int alto,String color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }
    
    // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("ancho", ancho);
        obj.put("alto", alto);
        obj.put("color", color);
        return obj;
    }

    // Crea un GameObjects a partir de JSON
    public static GameObject fromJSON(JSONObject obj) {
        GameObject go = new GameObject(
            obj.optString("id", null),
            obj.optInt("x", 0),
            obj.optInt("y", 0),
            obj.optInt("ancho", 1),
            obj.optInt("alto", 1),
            obj.optString("color", "gray")
        );
        return go;
    }
}
