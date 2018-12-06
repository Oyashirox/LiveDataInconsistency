package com.example.livedatainconsistency

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Observer
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.FlowableOnSubscribe
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    companion object {
        init {
            Timber.plant(Timber.DebugTree())
        }
    }

    private val permissionWrapper = Wrapper()
    /** Flowable that returns true or false depending on permission granted or not */
    private var subscriptionCounter = 0
    private val flowable = Flowable.create<Boolean>(permissionWrapper, BackpressureStrategy.LATEST)
        .doOnSubscribe {
            subscriptionCounter += 1
            Timber.d("Subscribed $subscriptionCounter times")
        }

    private val liveData = LiveDataReactiveStreams.fromPublisher(flowable)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        liveData.observe(this, Observer {
            Timber.d("Permission granted: $it")
            text.text = "Permission granted: $it, Subscribed $subscriptionCounter times"
        })
    }

    /** Wrapper that converts permission request to RxJava Flowable*/
    private inner class Wrapper : FlowableOnSubscribe<Boolean> {
        internal var emitter: FlowableEmitter<Boolean>? = null
        override fun subscribe(emitter: FlowableEmitter<Boolean>) {
            this.emitter = emitter
            emitter.setCancellable { this.emitter = null }

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                emitter.onNext(true)
            } else {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 123)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            123 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    permissionWrapper.emitter?.onNext(true)
                } else {
                    permissionWrapper.emitter?.onNext(false)
                }
            }
        }
    }
}
