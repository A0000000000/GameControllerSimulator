package cn.maoyanluo.ui_library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import cn.maoyanluo.game_event_common_library.Button
import cn.maoyanluo.game_event_common_library.DPad


@Composable
fun ActionButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit
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
                onKeyEvent(Button.Y, true)
            },
            onUp = {
                onKeyEvent(Button.Y, false)
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
                onKeyEvent(Button.X, true)
            },
            onUp = {
                onKeyEvent(Button.X, false)
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
                onKeyEvent(Button.B, true)
            },
            onUp = {
                onKeyEvent(Button.B, false)
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
                onKeyEvent(Button.A, true)
            },
            onUp = {
                onKeyEvent(Button.A, false)
            }
        )
    }
}

@Composable
fun ActionButtons2(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        CircleTextButton(
            text = "△",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(Button.Y, true)
            },
            onUp = {
                onKeyEvent(Button.Y, false)
            }
        )

        CircleTextButton(
            text = "○",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(Button.X, true)
            },
            onUp = {
                onKeyEvent(Button.X, false)
            }
        )

        CircleTextButton(
            text = "×",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(Button.B, true)
            },
            onUp = {
                onKeyEvent(Button.B, false)
            }
        )

        CircleTextButton(
            text = "□",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(Button.A, true)
            },
            onUp = {
                onKeyEvent(Button.A, false)
            }
        )
    }
}

@Composable
fun DPadButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: DPad, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        SquareTextButton(
            text = "↑",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(DPad.TOP, true)
            },
            onUp = {
                onKeyEvent(DPad.TOP, false)
            }
        )
        SquareTextButton(
            text = "↓",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(DPad.BOTTOM, true)
            },
            onUp = {
                onKeyEvent(DPad.BOTTOM, false)
            }
        )
        SquareTextButton(
            text = "←",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(DPad.LEFT, true)
            },
            onUp = {
                onKeyEvent(DPad.LEFT, false)
            }
        )
        SquareTextButton(
            text = "→",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(DPad.RIGHT, true)
            },
            onUp = {
                onKeyEvent(DPad.RIGHT, false)
            }
        )
    }
}

@Composable
fun DPadButtons2(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit
) {
    Box(modifier = modifier) {
        SquareTextButton(
            text = "T",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.TopCenter),
            onDown = {
                onKeyEvent(Button.TOP, true)
            },
            onUp = {
                onKeyEvent(Button.TOP, false)
            }
        )
        SquareTextButton(
            text = "B",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.BottomCenter),
            onDown = {
                onKeyEvent(Button.BOTTOM, true)
            },
            onUp = {
                onKeyEvent(Button.BOTTOM, false)
            }
        )
        SquareTextButton(
            text = "L",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterStart),
            onDown = {
                onKeyEvent(Button.LEFT, true)
            },
            onUp = {
                onKeyEvent(Button.LEFT, false)
            }
        )
        SquareTextButton(
            text = "F",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxSize(0.33f)
                .align(Alignment.CenterEnd),
            onDown = {
                onKeyEvent(Button.RIGHT, true)
            },
            onUp = {
                onKeyEvent(Button.RIGHT, false)
            }
        )
    }
}

@Composable
fun LTLBButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit
) {
    Row(modifier = modifier) {
        GameControllerTriggerButton(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            reverseDirection = true,
            onValueChanged = onTriggerChanged
        )

        SquareTextButton(
            text = "LB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            onDown = {
                onKeyEvent(Button.LB, true)
            },
            onUp = {
                onKeyEvent(Button.LB, false)
            }
        )
    }
}

@Composable
fun RBRTButtons(
    modifier: Modifier,
    fontSize: TextUnit,
    onTriggerChanged: (value: Int) -> Unit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit
) {
    Row(modifier = modifier) {
        SquareTextButton(
            text = "RB",
            fontSize = fontSize,
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f),
            onDown = {
                onKeyEvent(Button.RB, true)
            },
            onUp = {
                onKeyEvent(Button.RB, false)
            }
        )

        GameControllerTriggerButton(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            reverseDirection = false,
            onValueChanged = onTriggerChanged
        )
    }
}

@Composable
fun LeftButtonGroup(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit) {
    Row(modifier = modifier) {
        RectangleTextButton(
            text = "L2",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(Button.L2, true)
            },
            onUp = {
                onKeyEvent(Button.L2, false)
            }
        )
        RectangleTextButton(
            text = "LB",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(Button.LB, true)
            },
            onUp = {
                onKeyEvent(Button.LB, false)
            }
        )
    }
}

@Composable
fun RightButtonGroup(
    modifier: Modifier,
    fontSize: TextUnit,
    onKeyEvent: (btn: Button, on: Boolean) -> Unit) {
    Row(modifier = modifier) {
        RectangleTextButton(
            text = "RB",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(Button.RB, true)
            },
            onUp = {
                onKeyEvent(Button.RB, false)
            }
        )
        RectangleTextButton(
            text = "R2",
            fontSize = fontSize,
            modifier = Modifier
                .weight(1f),
            onDown = {
                onKeyEvent(Button.R2, true)
            },
            onUp = {
                onKeyEvent(Button.R2, false)
            }
        )
    }
}