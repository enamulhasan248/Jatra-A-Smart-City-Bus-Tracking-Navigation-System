# Jatra - Driver App 🚌📲

**Jatra** is a smart city bus tracking and navigation ecosystem designed to modernize public transit in Dhaka. This repository contains the source code for the **Driver Side Application**, which serves as the primary telemetry unit for the system. It turns the driver's smartphone into a real-time tracking device, broadcasting location data to the central server while ensuring minimal battery drain and high data accuracy.

## 🚀 Application Role & Features
The Driver App is responsible for:
* **Telemetry Broadcasting:** Continuously capturing and uploading GPS coordinates to the Firebase Realtime Database.
* **Data Validation:** Filtering out noise and errors from raw GPS sensor data before upload.
* **Authentication:** Verifying drivers and linking them to specific bus routes via Firebase Authentication.
* **Status Management:** Allowing drivers to toggle "On-Duty" and "Off-Duty" status to preserve privacy.
* **Trip Management:** Tools for drivers to start and end trips (future scope).

## 🧠 Algorithms Used
This application implements a **3-Layer Validation Algorithm** to ensure data integrity.
1.  **Accuracy Filter:** Discards any GPS update with a confidence radius > 50 meters to prevent "noisy" location plotting.
2.  **Speed Validator:** Uses physics-based constraints to reject "teleporting" updates where the implied speed exceeds 100 km/h (27.7 m/s).
3.  **Stationary Drift Filter:** Ignores micro-movements (<10 meters) when the bus is stopped in traffic, preventing the icon from "dancing" on the map.

## ⚡ Challenges Solved
* **Battery Drain:** Optimized the update intervals of the `FusedLocationProviderClient` to balance tracking precision with power efficiency for long shifts.
* **Background Execution:** Overcame Android's strict background limitations (Doze Mode) by implementing a high-priority Foreground Service to keep the GPS active.
* **GPS Teleporting:** Solved erratic location jumps caused by network switching (WiFi/Cellular) using the custom Speed Validator algorithm.

## 🛠 Tech Stack
* **Language:** Java (Android Native)
* **Cloud Backend:**
    * **Firebase Realtime Database:** For real-time location syncing.
    * **Firebase Authentication:** For driver management.
* **Location Services:**
    * **Google Maps SDK:** For location services and mapping interface.
    * **FusedLocationProviderClient API:** For efficient battery-aware location tracking.
* **Background Processing:** Android Foreground Services (with persistent notification).

## ⚙️ Setup Instructions

1.  **Clone the Repository**:
    ```bash
    git clone [https://github.com/enamulhasan248/Jatra-A-Smart-City-Bus-Tracking-Navigation-System.git](https://github.com/enamulhasan248/Jatra-A-Smart-City-Bus-Tracking-Navigation-System.git)
    ```
2.  **Open in Android Studio**:
    * Open Android Studio and select "Open an existing Android Studio project".
    * Navigate to the cloned directory and select it.
3.  **Firebase Configuration**:
    * Ensure `google-services.json` is present in the `app/` directory. If not, download it from your Firebase Console.
4.  **Build and Run**:
    * Sync Gradle files.
    * Connect an Android device or use an emulator.
    * Run the application.

## 🤝 Contributing
Contributions are welcome! Please fork the repository and submit a pull request.

## 🐛 Bug Reporting
Found a bug? Please open an issue in this repository or contact me directly at **enamulhasan248@gmail.com**.

## License
[Add License Information Here. Not yet licensed.]
