package dev.appblock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

private val QUOTES = listOf(
    "Your future self says thanks.",
    "That scroll can wait.",
    "Back to what matters.",
    "Discipline is choosing what you want most over what you want now.",
    "One less distraction.",
    "You put this wall here for a reason.",
)

class BlockedActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_REASON = "reason"
        const val EXTRA_IS_APP = "is_app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BlockRepository.init(this)

        val name = intent.getStringExtra(EXTRA_NAME) ?: "This"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        val isApp = intent.getBooleanExtra(EXTRA_IS_APP, true)

        // Back must not return to the blocked app/site.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        setContent {
            AppBlockTheme {
                BlockedScreen(name = name, reason = reason, isApp = isApp, onDismiss = ::goHome)
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }
}

@Composable
private fun BlockedScreen(name: String, reason: String, isApp: Boolean, onDismiss: () -> Unit) {
    val quote = QUOTES[(BlockRepository.blockedTotal % QUOTES.size + QUOTES.size) % QUOTES.size]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            if (isApp) "is blocked right now" else "This website is blocked right now",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (reason.isNotEmpty()) {
            Text(
                "Blocked by: $reason",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "“$quote”",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        Button(onClick = onDismiss) {
            Text("OK, take me home")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "${BlockRepository.blockedToday} distractions blocked today",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
