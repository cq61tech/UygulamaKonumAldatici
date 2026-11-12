package io.github.cq61tech.konumaldatici

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bridge = ModuleBridge.info
    val info = remember(bridge) { SystemInfo.collect(ctx, bridge) }
    val version = remember { appVersion(ctx) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = LocalDockPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Appear(0) { BannerCard() }
        Appear(1) { StatusCard(version.first, version.second, bridge) }
        Appear(2) { SystemCard(info) }
    }
}

@Composable
private fun BannerCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.25f to Color.Transparent,
                        1f to Color(0xCC000000),
                    )
                )
        )
        Text(
            text = stringResource(R.string.banner_text),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 23.sp,
            lineHeight = 28.sp,
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = Shadow(Color(0x99000000), Offset(0f, 3f), 8f)
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun StatusCard(versionName: String, versionCode: Long, bridge: ModuleBridge.Info?) {
    val cs = MaterialTheme.colorScheme
    val active = bridge != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) cs.primaryContainer else cs.tertiaryContainer,
            contentColor = if (active) cs.onPrimaryContainer else cs.onTertiaryContainer,
        ),
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (active) cs.primary else cs.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (active) Icons.Filled.Check else Icons.Filled.Info,
                    contentDescription = null,
                    tint = if (active) cs.onPrimary else cs.onTertiary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.home_status_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(if (active) R.string.home_status_on else R.string.home_status_off),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (bridge != null) {
                        stringResource(
                            R.string.home_status_on_desc,
                            bridge.name,
                            bridge.version,
                            bridge.api,
                            bridge.scope.size,
                        )
                    } else {
                        stringResource(R.string.home_status_off_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.home_version, versionName, versionCode),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SystemCard(info: List<Pair<Int, String>>) {
    SectionCard(stringResource(R.string.home_system_title)) {
        info.forEach { (label, value) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.4f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
    }
}
