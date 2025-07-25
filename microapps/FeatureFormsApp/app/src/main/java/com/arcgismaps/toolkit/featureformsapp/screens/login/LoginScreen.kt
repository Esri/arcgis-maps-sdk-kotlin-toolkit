/*
 * Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arcgismaps.toolkit.featureformsapp.screens.login

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.arcgismaps.toolkit.authentication.Authenticator
import com.arcgismaps.toolkit.featureformsapp.AnimatedLoading
import com.arcgismaps.toolkit.featureformsapp.R

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onSuccessfulLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    var showEnterpriseLogin by rememberSaveable { mutableStateOf(false) }
    Scaffold (
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            AnimatedContent(
                targetState = loginState is LoginState.Loading || loginState is LoginState.Success,
                transitionSpec = {
                    slideInVertically { h -> h } togetherWith
                            slideOutVertically(
                                animationSpec = tween()
                            ) { h -> h } + fadeOut()
                },
                label = "evaluation loading animation"
            ) {
                Column {
                    if (it) {
                        // show a loading indicator if the currently in progress or for a successful login
                        AnimatedLoading(
                            { true },
                            modifier = Modifier.fillMaxSize(),
                            statusText = stringResource(R.string.signing_in)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(50.dp))
                        LoginOptions(
                            onAgolLoginTapped = {
                                viewModel.login(useOAuth = true)
                            },
                            onEnterpriseLoginTapped = {
                                showEnterpriseLogin = true
                            },
                            skipSignInTapped = {
                                viewModel.skipSignIn()
                            }
                        )
                    }
                }
            }
        }
    }
    EnterpriseLogin(
        visibilityProvider = { showEnterpriseLogin },
        loginViewModel = viewModel,
        onCancel = {
            showEnterpriseLogin = false
        }
    )
    Authenticator(authenticatorState = viewModel.authenticatorState)
    LaunchedEffect(Unit) {
        viewModel.loginState.collect {
            if (it is LoginState.Success) {
                onSuccessfulLogin()
            } else if (it is LoginState.Failed) {
                showEnterpriseLogin = false
                Toast.makeText(context, "Login failed : ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun EnterpriseLogin(
    visibilityProvider: () -> Boolean,
    loginViewModel: LoginViewModel,
    onCancel: () -> Unit
) {
    val visible = visibilityProvider()
    if (visible) {
        var showPortalUrlForm by remember { mutableStateOf(true) }
        if (showPortalUrlForm) {
            PortalURLForm(
                recents = loginViewModel.urlHistory.collectAsState().value,
                onSubmit = { url ->
                    showPortalUrlForm = false
                    loginViewModel.addUrlToHistory(url)
                    loginViewModel.login(url, useOAuth = false)
                },
                onCancel = onCancel
            )
        }
    }
}

@Composable
fun PortalURLForm(
    recents: List<String>,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var url by remember { mutableStateOf("https://") }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(25.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.enter_enterprise_url),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                TextFieldWithHistory(
                    value = url,
                    recents = recents
                ) {
                    url = it
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 25.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(25.dp))
                    Button(onClick = {
                        onSubmit(url)
                    }) {
                        Text(text = stringResource(R.string.login))
                    }
                }
            }
        }
    }
}

@Composable
fun TextFieldWithHistory(
    value: String,
    recents: List<String>,
    onValueChange: (String) -> Unit
) {
    var showRecent by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .onFocusChanged {
                    showRecent = it.hasFocus
                },
            value = value,
            onValueChange = onValueChange,
            trailingIcon = {
                if (recents.isNotEmpty()) {
                    IconButton(onClick = { showRecent = !showRecent }) {
                        Icon(
                            imageVector = if (!showRecent) Icons.Default.KeyboardArrowDown
                            else Icons.Default.KeyboardArrowUp,
                            contentDescription = null
                        )
                    }
                }
            },
            prefix = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_world),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 5.dp)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Crossfade(
            targetState = showRecent && recents.isNotEmpty(), label = "recent urls anim",
        ) {
            if (it) {
                Card(
                    modifier = Modifier.heightIn(max = 175.dp)
                ) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier.verticalScrollbar(state),
                        state = state
                    ) {
                        itemsIndexed(recents) { index, url ->
                            Row(
                                modifier = Modifier
                                    .clickable { onValueChange(url) }
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 10.dp
                                    )
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.icon_history),
                                    contentDescription = null
                                )
                                Text(
                                    text = url,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.icon_restore),
                                    contentDescription = null
                                )
                            }
                            if (index < recents.lastIndex)
                                HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginOptions(
    modifier: Modifier = Modifier,
    onAgolLoginTapped: () -> Unit,
    onEnterpriseLoginTapped: () -> Unit,
    skipSignInTapped: () -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = 50.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // browse demo maps card
        Box(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(15.dp))
                .border(
                    width = 5.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(15.dp)
                )
                .clickable { skipSignInTapped() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_topographic_map),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.browse_demo_maps),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onAgolLoginTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in_with_agol),
                modifier = Modifier.padding(5.dp),
            )
        }
        Button(
            onClick = onEnterpriseLoginTapped,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.sign_in_with_enterprise),
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}

fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 5.dp
): Modifier = composed {
    val targetAlpha = if (state.isScrollInProgress) 1f else 0f
    val duration = if (state.isScrollInProgress) 150 else 500
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration),
        label = ""
    )

    drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        val needDrawScrollbar = state.isScrollInProgress || alpha > 0.0f

        // Draw scrollbar if scrolling or if the animation is still running and lazy column has content
        if (needDrawScrollbar && firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

            drawRoundRect(
                color = color,
                topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                size = Size(width.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(10F, 10F),
                alpha = alpha
            )
        }
    }
}

@Preview
@Composable
fun EnterpriseLoginPreview() {
    PortalURLForm(
        recents = listOf(
            "https://url1.com/portal",
            "https://url2.com/portal",
            "https://url3.com/portal"
        ),
        onSubmit = { _ ->
        }
    ) {

    }
}
