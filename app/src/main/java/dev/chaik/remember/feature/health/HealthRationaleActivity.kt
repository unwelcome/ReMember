package dev.chaik.remember.feature.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chaik.remember.R
import dev.chaik.remember.core.ui.theme.ReMemberTheme

/**
 * «Зачем приложению данные о здоровье». Health Connect открывает эту страницу из своих
 * настроек и не показывает запрос разрешений, если её нет.
 */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReMemberTheme {
                Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .safeDrawingPadding()
                            .padding(24.dp),
                    ) {
                        Text(stringResource(R.string.health_rationale_title), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.health_rationale_text), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = ::finish) { Text(stringResource(R.string.action_ok)) }
                    }
                }
            }
        }
    }
}
