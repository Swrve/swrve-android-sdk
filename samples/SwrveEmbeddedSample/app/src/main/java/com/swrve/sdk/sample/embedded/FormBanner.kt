package com.swrve.sdk.sample.embedded

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.swrve.sdk.SwrveSDK
import com.swrve.sdk.messaging.SwrveEmbeddedCampaign
import com.swrve.sdk.messaging.SwrveEmbeddedMessage
import kotlinx.coroutines.delay
import org.json.JSONObject

private const val FORM_TAG = "FormBanner"

// ---------------------------------------------------------------------------
// Form schema (parsed with org.json so the shared EmbeddedData model is untouched)
// ---------------------------------------------------------------------------

private data class FormText(val text: String, val color: String?)
private data class FormOption(val value: String, val label: String)

private data class FormField(
    val id: String,
    val type: String,          // text | email | number | single_choice | multi_choice | checkbox
    val label: String,
    val required: Boolean,
    val maxLength: Int?,
    val options: List<FormOption>
)

private data class FormButton(val label: String, val color: String?, val backgroundColor: String?)

private data class FormSpec(
    val formId: String,
    val backgroundColor: String?,
    val textColor: String?,
    val accentColor: String?,
    val title: FormText?,
    val intro: FormText?,
    val fields: List<FormField>,
    val submit: FormButton,
    val dismiss: FormButton,
    val eventName: String,
    val propertyKey: String?,
    val propertyValue: String,
    val successMessage: String
)

private fun JSONObject.textOrNull(key: String): FormText? {
    val o = optJSONObject(key) ?: return null
    val t = o.optString("text", "")
    return if (t.isBlank()) null else FormText(t, o.optString("color").ifBlank { null })
}

private fun parseForm(json: String): FormSpec? = try {
    val o = JSONObject(json)
    if (o.optString("type") != "form") {
        null
    } else {
        val fieldsJson = o.optJSONArray("fields")
        val fields = (0 until (fieldsJson?.length() ?: 0)).mapNotNull { i ->
            val f = fieldsJson!!.optJSONObject(i) ?: return@mapNotNull null
            val opts = f.optJSONArray("options")
            FormField(
                id = f.optString("id"),
                type = f.optString("type", "text"),
                label = f.optString("label"),
                required = f.optBoolean("required", false),
                maxLength = if (f.has("max_length")) f.optInt("max_length") else null,
                options = (0 until (opts?.length() ?: 0)).mapNotNull { j ->
                    val op = opts!!.optJSONObject(j) ?: return@mapNotNull null
                    FormOption(op.optString("value"), op.optString("label", op.optString("value")))
                }
            )
        }.filter { it.id.isNotBlank() }

        val buttons = o.optJSONObject("buttons")
        fun button(name: String, fallback: String): FormButton {
            val b = buttons?.optJSONObject(name)
            return FormButton(
                label = b?.optString("label")?.ifBlank { null } ?: fallback,
                color = b?.optString("color")?.ifBlank { null },
                backgroundColor = b?.optString("background_color")?.ifBlank { null }
            )
        }

        val onSubmit = o.optJSONObject("on_submit")
        val prop = onSubmit?.optJSONObject("user_property")

        FormSpec(
            formId = o.optString("form_id", "form"),
            backgroundColor = o.optString("background_color").ifBlank { null },
            textColor = o.optString("text_color").ifBlank { null },
            accentColor = o.optString("accent_color").ifBlank { null },
            title = o.textOrNull("title"),
            intro = o.textOrNull("intro"),
            fields = fields,
            submit = button("submit", "Submit"),
            dismiss = button("dismiss", "Not now"),
            eventName = onSubmit?.optString("event")?.ifBlank { null } ?: "form.submitted",
            propertyKey = prop?.optString("key")?.ifBlank { null },
            propertyValue = prop?.optString("value", "true") ?: "true",
            successMessage = onSubmit?.optString("success_message")?.ifBlank { null } ?: "Thanks!"
        )
    }
} catch (e: Exception) {
    Log.w(FORM_TAG, "Could not parse form JSON", e)
    null
}

/**
 * Finds the first Message Center embedded campaign whose payload has "type": "form".
 * If this call doesn't compile against your SDK version, copy the lines that
 * EmbeddedViewModel.loadFloatingBanner() uses to fetch campaigns instead.
 */
private fun loadForm(): Pair<SwrveEmbeddedMessage, FormSpec>? {
    val campaigns = SwrveSDK.getEmbeddedMessageCenterCampaigns()
    Log.d(FORM_TAG, "Message Center returned ${campaigns?.size ?: "null"} item(s)")
    if (campaigns == null) return null
    for (item in campaigns) {
        // Depending on SDK version the list holds messages or campaigns; handle both.
        val message: SwrveEmbeddedMessage = when (val any = item as Any) {
            is SwrveEmbeddedMessage -> any
            is SwrveEmbeddedCampaign -> any.message ?: run {
                Log.d(FORM_TAG, "Campaign ${any.id} has no message"); null
            } ?: continue
            else -> {
                Log.d(FORM_TAG, "Skipping unexpected item type: ${any::class.java.name}")
                continue
            }
        }
        val raw = message.data
        Log.d(FORM_TAG, "Message ${message.id} data: ${raw?.take(200)}")
        val spec = raw?.let { parseForm(it) }
        if (spec == null) {
            Log.d(FORM_TAG, "Message ${message.id} is not a valid \"type\": \"form\" payload")
            continue
        }
        Log.d(FORM_TAG, "Showing form ${spec.formId} from message ${message.id}")
        return message to spec
    }
    Log.d(FORM_TAG, "No form campaign found")
    return null
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

@Composable
fun FormBanner(modifier: Modifier = Modifier) {
    var entry by remember { mutableStateOf<Pair<SwrveEmbeddedMessage, FormSpec>?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    val answers = remember { mutableStateMapOf<String, String>() }
    val multi = remember { mutableStateMapOf<String, Set<String>>() }

    LaunchedEffect(Unit) {
        delay(1000L)
        entry = loadForm()
        isVisible = entry != null
    }

    // Impression, once per message
    LaunchedEffect(entry?.first?.id) {
        entry?.let { SwrveSDK.embeddedMessageWasShownToUser(it.first) }
    }

    LaunchedEffect(submitted) {
        if (submitted) {
            delay(2000L)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && entry != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        val (message, spec) = entry!!

        fun valueOf(f: FormField): String =
            if (f.type == "multi_choice") (multi[f.id] ?: emptySet()).joinToString("|")
            else answers[f.id].orEmpty()

        fun isInvalid(f: FormField): Boolean {
            val v = valueOf(f)
            return when {
                f.required && f.type == "checkbox" -> v != "true"
                f.required && v.isBlank() -> true
                f.type == "email" && v.isNotBlank() && !v.contains("@") -> true
                else -> false
            }
        }

        fun submit() {
            if (spec.fields.any { isInvalid(it) }) {
                showErrors = true
                return
            }
            // 1. campaign click metric
            SwrveSDK.embeddedMessageButtonWasPressed(message, "submit")
            // 2. the answers, as a custom event (string values; multi-select as a|b|c)
            val payload = mutableMapOf("form_id" to spec.formId)
            spec.fields.forEach { f ->
                val v = valueOf(f)
                if (v.isNotBlank()) payload[f.id] = v
                else if (f.type == "checkbox") payload[f.id] = "false"
            }
            SwrveSDK.event(spec.eventName, payload)
            // 3. state, as a user property
            spec.propertyKey?.let { SwrveSDK.userUpdate(mapOf(it to spec.propertyValue)) }
            // 4. send now rather than waiting for the next batch
            SwrveSDK.sendQueuedEvents()
            submitted = true
        }

        // Text defaults to dark when the form sets its own (light) background, so it
        // stays readable when the phone is in dark mode. Override with "text_color".
        val textColor = hexToColor(spec.textColor)
            ?: if (spec.backgroundColor != null) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface
        val accentColor = hexToColor(spec.accentColor)
            ?: hexToColor(spec.submit.backgroundColor) ?: MaterialTheme.colorScheme.primary

        // Keyboard handling: the form sits above the app's bottom navigation bar, which
        // is already part of the keyboard's height. Only lift the form by the part of the
        // keyboard that actually overlaps it, so there's no gap above the keyboard.
        val density = LocalDensity.current
        val rootView = LocalView.current.rootView
        val keyboardPx = WindowInsets.ime.getBottom(density)
        var spaceBelowPx by remember { mutableIntStateOf(0) }
        val keyboardPadding = with(density) { (keyboardPx - spaceBelowPx).coerceAtLeast(0).toDp() }

        Box(
            modifier = modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    // distance from the bottom of this overlay to the bottom of the screen
                    spaceBelowPx = (rootView.height - coords.boundsInWindow().bottom).toInt().coerceAtLeast(0)
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .padding(bottom = keyboardPadding)
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(hexToColor(spec.backgroundColor) ?: MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                spec.title?.let {
                    Text(
                        text = it.text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = hexToColor(it.color) ?: textColor
                    )
                }

                if (submitted) {
                    Text(
                        text = spec.successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = hexToColor(spec.intro?.color) ?: textColor
                    )
                    return@Column
                }

                spec.intro?.let {
                    Text(
                        text = it.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = hexToColor(it.color) ?: textColor
                    )
                }

                spec.fields.forEach { f ->
                    FormFieldView(
                        field = f,
                        value = answers[f.id].orEmpty(),
                        selected = multi[f.id] ?: emptySet(),
                        error = showErrors && isInvalid(f),
                        textColor = textColor,
                        accentColor = accentColor,
                        onValue = { answers[f.id] = it },
                        onToggle = { opt ->
                            val cur = multi[f.id] ?: emptySet()
                            multi[f.id] = if (opt in cur) cur - opt else cur + opt
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        SwrveSDK.embeddedMessageButtonWasPressed(message, "dismiss")
                        isVisible = false
                    }) {
                        Text(spec.dismiss.label, color = hexToColor(spec.dismiss.color) ?: textColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { submit() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = hexToColor(spec.submit.backgroundColor) ?: MaterialTheme.colorScheme.primary,
                            contentColor = hexToColor(spec.submit.color) ?: MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(spec.submit.label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun FormFieldView(
    field: FormField,
    value: String,
    selected: Set<String>,
    error: Boolean,
    textColor: Color,
    accentColor: Color,
    onValue: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    val label = if (field.required) "${field.label} *" else field.label
    val errorColor = MaterialTheme.colorScheme.error

    when (field.type) {
        "single_choice" -> Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (error) errorColor else textColor)
            field.options.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onValue(opt.value) }
                ) {
                    RadioButton(selected = value == opt.value, onClick = { onValue(opt.value) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = textColor.copy(alpha = 0.6f)))
                    Text(opt.label, color = textColor)
                }
            }
        }

        "multi_choice" -> Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (error) errorColor else textColor)
            field.options.forEach { opt ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(opt.value) }
                ) {
                    Checkbox(checked = opt.value in selected, onCheckedChange = { onToggle(opt.value) }, colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = textColor.copy(alpha = 0.6f)))
                    Text(opt.label, color = textColor)
                }
            }
        }

        "checkbox" -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onValue(if (value == "true") "false" else "true") }
        ) {
            Checkbox(checked = value == "true", onCheckedChange = { onValue(if (it) "true" else "false") }, colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = textColor.copy(alpha = 0.6f)))
            Text(label, color = if (error) errorColor else textColor)
        }

        else -> OutlinedTextField(
            value = value,
            onValueChange = { new ->
                val limited = field.maxLength?.let { new.take(it) } ?: new
                onValue(if (field.type == "number") limited.filter { it.isDigit() } else limited)
            },
            label = { Text(label) },
            isError = error,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedLabelColor = accentColor,
                unfocusedLabelColor = textColor.copy(alpha = 0.7f),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = textColor.copy(alpha = 0.4f),
                cursorColor = accentColor
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = when (field.type) {
                    "email" -> KeyboardType.Email
                    "number" -> KeyboardType.Number
                    else -> KeyboardType.Text
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
