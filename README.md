# Jatra Driver

**Jatra** is a smart city bus tracking and navigation system designed to improve public transportation efficiency and user experience. This repository contains the **Driver App**, which allows bus drivers to broadcast their real-time location and manage their trips.

## Features

-   **Real-time Location Tracking**: Broadcasts the bus's current location to the Firebase Realtime Database.
-   **Driver Registration**: Allows drivers to register and log in to the system.
-   **Trip Management**: Tools for drivers to start and end trips (future scope).

## Technologies Used

-   **Android**: Native Android development using Java/Kotlin.
-   **Firebase**:
    -   Realtime Database for location syncing.
    -   Authentication for driver management.
-   **Google Maps SDK**: For location services and mapping.

## Setup Instructions

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/enamulhasan248/Jatra-A-Smart-City-Bus-Tracking-Navigation-System.git
    ```
2.  **Open in Android Studio**:
    -   Open Android Studio and select "Open an existing Android Studio project".
    -   Navigate to the cloned directory and select it.
3.  **Firebase Configuration**:
    -   Ensure `google-services.json` is present in the `app/` directory. If not, download it from your Firebase Console.
4.  **Build and Run**:
    -   Sync Gradle files.
    -   Connect an Android device or use an emulator.
    -   Run the application.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

## License

[Add License Information Here]
