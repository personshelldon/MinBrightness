package com.don11995.minbrightness

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    @Inject
    lateinit var brightnessHelper: BrightnessHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BrightnessService.startService(this)

        brightness_seek_bar.setOnSeekBarChangeListener(this)
    }

    override fun onStart() {
        super.onStart()
        if (!brightnessHelper.canWriteSettings()) {
            brightnessHelper.goToSettingsPermSetup()
        } else {
            invalidateSeekBar()
            invalidateInfoText()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        info_text.text = progress.toString()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // nothing
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        brightnessHelper.minBrightness = brightness_seek_bar.progress
        invalidateBrightness()
    }

    private fun invalidateSeekBar() {
        val minBrightness = brightnessHelper.minBrightness
        brightness_seek_bar.progress = minBrightness
    }

    private fun invalidateInfoText() {
        val minBrightness = brightnessHelper.minBrightness
        info_text.text = minBrightness.toString()
    }

    private fun invalidateBrightness() {
        val minBrightness = brightnessHelper.minBrightness
        brightnessHelper.setBrightness(minBrightness)
    }
}
