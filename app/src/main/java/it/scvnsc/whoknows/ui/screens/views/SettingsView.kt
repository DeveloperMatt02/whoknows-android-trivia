package it.scvnsc.whoknows.ui.screens.views

import android.annotation.SuppressLint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import it.scvnsc.whoknows.R
import it.scvnsc.whoknows.ui.screens.components.TopBar
import it.scvnsc.whoknows.ui.theme.WhoKnowsTheme
import it.scvnsc.whoknows.ui.theme.buttonsTextStyle
import it.scvnsc.whoknows.ui.theme.default_elevation
import it.scvnsc.whoknows.ui.theme.difficulty_buttons_height
import it.scvnsc.whoknows.ui.theme.difficulty_buttons_height_landscape
import it.scvnsc.whoknows.ui.theme.difficulty_buttons_width
import it.scvnsc.whoknows.ui.theme.difficulty_buttons_width_landscape
import it.scvnsc.whoknows.ui.theme.home_buttons_shape
import it.scvnsc.whoknows.ui.theme.icon_size_settings
import it.scvnsc.whoknows.ui.theme.icon_size_settings_landscape
import it.scvnsc.whoknows.ui.theme.pressed_elevation
import it.scvnsc.whoknows.ui.viewmodels.SettingsViewModel
import it.scvnsc.whoknows.utils.getLayoutDirection
import it.scvnsc.whoknows.utils.getSurfaceRotation
import it.scvnsc.whoknows.utils.isLandscape

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsView(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel
) {
    //determino orientamento schermo
    val context = LocalContext.current
    val isLandscape = isLandscape()

    val isDarkTheme by settingsViewModel.isDarkTheme.observeAsState(true)
    val isSoundEnabled by settingsViewModel.isSoundEnabled.observeAsState(true)

    WhoKnowsTheme(
        darkTheme = settingsViewModel.isDarkTheme.observeAsState().value == true,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            content = {
                Column(
                    modifier = Modifier
                        .paint(
                            // Replace with your image id
                            painterResource(
                                id = if (isDarkTheme) R.drawable.puzzle_bg_black else R.drawable.puzzle_bg_white
                            ),
                            contentScale = ContentScale.Crop,
                        )
                        .fillMaxSize()
                        .padding(start = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                            .asPaddingValues()
                            .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation() ==3) WindowInsets.displayCutout
                            .asPaddingValues().
                            calculateEndPadding(getLayoutDirection()) else 0.dp,
                            end = if (getSurfaceRotation() == 1) WindowInsets.displayCutout
                                .asPaddingValues()
                                .calculateStartPadding(getLayoutDirection()) else if (getSurfaceRotation() ==3) WindowInsets.displayCutout
                                .asPaddingValues().
                                calculateEndPadding(getLayoutDirection()) else 0.dp
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    //Top app bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        TopBar(
                            navController = navController,
                            onLeftBtnClick = { navController.navigate("home") },
                            leftBtnIcon = Icons.AutoMirrored.Filled.ArrowBack,
                            showTitle = true,
                            title = context.getString(R.string.app_name),
                            showThemeChange = false
                        )
                    }

                    //Settings buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        SettingsButtons(
                            isDarkTheme,
                            isSoundEnabled,
                            onToggleTheme = { settingsViewModel.toggleTheme() },
                            onToggleSound = { settingsViewModel.toggleSound() }
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsButtons(
    isDarkTheme: Boolean,
    isSoundEnabled: Boolean,
    onToggleTheme: () -> Unit,
    onToggleSound: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val isLandscape = isLandscape()

    val buttonModifier = Modifier
        .height(if (isLandscape) difficulty_buttons_height_landscape else difficulty_buttons_height)
        .width(if (isLandscape) difficulty_buttons_width_landscape else difficulty_buttons_width)

    val iconSize = if (isLandscape) icon_size_settings_landscape else icon_size_settings


    //animate icon rotation
    val themeIconRotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 360f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "Theme Icon Rotation"
    )

    // Sound icon fade animation
    val soundIconAlpha by animateFloatAsState(
        targetValue = if (isSoundEnabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "Sound Icon Alpha"
    )



    val buttonContent: @Composable (String, @Composable () -> Unit) -> Unit = { text, icon ->
        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                icon()
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    color = MaterialTheme.colorScheme.onPrimary,
                    text = text,
                    style = buttonsTextStyle
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = MaterialTheme.colorScheme.onPrimary,
                    text = text,
                    style = buttonsTextStyle
                )
                icon()
            }
        }
    }

    val buttonLayout: @Composable (@Composable () -> Unit) -> Unit = { content ->
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }

    buttonLayout {
        Button(
            onClick = onToggleTheme,
            elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
            shape = RoundedCornerShape(home_buttons_shape),
            modifier = buttonModifier
        ) {
            buttonContent("Toggle Theme") {
                Spacer(modifier = Modifier.size(10.dp))

                Icon(
                    if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                        .rotate(themeIconRotation)
                )
            }
        }

        Button(
            onClick = onToggleSound,
            elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
            shape = RoundedCornerShape(home_buttons_shape),
            modifier = buttonModifier
        ) {
            buttonContent("Toggle Sound") {
                Spacer(modifier = Modifier.size(10.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(iconSize)
                ) {
                    // Faded out icon (VolumeOff)
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(1f - soundIconAlpha)
                    )
                    // Faded in icon (VolumeUp)
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(soundIconAlpha)
                    )
                }

            }
        }

        Button(
            onClick = { uriHandler.openUri("https://github.com/DeveloperMatt02/whoknows-android-trivia") },
            elevation = ButtonDefaults.buttonElevation(default_elevation, pressed_elevation),
            shape = RoundedCornerShape(home_buttons_shape),
            modifier = buttonModifier
        ) {
            Text(
                color = MaterialTheme.colorScheme.onPrimary,
                text = "Credits",
                style = buttonsTextStyle
            )
        }
    }
}