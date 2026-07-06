package elieoko.app.mcoresystem

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import elieoko.app.mcoresystem.domain.route.Navigation
import elieoko.app.mcoresystem.domain.viewmodel.config.*
import elieoko.app.mcoresystem.domain.viewmodel.room.*
import elieoko.app.mcoresystem.presentation.ui.theme.*
import kotlin.getValue

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private lateinit var navHostController: NavHostController
    private val userViewModel: UserViewModel by viewModels { UserViewModelFactory((application as MCoreApplication).userRepository) }
    private val currencyViewModel: CurrencyViewModel by viewModels { CurrencyViewModelFactory((application as MCoreApplication).currencyRepository) }
    private val paymentMethodViewModel: PaymentMethodViewModel by viewModels { PaymentMethodViewModelFactory((application as MCoreApplication).paymentMethodRepository) }
    private val operationViewModel: OperationViewModel by viewModels { OperationViewModelFactory((application as MCoreApplication).operationRepository) }
    private val organismViewModel: OrganismViewModel by viewModels { OrganismViewModelFactory((application as MCoreApplication).organismRepository) }
    private val categoryViewModel: CategoryViewModel by viewModels { CategoryViewModelFactory((application as MCoreApplication).categoryRepository) }
    private val typeCategoryViewModel: TypeCategoryViewModel by viewModels { TypeCategoryViewModelFactory((application as MCoreApplication).typeCategoryRepository) }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ViewModelConstructorInComposable", "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureNotificationPermission()
        setContent {
            navHostController = rememberNavController()
            MCoreSystemTheme {
                val applicationViewModel = viewModel<ApplicationViewModel>{
                    ApplicationViewModel(
                        roomVm = InstanceRoomViewModel(
                            currencyViewModel = currencyViewModel,
                            userViewModel = userViewModel,
                            paymentMethodViewModel = paymentMethodViewModel,
                            organismViewModel = organismViewModel,
                            operationViewModel = operationViewModel,
                            categoryViewModel = categoryViewModel,
                            typeCategoryViewModel = typeCategoryViewModel
                        ),
                        exchangeRateRepository = (application as MCoreApplication).exchangeRateRepository
                    )
                }
                applicationViewModel.room.currency = currencyViewModel
                applicationViewModel.room.user = userViewModel
                applicationViewModel.room.paymentMethod = paymentMethodViewModel
                applicationViewModel.room.operation = operationViewModel
                applicationViewModel.room.category = categoryViewModel
                applicationViewModel.room.typeCategory = typeCategoryViewModel
                applicationViewModel.room.organism = organismViewModel
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Navigation(navHostController, applicationViewModel)
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