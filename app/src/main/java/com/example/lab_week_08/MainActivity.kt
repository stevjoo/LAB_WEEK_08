package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy {
        WorkManager.getInstance(this)
    }

    private var serviceToLaunch: Int = 0

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (serviceToLaunch == 1) {
                    launchNotificationService()
                } else if (serviceToLaunch == 2) {
                    launchSecondNotificationService()
                }
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val Id = "001"
        val Id2 = "002"

        val firstRequest = OneTimeWorkRequest
            .Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    FirstWorker
                        .INPUT_DATA_ID, Id
                )
            ).build()

        val secondRequest = OneTimeWorkRequest
            .Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    SecondWorker
                        .INPUT_DATA_ID, Id
                )
            ).build()

        val thirdRequest = OneTimeWorkRequest
            .Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(
                getIdInputData(
                    ThirdWorker
                        .INPUT_DATA_ID, Id2
                )
            ).build()

        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    checkPermissionAndLaunchService()
                }
            }

        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    checkPermissionAndLaunchSecondService()
                }
            }
    }

    private fun checkPermissionAndLaunchService() {
        serviceToLaunch = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchNotificationService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            launchNotificationService()
        }
    }

    private fun checkPermissionAndLaunchSecondService() {
        serviceToLaunch = 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                launchSecondNotificationService()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            launchSecondNotificationService()
        }
    }

    private fun getIdInputData(IdKey: String, IdValue: String) =
        Data.Builder()
            .putString(IdKey, IdValue)
            .build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun launchNotificationService() {
        NotificationService.trackingCompletion.observe(
            this
        ) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }
        val serviceIntent = Intent(
            this,
            NotificationService::class.java
        ).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun launchSecondNotificationService() {
        SecondNotificationService.trackingCompletion.observe(
            this
        ) { Id ->
            showResult("Process for Notification Channel ID $Id is done!")
        }
        val serviceIntent = Intent(
            this,
            SecondNotificationService::class.java
        ).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}