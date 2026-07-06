package elieoko.app.mcoresystem.presentation.components.element

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun Label(value:String, modifier: Modifier = Modifier, size:Int = 14, color: Color= Color.White, fontWeight: FontWeight =FontWeight.Normal){
    Text(value, modifier = modifier, fontSize = size.sp, color = color, fontWeight = fontWeight)
}

