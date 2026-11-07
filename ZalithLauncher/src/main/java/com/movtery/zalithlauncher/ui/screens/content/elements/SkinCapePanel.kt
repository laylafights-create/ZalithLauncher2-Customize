package com.movtery.zalithlauncher.ui.screens.content.elements

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.account.wardrobe.EmptyCape
import com.movtery.zalithlauncher.game.account.wardrobe.SkinModelType
import com.movtery.zalithlauncher.game.account.wardrobe.capeLocalRes
import com.movtery.zalithlauncher.game.account.yggdrasil.PlayerProfile
import com.movtery.zalithlauncher.game.account.yggdrasil.findUsing
import com.movtery.zalithlauncher.ui.components.McButton
import com.movtery.zalithlauncher.ui.components.McButtonStyle
import com.movtery.zalithlauncher.ui.theme.McColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Tab "Skin" kiểu trang hồ sơ minecraft.net:
 *  - Mục "Skin": chọn file skin (.png), chọn tay Wide/Slim, áp dụng / huỷ (offline có thêm Reset)
 *  - Mục "Choose your cape / OWNED CAPES": lưới cape có radio, "No Cape" + các cape sở hữu.
 *    Chọn cape là áp dụng ngay (giống trang web), chỉ tài khoản Microsoft mới có cape.
 */
@Composable
fun SkinCapePanel(
    modifier: Modifier = Modifier,
    account: Account?,
    availableCapes: List<PlayerProfile.Cape>?,
    skinState: ChangeSkin,
    isImportingSkin: Boolean,
    onSkinStateChange: (ChangeSkin) -> Unit,
    onSkinPicked: (Uri) -> Unit,
    onApplySkin: (File, SkinModelType) -> Unit,
    onResetSkin: () -> Unit,
    onApplyCape: (PlayerProfile.Cape) -> Unit,
    onFetchCapes: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(McColors.Panel)
            .border(1.dp, McColors.Outline)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (account == null) {
            Text(
                text = stringResource(R.string.account_no_account),
                color = McColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }

        SkinSection(
            account = account,
            skinState = skinState,
            isImportingSkin = isImportingSkin,
            onSkinStateChange = onSkinStateChange,
            onSkinPicked = onSkinPicked,
            onApplySkin = onApplySkin,
            onResetSkin = onResetSkin
        )

        if (account.isMicrosoftAccount()) {
            CapeSection(
                account = account,
                availableCapes = availableCapes,
                onApplyCape = onApplyCape,
                onFetchCapes = onFetchCapes
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = title,
            color = McColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = McColors.PlayGreen, thickness = 2.dp)
    }
}

@Composable
private fun SkinSection(
    account: Account,
    skinState: ChangeSkin,
    isImportingSkin: Boolean,
    onSkinStateChange: (ChangeSkin) -> Unit,
    onSkinPicked: (Uri) -> Unit,
    onApplySkin: (File, SkinModelType) -> Unit,
    onResetSkin: () -> Unit,
) {
    val skinPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(onSkinPicked)
    }

    SectionHeader(stringResource(R.string.account_change_skin))

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        McButton(
            onClick = { skinPicker.launch(arrayOf("image/png")) },
            enabled = !isImportingSkin
        ) {
            if (isImportingSkin) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = McColors.TextPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = stringResource(R.string.account_change_skin),
                color = McColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        if (account.isLocalAccount() && account.hasSkinFile && skinState != ChangeSkin.ResetSkin) {
            McButton(onClick = { onSkinStateChange(ChangeSkin.ResetSkin) }) {
                Text(
                    text = stringResource(R.string.generic_reset),
                    color = McColors.TextPrimary
                )
            }
        }
    }

    when (skinState) {
        ChangeSkin.None -> {}

        is ChangeSkin.ChangeSkinData -> {
            val model = skinState.data.getModelTypeNullable()
            val enabled = skinState.data !is ChangeSkin.SkinModel.Rejected

            Text(
                text = stringResource(R.string.account_change_skin_arm_style),
                color = McColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McRadioTile(
                    modifier = Modifier.weight(1f),
                    selected = model == SkinModelType.STEVE,
                    enabled = enabled,
                    text = stringResource(R.string.account_change_skin_arm_wide),
                    onClick = {
                        onSkinStateChange(skinState.copy(data = ChangeSkin.SkinModel.Model(SkinModelType.STEVE)))
                    }
                )
                McRadioTile(
                    modifier = Modifier.weight(1f),
                    selected = model == SkinModelType.ALEX,
                    enabled = enabled,
                    text = stringResource(R.string.account_change_skin_arm_slim),
                    onClick = {
                        onSkinStateChange(skinState.copy(data = ChangeSkin.SkinModel.Model(SkinModelType.ALEX)))
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onSkinStateChange(ChangeSkin.None) }
                ) {
                    Text(text = stringResource(R.string.generic_cancel), color = McColors.TextPrimary)
                }
                McButton(
                    modifier = Modifier.weight(1f),
                    style = McButtonStyle.Play,
                    onClick = {
                        onApplySkin(skinState.cacheFile, skinState.data.getModelType())
                        onSkinStateChange(ChangeSkin.None)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.generic_confirm),
                        color = McColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        ChangeSkin.ResetSkin -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                McButton(
                    modifier = Modifier.weight(1f),
                    onClick = { onSkinStateChange(ChangeSkin.None) }
                ) {
                    Text(text = stringResource(R.string.generic_cancel), color = McColors.TextPrimary)
                }
                McButton(
                    modifier = Modifier.weight(1f),
                    style = McButtonStyle.Play,
                    onClick = {
                        onResetSkin()
                        onSkinStateChange(ChangeSkin.None)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.generic_confirm),
                        color = McColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CapeSection(
    account: Account,
    availableCapes: List<PlayerProfile.Cape>?,
    onApplyCape: (PlayerProfile.Cape) -> Unit,
    onFetchCapes: () -> Unit,
) {
    // Tải danh sách cape lần đầu khi vào tab (và khi đổi tài khoản)
    LaunchedEffect(account.uniqueUUID) {
        if (availableCapes == null) onFetchCapes()
    }

    SectionHeader(stringResource(R.string.account_change_cape))

    if (availableCapes == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = McColors.TextPrimary)
            Text(
                text = stringResource(R.string.account_change_cape_fetch_all),
                color = McColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val using = availableCapes.findUsing() ?: EmptyCape
    val capes = remember(availableCapes) { listOf(EmptyCape) + availableCapes }

    // Lưới 2 cột như trang web: dựng bằng Row để nằm gọn trong cột cuộn của panel
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        capes.chunked(2).forEach { rowCapes ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCapes.forEach { cape ->
                    CapeCard(
                        modifier = Modifier.weight(1f),
                        cape = cape,
                        selected = cape == using || (cape.id.isNotEmpty() && cape.id == using.id),
                        onClick = {
                            if (cape != using) onApplyCape(cape)
                        }
                    )
                }
                if (rowCapes.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CapeCard(
    modifier: Modifier = Modifier,
    cape: PlayerProfile.Cape,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val preview by produceState<ImageBitmap?>(null, cape, density) {
        value = withContext(Dispatchers.IO) {
            if (cape == EmptyCape) null
            else runCatching {
                getCapeAvatar(cape = cape, size = 88.dp, density = density)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val localRes = cape.capeLocalRes()
    val name = if (localRes != null) stringResource(localRes) else cape.alias

    Column(
        modifier = modifier
            .border(1.dp, if (selected) McColors.PlayGreenLight else McColors.Outline)
            .background(McColors.Background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = preview
            if (bmp != null) {
                Image(
                    modifier = Modifier.height(88.dp),
                    bitmap = bmp,
                    contentDescription = null,
                    filterQuality = FilterQuality.None
                )
            } else if (cape == EmptyCape) {
                // Ô viền nét đứt giống "No Cape" trên web
                Box(
                    modifier = Modifier
                        .size(width = 55.dp, height = 88.dp)
                        .border(1.dp, McColors.TextMuted)
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = McColors.TextMuted)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(McColors.Panel)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            McRadioDot(selected = selected)
            androidx.compose.material3.Text(
                text = name,
                color = McColors.TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun McRadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .border(1.5.dp, if (selected) McColors.TextPrimary else McColors.TextSecondary, androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(McColors.TextPrimary, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
private fun McRadioTile(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .border(1.dp, if (selected) McColors.PlayGreenLight else McColors.Outline)
            .background(McColors.Background.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        McRadioDot(selected = selected)
        Text(text = text, color = McColors.TextPrimary, fontSize = 13.sp, maxLines = 1)
    }
}
