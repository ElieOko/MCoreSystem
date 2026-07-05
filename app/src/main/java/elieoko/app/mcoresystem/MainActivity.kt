package elieoko.app.mcoresystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import elieoko.app.mcoresystem.domain.viewmodel.config.*
import elieoko.app.mcoresystem.domain.viewmodel.room.*
import elieoko.app.mcoresystem.presentation.ui.theme.*
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels { UserViewModelFactory((application as MCoreApplication).userRepository) }
    private val currencyViewModel: CurrencyViewModel by viewModels { CurrencyViewModelFactory((application as MCoreApplication).currencyRepository) }
    private val paymentMethodViewModel: PaymentMethodViewModel by viewModels { PaymentMethodViewModelFactory((application as MCoreApplication).paymentMethodRepository) }
    private val operationViewModel: OperationViewModel by viewModels { OperationViewModelFactory((application as MCoreApplication).operationRepository) }
    private val organismViewModel: OrganismViewModel by viewModels { OrganismViewModelFactory((application as MCoreApplication).organismRepository) }
    private val categoryViewModel: CategoryViewModel by viewModels { CategoryViewModelFactory((application as MCoreApplication).categoryRepository) }
    private val typeCategoryViewModel: TypeCategoryViewModel by viewModels { TypeCategoryViewModelFactory((application as MCoreApplication).typeCategoryRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val applicationViewModel = viewModel<ApplicationViewModel>()
            applicationViewModel.room.currency = currencyViewModel
            applicationViewModel.room.user = userViewModel
            applicationViewModel.room.paymentMethod = paymentMethodViewModel
            applicationViewModel.room.operation = operationViewModel
            applicationViewModel.room.category = categoryViewModel
            applicationViewModel.room.typeCategory = typeCategoryViewModel
            applicationViewModel.room.organism = organismViewModel
            MCoreSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MCoreSystemTheme {
        Greeting("Android")
    }
}