
package br.com.fiap.letsflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.fiap.letsflow.ui.navegacao.LetsFlowNavHost
import br.com.fiap.letsflow.ui.theme.LetsFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            LetsFlowTheme {
                LetsFlowNavHost()
            }
        }
    }
}
