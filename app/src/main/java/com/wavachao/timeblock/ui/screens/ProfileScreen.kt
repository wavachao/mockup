package com.wavachao.timeblock.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavachao.timeblock.data.model.BlockCategory
import com.wavachao.timeblock.ui.components.CategoryDot
import com.wavachao.timeblock.ui.components.FieldLabel
import com.wavachao.timeblock.ui.components.SurfaceCard
import com.wavachao.timeblock.ui.icons.BlockIcons
import com.wavachao.timeblock.ui.icons.TimeBlockIcon
import com.wavachao.timeblock.ui.theme.AppTokens
import com.wavachao.timeblock.ui.theme.BrandColors
import com.wavachao.timeblock.ui.theme.Radius
import com.wavachao.timeblock.ui.util.TimeFormat

/**
 * Screen 1's 我的 tab. The mockup only hints at this tab, so it carries the app's
 * self-description: the design tokens it was built from and the reminder status.
 */
@Composable
fun ProfileScreen(
    totalBlocks: Int = 0,
    selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    onOpenToday: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = AppTokens.palette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("我的", color = palette.text, style = AppTokens.type.screenTitle)
        }

        SurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            brush = Brush.linearGradient(listOf(Color(0x3D6D5EF8), Color(0x1422D3EE))),
            contentPadding = 20.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandColors.brandGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    TimeBlockIcon(BlockIcons.Clock, size = 26.dp, tint = Color.White, strokeWidth = 2.2f)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "时间段",
                        color = palette.text,
                        style = AppTokens.type.sectionTitle.copy(fontSize = 17.sp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "把一天切成看得见的时间段",
                        color = palette.textSecondary,
                        style = AppTokens.type.caption,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(label = "已记录时间段", value = "$totalBlocks", modifier = Modifier.weight(1f))
                StatTile(
                    label = "今天",
                    value = TimeFormat.dateWithWeekday(selectedDate).substringBefore(" ·"),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentPadding = 18.dp,
        ) {
            FieldLabel("分类")
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BlockCategory.entries.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryDot(category, size = 10.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = category.displayName,
                            color = palette.text,
                            style = AppTokens.type.body,
                            modifier = Modifier.width(48.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(Radius.pill))
                                .background(category.color.copy(alpha = 0.28f)),
                        )
                        Spacer(Modifier.width(12.dp))
                        TimeBlockIcon(
                            icon = category.icon,
                            size = 16.dp,
                            tint = category.color,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentPadding = 18.dp,
        ) {
            FieldLabel("设计稿对照")
            Spacer(Modifier.height(12.dp))
            TokenRow(name = "主色", value = "#6D5EF8", color = BrandColors.Primary)
            TokenRow(name = "辅助", value = "#A78BFA", color = BrandColors.Secondary)
            TokenRow(name = "强调", value = "#22D3EE", color = BrandColors.Cyan)
            TokenRow(name = "危险", value = "#FB7185", color = AppTokens.palette.danger)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(Radius.inner))
                .background(palette.panel)
                .border(1.dp, palette.stroke, RoundedCornerShape(Radius.inner))
                .clickable(onClick = onOpenToday)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeBlockIcon(BlockIcons.Today, size = 18.dp, tint = palette.textSecondary)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "回到今天的时间轴",
                color = palette.text,
                style = AppTokens.type.body,
                modifier = Modifier.weight(1f),
            )
            TimeBlockIcon(BlockIcons.ChevronRight, size = 15.dp, tint = palette.mutedDim, strokeWidth = 2f)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.inner))
            .background(AppTokens.palette.panelStrong)
            .border(1.dp, AppTokens.palette.stroke, RoundedCornerShape(Radius.inner))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = AppTokens.palette.muted,
            style = AppTokens.type.micro.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = AppTokens.palette.text,
            style = AppTokens.type.bodyStrong.copy(fontWeight = FontWeight(750)),
        )
    }
}

@Composable
private fun TokenRow(name: String, value: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = name,
            color = AppTokens.palette.textSecondary,
            style = AppTokens.type.caption,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = AppTokens.palette.text,
            style = AppTokens.type.caption.copy(fontWeight = FontWeight(700)),
        )
    }
}

/** Placeholder circle used by the empty avatar in future profile work. */
@Composable
private fun AvatarPlaceholder() {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(AppTokens.palette.panelStrong),
    )
}
