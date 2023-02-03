package com.hbrtrdzk.wifigeolocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private Button scanWifi, findLocation;
    private List<ScanResult> results;
    final private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter arrayAdapter;
    private BroadcastReceiver wifiReceiver;
    private LocationManager locationManager;
    private ServerConnector serverConnector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Plume_design_project);
        setContentView(R.layout.activity_main);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.plume_background));
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        scanWifi = findViewById(R.id.btn_scanWiFI);
        findLocation = findViewById(R.id.btn_findMe);
        ListView listView = findViewById(R.id.list_WIFI);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        scanWifi.setEnabled(true);
        findLocation.setEnabled(false);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ArrayList<String> scannedAccessPoints = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                return view;
            }
        };
        listView.setAdapter(arrayAdapter);
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scannedAccessPoints.clear();
                arrayList.clear();
                scannedAccessPoints.add("SSID,BSSID,LEVEL");
                results = wifiManager.getScanResults();

                for (ScanResult scanResult : results) {
                    arrayList.add("SSID: " + scanResult.SSID + "\nBSSID: " + scanResult.BSSID + "\nLEVEL: "
                            + scanResult.level + " dBm\n");
                    scannedAccessPoints.add(scanResult.SSID + "," + scanResult.BSSID + "," + scanResult.level);
                    arrayAdapter.notifyDataSetChanged();
                }
                if (arrayList.size() < 3) {
                    findLocation.setEnabled(false);
                    Toast.makeText(MainActivity.this, "You need at least 3 access points.", Toast.LENGTH_SHORT).show();
                } else {
                    findLocation.setEnabled(true);
                }
                unregisterReceiver(wifiReceiver);
                try {
                    writeAccessPointsToCSV(scannedAccessPoints);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                scanWifi.setEnabled(true);
            }
        };
        scanWifi.setOnClickListener(v -> {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                alertMessageNoGps();
            } else {
                scanWifi.setEnabled(false);
                scanWifi(wifiManager);
            }
        });
        findLocation.setOnClickListener(v -> {
            if (checkInternetConnection()) {
                serverConnector = new ServerConnector(MainActivity.this);
                serverConnector.start();
                try {
                    serverConnector.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                    startActivityForResult(panelIntent, 545);
                } else {
                    alertRestartWIFI();
                }
            }
        });
    }

    private boolean checkInternetConnection() {
        try {
            String command = "ping -c 1 google.com";
            return (Runtime.getRuntime().exec(command).waitFor() == 0);
        } catch (Exception e) {
            return false;
        }
    }

    private void writeAccessPointsToCSV(@NonNull ArrayList<String> scannedAccessPoints) throws IOException {
        String filename = "AccessPoints.csv";
        File path = getApplicationContext().getFilesDir();
        FileOutputStream fileOutputStream = new FileOutputStream(new File(path, filename));
        for (String x : scannedAccessPoints) {
            fileOutputStream.write(x.getBytes(StandardCharsets.UTF_8));
            fileOutputStream.write('\n');
        }
        fileOutputStream.close();
    }

    void scanWifi(WifiManager wifiManagerToScan) {
        String[] perm = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE };
        if (EasyPermissions.hasPermissions(this, perm)) {
            boolean scanStarted = wifiManagerToScan.startScan();
            if (scanStarted) {
                registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Problem occurred, try to reset your WIFI.", Toast.LENGTH_LONG).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    startActivityForResult(panelIntent, 545);
                } else {
                    alertRestartWIFI();
                }
                scanWifi.setEnabled(true);
            }
        } else {
            scanWifi.setEnabled(true);
            EasyPermissions.requestPermissions(this, "WE REQUIRE THESE PERMISSIONS FOR THE APP TO FUNCTION", 123, perm);
        }
    }

    private void alertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("To continue let your device turn on location.")
                .setCancelable(false)
                .setPositiveButton("Ok", (dialog, id) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void alertRestartWIFI() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Problem occurred, try to reset your WIFI.")
                .setCancelable(false)
                .setPositiveButton("Ok", (dialog, id) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private class ServerConnector extends Thread {
        Context context;

        ServerConnector(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                Socket socket = new Socket("127.0.0.1", 5000);
                socket.setSoTimeout(1000);
                OutputStream outputStream = socket.getOutputStream();
                File path = getApplicationContext().getFilesDir();
                File file = new File(path, "AccessPoints.csv");
                FileInputStream fileInputStream = new FileInputStream(file);

                byte[] bytes = new byte[(int) file.length()];
                fileInputStream.read(bytes, 0, bytes.length);
                outputStream.write(bytes, 0, bytes.length);

                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String output = bufferedReader.readLine();

                bufferedReader.close();
                inputStreamReader.close();
                outputStream.close();
                socket.close();
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("Location", output);
                startActivity(intent);
            } catch (IOException e) {
                Activity a = MainActivity.this;
                a.runOnUiThread(() -> Toast.makeText(context, "Timeout error", Toast.LENGTH_SHORT).show());
            }
        }
    }
}