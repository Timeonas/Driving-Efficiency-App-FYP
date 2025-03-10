Improvements and Final Touches:
- Fix the orientation, right now while driving the map is not oriented correctly for some reason
- Fix Bluetooth functionality, sometimes it does not connect to the OBD2 device
- Fix instant fuel rate, not accurate as of right now
- Implement acceleration and deceleration data

Appearance (To be done after core app is done if time permits):
- Better App Logo
- Improve the appearance of the app, with a better color scheme and more modern design
- Add animations to the app, such as when a trip starts or ends
- Come up with an app name

~~Driving Efficiency Score~~:
- ~~Implement a driving efficiency score, that takes into account the user's driving habits and gives them a score~~
- ~~Used the data in the trips to calculate the score~~
- ~~Explore ML models to predict the score~~

Core App:
- ~~Implement a map view to show the user's current location when drive starts~~
- ~~Parse basic information from the user's phone, such as the current speed, and display it on the screen~~
- ~~Tilt the map view so its in 3d mode~~
- ~~Orient the map view so that it is always facing the direction the user is driving~~
- ~~Implement a trip summary screen that shows the user's trip information, such as the distance traveled,
  the time taken, and the average speed~~
- ~~Add icon on map to show the user's current location~~
- ~~Add ability to delete trips~~
- ~~Add offline functionality, so that trips are cached locally if the user is offline~~
- ~~Make the tracking trip notification not cancelable~~
- ~~Replace trip basic data in the db with the OBD2 trip~~

Skeleton for App (complete):
- ~~Implement a cloud-based database to store trips~~
- ~~Reset Password functionality~~
- ~~Ability to add a profile picture to the profile screen~~
- ~~Store profile picture in Firebase Storage, and retrieve it when a user signs in~~

OBD2 Functionality (complete):
- ~~Create a simple kotlin app that communicates with the OBD2 device first~~
- ~~Parse any kind of information from the OBD2 device~~
- ~~Try to parse good data relevant to the project, such as fuel consumption, speed, gear, etc.~~
- ~~Using rpm, gear ratios and speed, estimate gear~~
- ~~Implement fuel related parameters~~
~~- **Implement into the main project**~~
    - ~~Move the OBD2 connect functionality into the main menu.~~
    - ~~Once connected, allow the user to enter the drive screen~~.
    - ~~Display the instant OBD2 data (speed, rpm) on the drive screen~~.
    - ~~Once a user finishes a drive, display the trip summary screen with the OBD2 trip summary data.~~
    - ~~Keep OBD activity for test purposes for now, move the connection logic to the main menu.~~

Project:
- ~~Sort the Kotlin files into different files and folders to make it more organized~~
- ~~Develop the README.md, with instructions on how to build and run the app~~
- Fix linting issues
- ~~Refactor StartDriveActivity.kt to reduce code repetition !!~~

Bugs:
- ~~Fix the bug where the app crashes when the user tries to start a trip a second time back to back~~
- ~~Sometimes on first instance of saving a trip, error job cancelled~~
- ~~Sometimes location does not work on my phone~~
- ~~Fix the default icon appearing for a 1-2 seconds before the pfp loads~~