package dev.appblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BlockRepository.init(this)
        setContent {
            AppBlockTheme {
                Surface(Modifier.fillMaxSize()) {
                    Root()
                }
            }
        }
    }
}

private data class TabSpec(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun Root() {
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Schedule?>(null) }

    val currentlyEditing = editing
    if (currentlyEditing != null) {
        ScheduleEditorScreen(initial = currentlyEditing, onClose = { editing = null })
        return
    }

    val tabs = listOf(
        TabSpec("Blocking", Icons.Default.Shield),
        TabSpec("Strict Mode", Icons.Default.Lock),
        TabSpec("Insights", Icons.Default.BarChart),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, spec ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(spec.icon, contentDescription = spec.label) },
                        label = { Text(spec.label) }
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> BlockingScreen(onEditSchedule = { editing = it })
                1 -> StrictModeScreen()
                2 -> InsightsScreen()
            }
        }
    }
}
