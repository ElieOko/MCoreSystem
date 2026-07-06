package elieoko.app.mcoresystem.presentation.components.element

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Space(x:Int=0,y:Int=0){
    Spacer(Modifier.width(x.dp).height(y.dp))
}