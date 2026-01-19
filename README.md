# Jatra - Driver App 🚌📲

**Jatra** is a smart city bus tracking and navigation ecosystem designed to modernize public transit in Dhaka. This repository contains the source code for the **Driver Side Application**, which serves as the primary telemetry unit for the system. It turns the driver's smartphone into a real-time tracking device, broadcasting location data to the central server while ensuring minimal battery drain and high data accuracy.

## 🚀 Application Role
The Driver App is responsible for:
* **Authentication:** Verifying drivers and linking them to specific bus routes.
* **Telemetry Broadcasting:** Continuously capturing and uploading GPS coordinates to Firebase.
* **Data Validation:** Filtering out noise and errors from raw GPS sensor data before upload.
* [cite_start]**Status Management:** Allowing drivers to toggle "On-Duty" and "Off-Duty" status to preserve privacy[cite: 1330].

## 🧠 Algorithms Used
[cite_start]This application implements a **3-Layer Validation Algorithm** to ensure data integrity [cite: 875-938]:
1.  **Accuracy Filter:** Discards any GPS update with a confidence radius > 50 meters to prevent "noisy" location plotting.
2.  **Speed Validator:** Uses physics-based constraints to reject "teleporting" updates where the implied speed exceeds 100 km/h (27.7 m/s).
3.  **Stationary Drift Filter:** Ignores micro-movements (<10 meters) when the bus is stopped in traffic, preventing the icon from "dancing" on the map.

## 🛠 Tech Stack
* **Language:** Java (Android Native)
* **Cloud Backend:** Firebase Realtime Database
* [cite_start]**Location Services:** Google FusedLocationProviderClient API [cite: 805]
* [cite_start]**Background Processing:** Android Foreground Services (with persistent notification) [cite: 731]

## ⚡ Challenges Solved
* [cite_start]**Battery Drain:** Optimized the update intervals of the `FusedLocationProviderClient` to balance tracking precision with power efficiency for long shifts[cite: 736].
* [cite_start]**Background Execution:** Overcame Android's strict background limitations (Doze Mode) by implementing a high-priority Foreground Service to keep the GPS active[cite: 731].
* [cite_start]**GPS Teleporting:** Solved erratic location jumps caused by network switching (WiFi/Cellular) using the custom Speed Validator algorithm[cite: 898].

## 🐛 Bug Reporting
Found a bug? Please open an issue in this repository or contact me directly at **enamulhasan248@gmail.com**.
