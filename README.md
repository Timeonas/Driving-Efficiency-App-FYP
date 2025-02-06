# Driving Efficiency App

An Android application that helps drivers evaluate and improve their driving efficiency through real-time monitoring and analysis. The app uses smartphone sensors and optional OBD-II adapter data to provide insights into driving behavior and fuel economy.

## Features

**Real-time Driving Analysis**
- Acceleration monitoring using phone sensors
- Speed tracking via GPS
- Optional OBD-II integration for detailed vehicle data
- Live map view during trips

**Trip Tracking**
- Detailed trip summaries
- Historical trip data analysis
- Duration and route recording
- Offline capability with data syncing

**User Management**
- Secure authentication with Firebase
- Profile customization
- Cloud data backup
- Cross-device synchronization

**Technical Implementation**
- Location services with background tracking
- Google Maps integration
- Sensor data processing
- Firebase Authentication and Cloud Firestore
- Material Design UI components

## Architecture

- **Language:** Kotlin
- **Platform:** Android
- **Minimum SDK:** API 21 (Android 5.0)
- **Dependencies:**
- Firebase Authentication
- Cloud Firestore
- Google Maps SDK
- Material Design Components
- Android Jetpack libraries

## Setup

1. Clone the repository
2. Set up Firebase project and add `google-services.json`
3. Configure Google Maps API key in `local.properties`
4. Build and run using Android Studio

## OBD-II Integration

The app supports ELM327-based OBD-II adapters for enhanced vehicle data collection:
- Real-time engine data
- Fuel consumption metrics
- Vehicle diagnostics
- Performance monitoring

## Future Enhancements

- Machine learning for driving behavior analysis
- Fuel efficiency predictions
- Route optimization
- Local fuel price integration
- Driving behavior classification
- Enhanced analytics dashboard

## Author

Tim Samoska  
Student ID: 21326923  
University of Galway  
School of Computer Science