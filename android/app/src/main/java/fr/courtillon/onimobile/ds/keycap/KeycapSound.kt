package fr.courtillon.onimobile.ds.keycap

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import fr.courtillon.onimobile.R
import kotlin.math.pow
import kotlin.random.Random

/**
 * The click, its pitch wandering a few tens of cents per press so repeats read as a struck object, not a
 * sample. A sonification stream, so the phone's silent mode mutes it.
 */
object KeycapSound {
    /** Pitch wander in cents either side: two clicks at one pitch read as one sample played twice. */
    private const val JITTER_CENTS = 120.0
    private const val VOLUME = 0.5f

    private var pool: SoundPool? = null
    private var down = 0
    private var up = 0
    /** Drawn at the press and reused at the release, since two pitches would sound like two keys. */
    private var rate = 1f

    /** Loads both clicks ahead of the first press, which would otherwise play nothing. */
    fun load(context: Context) {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val made = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
        down = made.load(context.applicationContext, R.raw.click_down, 1)
        up = made.load(context.applicationContext, R.raw.click_up, 1)
        pool = made
    }

    fun press(context: Context) {
        load(context)
        // A ratio, because pitch is geometric: an additive jitter would be audible at one pitch only.
        val cents = Random.nextDouble(-JITTER_CENTS, JITTER_CENTS)
        rate = 2.0.pow(cents / 1200).toFloat()
        pool?.play(down, VOLUME, VOLUME, 1, 0, rate)
    }

    fun release() {
        pool?.play(up, VOLUME, VOLUME, 1, 0, rate)
    }
}
