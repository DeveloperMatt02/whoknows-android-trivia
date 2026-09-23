package it.scvnsc.whoknows.ui.screens.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.scvnsc.whoknows.R
import it.scvnsc.whoknows.ui.screens.components.TopBar
import it.scvnsc.whoknows.ui.theme.WhoKnowsTheme
import it.scvnsc.whoknows.ui.screens.components.AutoResizeText
import it.scvnsc.whoknows.ui.theme.buttonsTextStyle
import it.scvnsc.whoknows.ui.theme.default_elevation
import it.scvnsc.whoknows.ui.theme.home_buttons_height
import it.scvnsc.whoknows.ui.theme.home_buttons_height_landscape
import it.scvnsc.whoknows.ui.theme.home_buttons_shape
import it.scvnsc.whoknows.ui.theme.home_buttons_width
import it.scvnsc.whoknows.ui.theme.home_buttons_width_landscape
import it.scvnsc.whoknows.ui.theme.pressed_elevation
import it.scvnsc.whoknows.ui.theme.small_padding
import it.scvnsc.whoknows.ui.theme.titleTextStyle
import it.scvnsc.whoknows.ui.viewmodels.SettingsViewModel
import it.scvnsc.whoknows.utils.getLayoutDirection
import it.scvnsc.whoknows.utils.getSurfaceRotation
import it.scvnsc.whoknows.utils.isLandscape

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeView(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel
) {

    //determino orientamento schermo
    val isLandscape = isLandscape()

    val context = LocalContext.current

    WhoKnowsTheme(darkTheme = settingsViewModel.isDarkTheme.observeAsState().value == true) {
        Scaffold(
            //contenuto principale
            content = {
                if(isLandscape) {
                    Column (
                        modifier = Modifier
                            .paint(
                                // Replace with your image id
                                painterResource(
                                    id = if (settingsViewModel.isDarkTheme.observeAsState().value == true) R.drawable.puzzle_bg_black else R.drawable.puzzle_bg_white
                                ),
                                contentScale = ContentScale.Crop
                            )
                            .fillMaxSize()

                            .padding(start = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                                .asPaddingValues()
                                .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation()==3) WindowInsets.displayCutout
                                .asPaddingValues().
                                calculateEndPadding(getLayoutDirection()) else 0.dp,
                                end = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation()==3) WindowInsets.displayCutout
                                    .asPaddingValues().
                                    calculateEndPadding(getLayoutDirection()) else 0.dp
                            )
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Box (
                            modifier = Modifier
                                .fillMaxWidth()
                        ){
                            TopBar(
                                showTitle = true,
                                title = context.getString(R.string.app_name),
                                showRightButton = true,
                                showThemeChange = true,
                                settingsViewModel = settingsViewModel
                            )
                        }

                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Box (
                                modifier = Modifier
                                    .fillMaxSize()
//                                    .padding(start = bottom_bar_padding, end = bottom_bar_padding)
                            ){
                                HomeViewButtons(navController)
                            }
                        }
                    }
                } else {
                    Column (
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .paint(
                                // Replace with your image id
                                painterResource(
                                    id = if (settingsViewModel.isDarkTheme.observeAsState().value == true) R.drawable.puzzle_bg_black else R.drawable.puzzle_bg_white
                                ),
                                contentScale = ContentScale.Crop
                            )
                            .fillMaxSize()

                            .padding(start = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                                .asPaddingValues()
                                .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation()==3) WindowInsets.displayCutout
                                .asPaddingValues().
                                calculateEndPadding(getLayoutDirection()) else 0.dp,
                                end = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                                    .asPaddingValues()
                                    .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation()==3) WindowInsets.displayCutout
                                    .asPaddingValues().
                                    calculateEndPadding(getLayoutDirection()) else 0.dp
                            )
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ){
                        Box (
                            modifier = Modifier
                                .fillMaxWidth()
                        ){
                            TopBar(
                                showTitle = false,
                                showRightButton = true,
                                showThemeChange = true,
                                settingsViewModel = settingsViewModel
                            )
                        }

                        Box (
                            modifier = Modifier
                                .fillMaxSize()
//                                .padding(bottom = bottom_bar_padding)
                        ){
                            HomeViewButtons(navController)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun HomeViewButtons(navController: NavHostController) {
    val context = LocalContext.current
    val isLandscape = isLandscape()

    if (isLandscape){
        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
        ) {
            //play button
            Button(
                onClick = {
                    navController.navigate("game")
                },
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                shape = RoundedCornerShape(home_buttons_shape),
                modifier = Modifier
                    .height(home_buttons_height_landscape)
                    .width(home_buttons_width_landscape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Play",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }

            }

            //stats button
            Button(
                onClick = {
                    navController.navigate("stats")
                },
                shape = RoundedCornerShape(home_buttons_shape),
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                modifier = Modifier
                    .height(home_buttons_height_landscape)
                    .width(home_buttons_width_landscape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Your games",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }

            //settings button
            Button(
                onClick = {
                    navController.navigate("settings")
                },
                shape = RoundedCornerShape(home_buttons_shape),
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                modifier = Modifier
                    .height(home_buttons_height_landscape)
                    .width(home_buttons_width_landscape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Settings",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }


    } else {
        //I pulsanti si dividono lo spazio verticale disponibile (fino a un massimo di home_buttons_height),
        //cosi' su schermi bassi o con testo/visualizzazione ingranditi non vengono tagliati
        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            //Titolo applicazione
            AppTitle(context)

            //play button
            Button(
                onClick = {
                    navController.navigate("game")
                },
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                shape = RoundedCornerShape(home_buttons_shape),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = home_buttons_height)
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .widthIn(max = home_buttons_width)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Play",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }

            }

            //stats button
            Button(
                onClick = {
                    navController.navigate("stats")
                },
                shape = RoundedCornerShape(home_buttons_shape),
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = home_buttons_height)
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .widthIn(max = home_buttons_width)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Your games",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }

            //settings button
            Button(
                onClick = {
                    navController.navigate("settings")
                },
                shape = RoundedCornerShape(home_buttons_shape),
                elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = home_buttons_height)
                    .fillMaxHeight()
                    .fillMaxWidth(0.8f)
                    .widthIn(max = home_buttons_width)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(small_padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )

                    Spacer(modifier = Modifier.size(15.dp))

                    AutoResizeText(
                        text = "Settings",
                        style = buttonsTextStyle,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AppTitle(context: Context) {
    val text = context.getString(R.string.app_name)

    // Creiamo una transizione infinita per animare il movimento delle lettere
    val infiniteTransition = rememberInfiniteTransition(label = "")

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        text.forEachIndexed { index, char ->
            // Creiamo un'animazione per la traslazione verticale e la scala per ogni lettera
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            Text(
                text = char.toString(),
                style = titleTextStyle,
                modifier = Modifier
                    .graphicsLayer(
                        translationY = offsetY,  // Salto verticale della lettera
                        scaleX = scale,  // Scala orizzontale della lettera
                        scaleY = scale   // Scala verticale della lettera
                    )
            )
        }
    }
}

