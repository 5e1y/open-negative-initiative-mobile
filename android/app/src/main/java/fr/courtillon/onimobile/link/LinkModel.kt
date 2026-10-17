package fr.courtillon.onimobile.link

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uniffi.oni_link_ffi.Command
import uniffi.oni_link_ffi.DisplayListener
import uniffi.oni_link_ffi.DisplayState
import uniffi.oni_link_ffi.LinkException
import uniffi.oni_link_ffi.RemoteSession
import uniffi.oni_link_ffi.ScannerState
import uniffi.oni_link_ffi.parsePairingCode
import java.io.File

/** The link as the screens see it: one session at a time, its screen published on the main thread. */
class LinkModel(application: Application) : AndroidViewModel(application) {
    enum class Status { UNPAIRED, CONNECTING, CONNECTED, OFFLINE }

    private val vault = Vault(application)
    private val phoneName = Settings.Global.getString(application.contentResolver, Settings.Global.DEVICE_NAME)
        ?: Build.MODEL

    var desktop by mutableStateOf(vault.desktop())
        private set
    var state by mutableStateOf<ScannerState?>(null)
        private set
    var status by mutableStateOf(if (desktop == null) Status.UNPAIRED else Status.OFFLINE)
        private set
    var roundTripMs by mutableStateOf<Double?>(null)
        private set
    var problem by mutableStateOf<String?>(null)
        private set
    /** True when the connected desktop announced a receiver for photos. */
    var canImport by mutableStateOf(false)
        private set

    private var session: RemoteSession? = null
    private var dialing: Job? = null
    private var active = false
    /** The picker covering the app, or a batch on its way, holds the session open. */
    private var holding = false

    /** Pairs from a scanned or pasted code; the desktop is kept only once it has answered. */
    suspend fun pair(uri: String): Boolean {
        val code = uri.trim()
        try {
            val info = parsePairingCode(code)
            hangUp()
            status = Status.CONNECTING
            val session = RemoteSession.pair(code, withContext(Dispatchers.IO) { vault.identity() }, phoneName)
            val paired = PairedDesktop(session.hostKey(), session.hostName(), info.addresses)
            vault.save(paired)
            desktop = paired
            attach(session)
            return true
        } catch (error: LinkException.Failed) {
            problem = "The desktop hung up: the code may have expired or been used. Show a fresh one."
        } catch (error: LinkException) {
            problem = describe(error)
        }
        status = if (desktop == null) Status.UNPAIRED else Status.OFFLINE
        return false
    }

    /** Dials the paired desktop until it answers, for as long as the app is in front. */
    fun resume() {
        active = true
        val desktop = desktop ?: return
        if (session != null || dialing != null) return
        status = Status.CONNECTING
        dialing = viewModelScope.launch { dial(desktop) }
    }

    /** Hangs up when the app leaves the screen, unless photos are being picked or sent. */
    fun suspend() {
        active = false
        if (holding) return
        hangUp()
        if (desktop != null) status = Status.OFFLINE
    }

    /** Drops the desktop and its key from this phone; the desktop keeps trusting it until revoked there. */
    fun forget() {
        hangUp()
        vault.forgetDesktop()
        desktop = null
        state = null
        roundTripMs = null
        problem = null
        status = Status.UNPAIRED
    }

    fun press(command: Command) {
        val session = session ?: return
        if (status != Status.CONNECTED) return
        state = session.press(command)
    }

    /** Keeps the session through the system's photo picker, which takes the screen from the app. */
    fun holdForImport() {
        holding = true
    }

    /** Ends a hold with nothing sent, as when the picker is dismissed. */
    fun releaseImport() {
        holding = false
        if (!active) suspend()
    }

    /** Sends files byte for byte to the desktop, which files them without asking, then ends the hold. */
    suspend fun send(files: List<File>): ImportOutcome {
        holding = true
        try {
            // A session dropped while the picker was up is dialled again before giving up.
            if (session == null && active) resume()
            withTimeoutOrNull(RECONNECT_WAIT_MS) { snapshotFlow { status }.first { it == Status.CONNECTED } }
            val session = session
            if (session == null || !canImport) return ImportOutcome(0, 0, "The desktop is not connected.")
            val result = session.sendFiles(files.map { it.path })
            return ImportOutcome(result.sent.size, result.skipped.size, null)
        } catch (error: LinkException) {
            return ImportOutcome(0, 0, describe(error))
        } finally {
            releaseImport()
        }
    }

    override fun onCleared() {
        hangUp()
    }

    // Session

    private suspend fun dial(desktop: PairedDesktop) {
        var pause = 1L
        while (currentCoroutineContextActive()) {
            try {
                val identity = withContext(Dispatchers.IO) { vault.identity() }
                val session = RemoteSession.connect(desktop.addresses, desktop.key, identity, phoneName)
                if (!currentCoroutineContextActive()) {
                    session.hangUp()
                    session.destroy()
                    return
                }
                dialing = null
                attach(session)
                return
            } catch (error: CancellationException) {
                throw error
            } catch (error: LinkException) {
                problem = describe(error)
                status = Status.OFFLINE
                // A desktop that revoked this phone refuses it every time: dialing again only drains the battery.
                if (error is LinkException.Refused) {
                    dialing = null
                    return
                }
            }
            delay(pause * 1000)
            pause = minOf(pause * 2, 8)
            status = Status.CONNECTING
        }
    }

    private suspend fun currentCoroutineContextActive() = kotlin.coroutines.coroutineContext.isActive

    private fun attach(session: RemoteSession) {
        this.session = session
        problem = null
        canImport = session.canSendFiles()
        apply(session.current(), session)
        session.setListener(object : DisplayListener {
            override fun onDisplay(display: DisplayState) {
                viewModelScope.launch { apply(display, session) }
            }
        })
    }

    /** A screen from a session already replaced is dropped; a lost connection dials again. */
    private fun apply(display: DisplayState, source: RemoteSession) {
        if (source !== session) return
        state = display.state
        roundTripMs = display.roundTripMs
        if (!display.connected) {
            hangUp()
            status = Status.OFFLINE
            if (active) resume()
            return
        }
        status = Status.CONNECTED
    }

    private fun hangUp() {
        dialing?.cancel()
        dialing = null
        session?.let {
            it.hangUp()
            it.destroy()
        }
        session = null
        canImport = false
    }

    companion object {
        private const val RECONNECT_WAIT_MS = 10_000L

        fun describe(error: LinkException): String = when (error) {
            is LinkException.BadCode -> "This is not an ONI pairing code."
            is LinkException.Version -> "The desktop runs another version of ONI Link: update both apps."
            is LinkException.Refused -> "The desktop no longer knows this phone. Forget it here, then pair again."
            is LinkException.Unreachable -> "The desktop does not answer. Check that both are on the same network."
            is LinkException.ImportUnavailable -> "The desktop does not take photos."
            is LinkException.ImportRefused -> "The desktop refused the photos. Pair again from the desktop's code."
            is LinkException.Failed -> error.reason
        }
    }
}

/** One batch sent to the desktop, as the Import screen reports it. */
data class ImportOutcome(val sent: Int, val skipped: Int, val problem: String?)
