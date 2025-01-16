package com.example.drivingefficiencyapp
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.main_menu)

        //val startDriveButton = findViewById<Button>(R.id.startDriveButton)
        //val viewTripsButton = findViewById<Button>(R.id.viewTripsButton)

        // Starts driving activity, but doesn't exist yet
        //startDriveButton.setOnClickListener {
            // val intent = Intent(this, DriveActivity::class.java)
            // startActivity(intent)
        //}

        // Start trip activity, but doesn't exist yet
        //viewTripsButton.setOnClickListener {
            // val intent = Intent(this, TripsActivity::class.java)
            // startActivity(intent)
        //}
    }
}