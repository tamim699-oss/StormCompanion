package com.stormcompanion.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> found = new LinkedHashMap<>();
    private LinearLayout list;
    private TextView status;
    private Button scanButton;
    private boolean scanning = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        buildUi();
        requestNeededPermissions();
        setStatus("Ready. Turn on the scooter and keep it stationary, then scan.");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(0xFFF4F7FA); scroll.addView(root);
        TextView title = new TextView(this); title.setText("STORM COMPANION"); title.setTextSize(24); title.setTextColor(0xFF102A43); title.setTypeface(null, 1); root.addView(title);
        TextView sub = new TextView(this); sub.setText("Nasir Syntax Storm 1500W • Bluetooth diagnostics"); sub.setTextSize(14); sub.setTextColor(0xFF486581); sub.setPadding(0, dp(5), 0, dp(16)); root.addView(sub);
        status = new TextView(this); status.setTextSize(15); status.setTextColor(0xFF243B53); status.setPadding(dp(12), dp(12), dp(12), dp(12)); status.setBackgroundColor(0xFFE1ECF4); root.addView(status);
        scanButton = new Button(this); scanButton.setText("Scan for scooter"); scanButton.setOnClickListener(v -> startScan()); root.addView(scanButton);
        Button disconnect = new Button(this); disconnect.setText("Disconnect"); disconnect.setOnClickListener(v -> disconnect()); root.addView(disconnect);
        TextView safety = new TextView(this); safety.setText("READ-ONLY PROTOTYPE\nThis version scans and lists Bluetooth services only. It does not send commands, change speed modes, unlock/lock the scooter, or control the motor. These features require a verified protocol."); safety.setTextSize(13); safety.setTextColor(0xFF7B341E); safety.setPadding(0, dp(8), 0, dp(14)); root.addView(safety);
        TextView devicesHeading = new TextView(this); devicesHeading.setText("NEARBY DEVICES"); devicesHeading.setTextColor(0xFF102A43); devicesHeading.setTypeface(null, 1); devicesHeading.setPadding(0, dp(8), 0, dp(8)); root.addView(devicesHeading);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        setContentView(scroll);
    }

    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + 0.5f); }
    private void setStatus(String text) { status.setText(text); }

    private void requestNeededPermissions() {
        ArrayList<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!needed.isEmpty()) requestPermissions(needed.toArray(new String[0]), 101);
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= 31) return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startScan() {
        if (!hasPermissions()) { requestNeededPermissions(); setStatus("Bluetooth permissions are required. Allow them and scan again."); return; }
        if (adapter == null) { setStatus("This phone does not expose a Bluetooth adapter."); return; }
        if (!adapter.isEnabled()) { setStatus("Turn Bluetooth on in Android settings, then scan again."); return; }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) { setStatus("BLE scanner unavailable. Check Bluetooth settings."); return; }
        found.clear(); list.removeAllViews(); scanning = true; scanButton.setEnabled(false); setStatus("Scanning for 12 seconds… Turn the scooter on and keep it nearby.");
        try { scanner.startScan(scanCallback); } catch (SecurityException e) { setStatus("Bluetooth permission denied."); scanButton.setEnabled(true); scanning = false; return; }
        handler.postDelayed(() -> stopScan("Scan complete. Tap a device to inspect its services."), 12000);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice d = result.getDevice();
            String key;
            try { key = d.getAddress(); } catch (SecurityException e) { return; }
            if (!found.containsKey(key)) { found.put(key, d); runOnUiThread(() -> addDevice(d, result.getRssi())); }
        }
        @Override public void onScanFailed(int errorCode) { runOnUiThread(() -> stopScan("Scan failed (code " + errorCode + "). Check permissions and Bluetooth.")); }
    };

    private void stopScan(String message) {
        if (scanning && scanner != null && hasPermissions()) { try { scanner.stopScan(scanCallback); } catch (Exception ignored) {} }
        scanning = false; scanButton.setEnabled(true); setStatus(message + " Found: " + found.size() + " device(s).");
    }

    private void addDevice(BluetoothDevice device, int rssi) {
        String name = "Unnamed Bluetooth device";
        try { if (device.getName() != null && !device.getName().trim().isEmpty()) name = device.getName(); } catch (SecurityException ignored) {}
        String address = "Address hidden"; try { address = device.getAddress(); } catch (SecurityException ignored) {}
        final String displayName = name;
        Button item = new Button(this); item.setAllCaps(false); item.setText(displayName + "\n" + address + " • RSSI " + rssi + " dBm\nTap to connect and inspect");
        item.setOnClickListener(v -> connect(device, displayName)); list.addView(item);
    }

    private void connect(BluetoothDevice device, String name) {
        if (!hasPermissions()) { requestNeededPermissions(); return; }
        disconnect(); setStatus("Connecting to " + name + "…");
        try { gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE); }
        catch (Exception e) { setStatus("Could not connect: " + e.getMessage()); }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int statusCode, int newState) {
            runOnUiThread(() -> {
                if (statusCode != BluetoothGatt.GATT_SUCCESS) { setStatus("Connection error " + statusCode + ". Try again with the scooter nearby."); return; }
                if (newState == BluetoothProfile.STATE_CONNECTED) { setStatus("Connected. Discovering services…"); g.discoverServices(); }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) setStatus("Disconnected.");
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt g, int statusCode) {
            runOnUiThread(() -> {
                if (statusCode != BluetoothGatt.GATT_SUCCESS) { setStatus("Connected, but service discovery failed: " + statusCode); return; }
                list.removeAllViews(); int count = 0;
                for (BluetoothGattService service : g.getServices()) {
                    TextView header = new TextView(MainActivity.this); header.setText("SERVICE  " + service.getUuid().toString()); header.setTextColor(0xFF102A43); header.setTypeface(null, 1); header.setPadding(0, dp(12), 0, dp(4)); list.addView(header); count++;
                    for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                        TextView row = new TextView(MainActivity.this);
                        row.setText("Characteristic: " + c.getUuid() + "\nProperties: " + properties(c.getProperties())); row.setTextSize(12); row.setTextColor(0xFF334E68); row.setPadding(dp(8), dp(6), dp(8), dp(6)); list.addView(row); count++;
                    }
                }
                setStatus("Connected. Found " + g.getServices().size() + " service(s). Service discovery does not prove which commands or telemetry are supported.");
                if (count == 0) { TextView empty = new TextView(MainActivity.this); empty.setText("No GATT services reported. This may be Classic Bluetooth, or the device may require its official app / pairing sequence."); list.addView(empty); }
            });
        }
    };

    private String properties(int p) {
        ArrayList<String> a = new ArrayList<>();
        if ((p & BluetoothGattCharacteristic.PROPERTY_READ) != 0) a.add("READ");
        if ((p & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) a.add("NOTIFY");
        if ((p & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) a.add("INDICATE");
        if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) a.add("WRITE");
        if ((p & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) a.add("WRITE_NO_RESPONSE");
        return a.isEmpty() ? "none advertised" : android.text.TextUtils.join(", ", a);
    }

    private void disconnect() {
        if (gatt != null) { try { gatt.disconnect(); gatt.close(); } catch (Exception ignored) {} gatt = null; }
    }

    @Override protected void onDestroy() { super.onDestroy(); if (scanning && scanner != null && hasPermissions()) { try { scanner.stopScan(scanCallback); } catch (Exception ignored) {} } disconnect(); handler.removeCallbacksAndMessages(null); }
}