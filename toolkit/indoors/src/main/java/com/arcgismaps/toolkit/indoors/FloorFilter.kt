package com.arcgismaps.toolkit.indoors

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arcgismaps.LoadStatus
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.portal.Portal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
// TBD: Floor filter properties need to be defined
public fun FloorFilter(
    modifier: Modifier = Modifier,
    selectedFloorIndex: Int = 0,
    textSize: TextUnit = 15.sp,
    buttonSize: Dp = 60.dp,
    textColor: Color = Color.Black,
    selectedTextColor: Color = Color(0xFF005E95),
    buttonBackgroundColor: Color = Color.White,
    selectedButtonBackgroundColor: Color = Color(0xFFE2F1FB),
    typography: Typography = MaterialTheme.typography,
    closeButtonVisibility: Int = View.VISIBLE,
    siteFacilityButtonVisibility: Int = View.VISIBLE,
    siteSearchVisibility: Int = View.VISIBLE,
    closeButtonPosition: Int = 0,
    maxDisplayLevels: Int = 0,
    arcGISMap: ArcGISMap,
) {
    // check if the map is loaded
    if (arcGISMap.loadStatus.collectAsState().value != LoadStatus.Loaded) {
        return
    }

    // get the floor manager
    val floorManager = arcGISMap.floorManager ?: throw Throwable("Map does not contain floors")

    // load the floor manager
    LaunchedEffect(Unit) {
        floorManager.load().getOrElse {
            throw Throwable("Error loading the floor manager")
        }
    }

    // check if floor manager is loaded
    if(floorManager.loadStatus.collectAsState().value != LoadStatus.Loaded){
        return
    }

    Surface(
        shadowElevation = 10.dp,
        color = Color.Transparent
    ) {
        // column with rounded corners
        Column(
            modifier = modifier
                .width(buttonSize)
                .background(color = buttonBackgroundColor, shape = RoundedCornerShape(5.dp))
        ) {
            // collapse list box
            if (closeButtonVisibility == View.VISIBLE){
                Box(modifier.fillMaxWidth()) {
                    Icon(
                        modifier = modifier.padding(5.dp).align(Alignment.Center),
                        painter = painterResource(id = R.drawable.ic_x_24),
                        contentDescription = "Close icon"
                    )
                }
            }

            // set the floor index
            var selectedIndex by remember { mutableStateOf(selectedFloorIndex) }
            val onItemClick = { index: Int -> selectedIndex = index }
            LazyColumn(modifier.fillMaxWidth(),) {
                items(floorManager.levels.size) { index ->
                    FloorItemView(
                        index = index,
                        selected = selectedIndex == index,
                        onClick = onItemClick,
                        floorText = floorManager.levels[index].longName,
                        textSize = textSize,
                        textColor = textColor,
                        selectedTextColor = selectedTextColor,
                        buttonBackgroundColor = buttonBackgroundColor,
                        selectedButtonBackgroundColor = selectedButtonBackgroundColor,
                        typography = typography
                    )
                }
            }

            // facilities box
            if(siteFacilityButtonVisibility == View.VISIBLE){
                Box() {
                    Icon(
                        modifier = modifier.fillMaxWidth().padding(5.dp),
                        painter = painterResource(id = R.drawable.ic_site_facility_24),
                        tint = Color(0xFF005E95),
                        contentDescription = "Facilities icon"
                    )
                }
            }
        }
    }
}

@Composable
public fun FloorItemView(
    index: Int,
    selected: Boolean,
    onClick: (Int) -> Unit,
    floorText: String,
    textSize: TextUnit,
    textColor: Color,
    selectedTextColor: Color,
    buttonBackgroundColor: Color,
    selectedButtonBackgroundColor: Color,
    typography: Typography
) {
    Text(
        text = floorText,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Light,
        fontFamily = typography.labelSmall.fontFamily,
        fontSize = textSize,
        color = if (selected) selectedTextColor else textColor,
        modifier = Modifier
            .clickable { onClick.invoke(index) }
            .background(if (selected) selectedButtonBackgroundColor else buttonBackgroundColor)
            .fillMaxWidth()
            .padding(5.dp)
    )
}

@Preview(showBackground = true)
@Composable
internal fun FloorFilterPreview() {
    val viewModel = object : FloorFilterInterface {
        private val _someProperty: MutableStateFlow<String> =
            MutableStateFlow("Hello Indoors Preview")
        override val someProperty: StateFlow<String> = _someProperty.asStateFlow()
    }

    val portal = Portal("https://arcgis.com/")
    val portalItem = PortalItem(portal, "f133a698536f44c8884ad81f80b6cfc7")
    val floorAwareWebMap = ArcGISMap(portalItem)

    FloorFilter(arcGISMap = floorAwareWebMap)
}
