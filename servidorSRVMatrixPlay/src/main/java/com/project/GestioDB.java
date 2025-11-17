package com.project;

import java.sql.Connection;
import java.sql.SQLException;

public class GestioDB {

    private static Connection connGeneral;

    // Obrir connexió un cop
    public static void iniciarConnexio() throws SQLException {
        if (connGeneral == null || connGeneral.isClosed()) {
            connGeneral = UtilsSQLite.connect("servidorSRVMatrixPlay/dades/log.sqlite");
        }
    }

    // Fer-la accessible
    public static Connection getConnexio() {
        return connGeneral;
    }

    public static void crearDB() throws SQLException {
        try (Connection conn = UtilsSQLite.connect("servidorSRVMatrixPlay/dades/log.sqlite")) {
            UtilsSQLite.queryUpdate(conn, "DROP TABLE IF EXISTS log");
            UtilsSQLite.queryUpdate(conn,
                    "CREATE TABLE IF NOT EXISTS log ("
                    + " id integer PRIMARY KEY AUTOINCREMENT,"
                    + " valor text NOT NULL,"
                    + " data text NOT NULL);");
        }
    }

    // Usa la connexió general
    public static void afegeixEntradaLog(String text, String data) throws SQLException {
        UtilsSQLite.queryUpdatePS(connGeneral,
                "INSERT INTO log (valor, data) VALUES (?, ?)",
                text, data);
    }
}
