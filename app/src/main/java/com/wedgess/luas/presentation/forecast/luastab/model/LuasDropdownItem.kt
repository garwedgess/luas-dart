package com.wedgess.luas.presentation.forecast.luastab.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wedgess.luas.presentation.model.DropdownItem
import com.wedgess.luas.presentation.model.DropdownTrailingContent
import com.wedgess.luas.ui.theme.LuasTheme

@Stable
data class LuasDropdownItem(
    override val text: String,
    val isParkAndRide: Boolean,
    val isCycleAndRide: Boolean
) : DropdownItem, DropdownTrailingContent {

    @Composable
    override fun RenderTrailingContent() {
        if (isParkAndRide || isCycleAndRide) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isParkAndRide) {
                    AndRideIcon(Icons.Default.LocalParking)
                }
                if (isCycleAndRide) {
                    AndRideIcon(Icons.Default.PedalBike)
                }
            }
        }
    }

    companion object {
        val default = LuasDropdownItem(
            text = "",
            isParkAndRide = false,
            isCycleAndRide = false
        )
    }
}

@Composable
private fun AndRideIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = Color(0xFF4A5DD6),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.padding(4.dp),
            imageVector = icon,
            contentDescription = icon.name,
            tint = Color.White
        )
    }
}

@Preview
@Composable
private fun AndRideIconPreview() {
    LuasTheme {
        Surface {
            AndRideIcon(Icons.Default.PedalBike)
        }
    }
}
