
package com.ixhua.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MokiCard(level: Int) {

    val (greeting, message) = when {
        level >= 10 -> Pair(
            "¡Eres imparable!",
            "Tu disciplina está dando frutos. ¡Sigue conquistando tus metas!"
        )

        level >= 5 -> Pair(
            "¡Te estás superando!",
            "Cada día organizas mejor tu tiempo. ¡Estoy orgulloso de ti!"
        )

        level >= 2 -> Pair(
            "¡Buen trabajo!",
            "Tu constancia está creciendo. ¡Vamos por el siguiente nivel!"
        )

        else -> Pair(
            "¡Comencemos esta aventura!",
            "Cada tarea cuenta. ¡Vamos paso a paso!"
        )
    }

    val card = Color(0xFF17141F)
    val purple = Color(0xFFB65CFF)
    val purpleDark = Color(0xFF7135B5)
    val white = Color(0xFFF7F2FC)
    val gray = Color(0xFF9991A5)

    Surface(
        color = card,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ROSTRO PROVISIONAL DE MOKI
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFECE8F3)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(purpleDark)
                        )

                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(purpleDark)
                        )
                    }

                    Box(
                        Modifier
                            .width(13.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(purpleDark)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "MOKI · NIVEL $level",
                    color = purple,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    greeting,
                    color = white,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    message,
                    color = gray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
