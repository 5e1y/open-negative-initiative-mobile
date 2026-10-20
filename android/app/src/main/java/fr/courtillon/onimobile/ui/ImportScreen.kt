package fr.courtillon.onimobile.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.courtillon.onimobile.ds.Button
import fr.courtillon.onimobile.ds.DS
import fr.courtillon.onimobile.ds.KeyPlate
import fr.courtillon.onimobile.ds.Label
import fr.courtillon.onimobile.ds.Note
import fr.courtillon.onimobile.ds.Progress
import fr.courtillon.onimobile.ds.Section
import fr.courtillon.onimobile.ds.TextRole
import fr.courtillon.onimobile.ds.Tone
import fr.courtillon.onimobile.ds.WarningBand
import fr.courtillon.onimobile.link.ImportOutcome
import fr.courtillon.onimobile.link.LinkModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private data class Batch(val count: Int, val outcome: ImportOutcome)

/** Sends photos from the phone to the desktop, as their original files, one batch per pick. */
@Composable
fun ImportScreen(link: LinkModel, done: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf<String?>(null) }
    // The share of the picked photos read so far; unknown while they are sent.
    var read by remember { mutableStateOf<Float?>(null) }
    val batches = remember { mutableStateListOf<Batch>() }
    BackHandler(onBack = done)

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { picked ->
        if (picked.isEmpty()) return@rememberLauncherForActivityResult link.releaseImport()
        scope.launch {
            val total = picked.size
            val folder = File(context.cacheDir, "import-${UUID.randomUUID()}")
            val outcome = try {
                working = "Reading 0 of $total"
                read = 0f
                val files = originals(context, picked, folder) { n ->
                    working = "Reading $n of $total"
                    read = n.toFloat() / total
                }
                working = "Sending $total"
                read = null
                link.send(files)
            } catch (error: Exception) {
                link.releaseImport()
                ImportOutcome(0, 0, "The photos could not be read: ${error.message}")
            }
            withContext(Dispatchers.IO) { folder.deleteRecursively() }
            batches.add(0, Batch(total, outcome))
            working = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Section("", separated = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("Import", Modifier.weight(1f), role = TextRole.TITLE)
                Button("DONE", clicked = done)
            }
            Label(
                "Pick photos to send to ${link.desktop?.name ?: "the desktop"}. Each goes as its original file: a RAW scan arrives as its DNG.",
                tone = Tone.SECONDARY,
                wrap = true,
            )
            KeyPlate(Modifier.fillMaxWidth()) {
                Button("CHOOSE PHOTOS", Modifier.weight(1f), enabled = working == null && link.canImport) {
                    link.holdForImport()
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            working?.let {
                Progress(read)
                Label(it, tone = Tone.SECONDARY)
            }
            if (!link.canImport) {
                WarningBand(
                    if (link.status == LinkModel.Status.CONNECTED) "This desktop does not take photos." else "Connect to the desktop first.",
                )
            }
        }
        if (batches.isNotEmpty()) {
            Section("Sent") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(DS.Space.xs)) {
                    items(batches) { batch ->
                        Column(verticalArrangement = Arrangement.spacedBy(DS.Space.xxs)) {
                            Label(summary(batch))
                            batch.outcome.problem?.let { Note(it) }
                        }
                    }
                }
            }
        }
    }
}

private fun summary(batch: Batch): String {
    val sent = "${batch.outcome.sent} of ${batch.count} sent"
    return if (batch.outcome.skipped > 0) "$sent, ${batch.outcome.skipped} declined" else sent
}

/** Copies each picked photo's bytes untouched into `folder`, under its own file name, for the sender to read. */
private suspend fun originals(context: Context, picked: List<Uri>, folder: File, progress: (Int) -> Unit): List<File> {
    val files = mutableListOf<File>()
    for (uri in picked) {
        files += withContext(Dispatchers.IO) {
            folder.mkdirs()
            val target = unique(folder, displayName(context, uri) ?: "photo-${files.size + 1}")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "no data for $uri" }
                target.outputStream().use { input.copyTo(it) }
            }
            target
        }
        progress(files.size)
    }
    return files
}

private fun displayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

/** Two picks can share a name, from two folders of the phone: the second gets a number, never the first's place. */
private fun unique(folder: File, name: String): File {
    val clean = name.replace('/', '_')
    var candidate = File(folder, clean)
    var n = 2
    while (candidate.exists()) {
        candidate = File(folder, "${clean.substringBeforeLast('.')}-$n.${clean.substringAfterLast('.', "")}".trimEnd('.'))
        n += 1
    }
    return candidate
}
