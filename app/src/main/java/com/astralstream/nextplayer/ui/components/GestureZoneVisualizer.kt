package com.astralstream.nextplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astralstream.nextplayer.viewmodels.GestureCustomizationViewModel.GestureZone

@Composable
fun GestureZoneVisualizer(
    selectedZone: GestureZone?,
    onZoneSelected: (GestureZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .border(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        // Create a 3x3 grid
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                ZoneBox(
                    zone = GestureZone.TOP_LEFT,
                    isSelected = selectedZone == GestureZone.TOP_LEFT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.TOP_CENTER,
                    isSelected = selectedZone == GestureZone.TOP_CENTER,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.TOP_RIGHT,
                    isSelected = selectedZone == GestureZone.TOP_RIGHT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                ZoneBox(
                    zone = GestureZone.MIDDLE_LEFT,
                    isSelected = selectedZone == GestureZone.MIDDLE_LEFT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.MIDDLE_CENTER,
                    isSelected = selectedZone == GestureZone.MIDDLE_CENTER,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.MIDDLE_RIGHT,
                    isSelected = selectedZone == GestureZone.MIDDLE_RIGHT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                ZoneBox(
                    zone = GestureZone.BOTTOM_LEFT,
                    isSelected = selectedZone == GestureZone.BOTTOM_LEFT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.BOTTOM_CENTER,
                    isSelected = selectedZone == GestureZone.BOTTOM_CENTER,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
                ZoneBox(
                    zone = GestureZone.BOTTOM_RIGHT,
                    isSelected = selectedZone == GestureZone.BOTTOM_RIGHT,
                    onClick = onZoneSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ZoneBox(
    zone: GestureZone,
    isSelected: Boolean,
    onClick: (GestureZone) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { onClick(zone) }
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
            )
            .background(
                if (isSelected) 
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                else 
                    Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = zone.displayName,
            fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(if (isSelected) 1f else 0.7f)
        )
    }
}