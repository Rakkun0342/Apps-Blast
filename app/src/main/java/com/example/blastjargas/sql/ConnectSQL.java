package com.example.blastjargas.sql;

import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectSQL {
    String ip = "";
    String db = "";
    String user = "";
    String pass = "";
    public Connection connection() {
        StrictMode.ThreadPolicy p = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(p);
        Connection con = null;
        String url = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            url = "jdbc:jtds:sqlserver://" + ip + ";" + "databaseName=" + db + ";" + "user=" + user + ";" + "password=" + pass + ";";
            con = DriverManager.getConnection(url);
        } catch (SQLException a){
            Log.e("Error SQL:", a.getMessage());
        } catch (Exception e) {
            Log.e("Error :", e.getMessage());
        }
        return con;
    }
}
