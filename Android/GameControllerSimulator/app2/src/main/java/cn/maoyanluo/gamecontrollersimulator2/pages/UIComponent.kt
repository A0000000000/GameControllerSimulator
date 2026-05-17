package cn.maoyanluo.gamecontrollersimulator2.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadButton
import cn.maoyanluo.ui_library.CircleTextButton
import cn.maoyanluo.ui_library.GameControllerTriggerButton
import cn.maoyanluo.ui_library.SquareTextButton


@Composable
fun ActionButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GamepadButton, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        CircleTextButton(
            text = "Y",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            textColor = Color(0xFFFFC107),
            onDown = {
                onKeyEvent(GamepadButton.Y, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.Y, false)
            }
        )

        CircleTextButton(
            text = "X",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            textColor = Color(0xFF2196F3),
            onDown = {
                onKeyEvent(GamepadButton.X, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.X, false)
            }
        )

        CircleTextButton(
            text = "B",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            textColor = Color(0xFFF44336),
            onDown = {
                onKeyEvent(GamepadButton.B, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.B, false)
            }
        )

        CircleTextButton(
            text = "A",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            textColor = Color(0xFF4CAF50),
            onDown = {
                onKeyEvent(GamepadButton.A, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.A, false)
            }
        )
    }
}


@Composable
fun DPadButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: GamepadButton, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        SquareTextButton(
            text = "↑",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(GamepadButton.Top, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.Top, false)
            }
        )
        SquareTextButton(
            text = "↓",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(GamepadButton.Bottom, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.Bottom, false)
            }
        )
        SquareTextButton(
            text = "←",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(GamepadButton.Left, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.Left, false)
            }
        )
        SquareTextButton(
            text = "→",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(GamepadButton.Right, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.Right, false)
            }
        )
    }
}

@Composable
fun LTLBButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    lbPressed: Boolean = false,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: GamepadButton, on: Boolean) -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleTextButton(
            text = "LB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            enabled = !lbPressed,
            externalPressed = lbPressed,
            onDown = {
                onKeyEvent(GamepadButton.LB, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.LB, false)
            }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            GameControllerTriggerButton(
                text = "LT",
                fontSize = fontSize,
                modifier = Modifier.fillMaxHeight(),
                reverseDirection = true,
                onValueChanged = onTriggerChanged,
                minValue = 0,
                maxValue = 255,
                initialValue = 0
            )
        }
    }
}

@Composable
fun RBRTButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    rbPressed: Boolean = false,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: GamepadButton, on: Boolean) -> Unit
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            GameControllerTriggerButton(
                text = "RT",
                fontSize = fontSize,
                modifier = Modifier.fillMaxHeight(),
                reverseDirection = false,
                onValueChanged = onTriggerChanged,
                minValue = 0,
                maxValue = 255,
                initialValue = 0
            )
        }
        CircleTextButton(
            text = "RB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            enabled = !rbPressed,
            externalPressed = rbPressed,
            onDown = {
                onKeyEvent(GamepadButton.RB, true)
            },
            onUp = {
                onKeyEvent(GamepadButton.RB, false)
            }
        )
    }
}
