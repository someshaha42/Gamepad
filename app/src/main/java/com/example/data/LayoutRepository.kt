package com.example.data

import com.example.model.KeySpec
import com.example.model.KeyboardLayoutProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

class LayoutRepository(private val layoutDao: LayoutDao) {

    val allLayouts: Flow<List<KeyboardLayoutProfile>> = layoutDao.observeAllLayouts()
        .map { entities ->
            entities.map { entity ->
                val keys = layoutDao.getKeysForLayout(entity.id).map { it.toKeySpec() }
                entity.toDomain(keys)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLayout(layoutId: Long): Flow<KeyboardLayoutProfile> {
        return combine(
            layoutDao.observeAllLayouts(),
            layoutDao.observeKeysForLayout(layoutId)
        ) { layouts, keys ->
            val target = layouts.firstOrNull { it.id == layoutId }
                ?: layouts.firstOrNull { it.isDefault }
                ?: return@combine DefaultSounceLayouts.createDefault35LayoutProfile()
            val resolvedKeys = if (target.id == layoutId) {
                keys.map { it.toKeySpec() }
            } else {
                layoutDao.getKeysForLayout(target.id).map { it.toKeySpec() }
            }
            target.toDomain(if (resolvedKeys.isEmpty()) DefaultSounceLayouts.createSounce35Keys() else resolvedKeys)
        }
    }

    suspend fun ensureDefaultLayoutSeeded(): Long {
        val existingDefault = layoutDao.getLayoutById(1L)
        if (existingDefault == null) {
            val insertedId = layoutDao.insertLayout(
                CustomLayoutEntity(
                    id = 1L,
                    name = DefaultSounceLayouts.GAMING_35_KEYBOARD_NAME,
                    gameTitle = "Default 35-Key Sounce Preset",
                    keySpacingDp = 4f,
                    keyScale = 1f,
                    isDefault = true
                )
            )
            val defaultKeys = DefaultSounceLayouts.createSounce35Keys().map { it.toEntity(insertedId) }
            layoutDao.insertKeys(defaultKeys)
            return insertedId
        }
        val existingKeys = layoutDao.getKeysForLayout(1L)
        if (existingKeys.isEmpty()) {
            layoutDao.insertKeys(DefaultSounceLayouts.createSounce35Keys().map { it.toEntity(1L) })
        }
        return existingDefault.id
    }

    suspend fun addKeyToLayout(layoutId: Long, spec: KeySpec) {
        val currentKeys = layoutDao.getKeysForLayout(layoutId)
        val uniqueId = if (currentKeys.any { it.keyId == spec.id }) {
            "${spec.id}_${System.currentTimeMillis() % 10000}"
        } else {
            spec.id
        }
        val rowKeys = currentKeys.filter { it.row == spec.row }
        val nextCol = (rowKeys.maxOfOrNull { it.column } ?: -1) + 1
        layoutDao.insertKey(
            spec.copy(
                id = uniqueId,
                column = nextCol,
                isCustomAdded = true
            ).toEntity(layoutId)
        )
        touchLayoutTimestamp(layoutId)
    }

    suspend fun removeCustomKey(layoutId: Long, keyId: String) {
        layoutDao.deleteKeyByKeyId(layoutId, keyId)
        normalizeRowColumns(layoutId)
        touchLayoutTimestamp(layoutId)
    }

    suspend fun updateKeySpec(layoutId: Long, updated: KeySpec) {
        val current = layoutDao.getKeysForLayout(layoutId)
        val match = current.firstOrNull { it.keyId == updated.id } ?: return
        layoutDao.updateKey(
            match.copy(
                label = updated.label.ifBlank { match.label },
                hidUsageCode = updated.hidUsageCode,
                row = updated.row.coerceIn(0, 7),
                column = updated.column.coerceAtLeast(0),
                widthWeight = updated.widthWeight.coerceIn(0.6f, 4.0f),
                heightScale = updated.heightScale.coerceIn(0.75f, 2.0f),
                localTapXPercent = updated.localTapXPercent.coerceIn(0.02f, 0.98f),
                localTapYPercent = updated.localTapYPercent.coerceIn(0.02f, 0.98f)
            )
        )
        normalizeRowColumns(layoutId)
        touchLayoutTimestamp(layoutId)
    }

    suspend fun swapOrMoveKeyPosition(layoutId: Long, keyId: String, deltaColumn: Int, deltaRow: Int) {
        val keys = layoutDao.getKeysForLayout(layoutId).toMutableList()
        val idx = keys.indexOfFirst { it.keyId == keyId }
        if (idx == -1) return
        val item = keys[idx]
        val targetRow = (item.row + deltaRow).coerceIn(0, 7)
        if (deltaRow != 0) {
            val targetRowCols = keys.filter { it.row == targetRow }
            val nextCol = (targetRowCols.maxOfOrNull { it.column } ?: -1) + 1
            layoutDao.updateKey(item.copy(row = targetRow, column = nextCol))
        } else if (deltaColumn != 0) {
            val sameRow = keys.filter { it.row == item.row }.sortedBy { it.column }
            val posInRow = sameRow.indexOfFirst { it.keyId == keyId }
            val swapPos = (posInRow + deltaColumn).coerceIn(0, sameRow.lastIndex)
            if (posInRow != -1 && swapPos != posInRow) {
                val other = sameRow[swapPos]
                layoutDao.updateKey(item.copy(column = other.column))
                layoutDao.updateKey(other.copy(column = item.column))
            }
        }
        normalizeRowColumns(layoutId)
        touchLayoutTimestamp(layoutId)
    }

    suspend fun updateLayoutSpacingAndScale(layoutId: Long, spacingDp: Float, scale: Float) {
        val layout = layoutDao.getLayoutById(layoutId) ?: return
        layoutDao.updateLayout(
            layout.copy(
                keySpacingDp = spacingDp.coerceIn(1f, 14f),
                keyScale = scale.coerceIn(0.7f, 1.5f),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveAsNewGameLayout(
        sourceLayoutId: Long,
        newName: String,
        gameTitle: String
    ): Long {
        val sourceLayout = layoutDao.getLayoutById(sourceLayoutId)
        val sourceKeys = layoutDao.getKeysForLayout(sourceLayoutId)
        val newId = layoutDao.insertLayout(
            CustomLayoutEntity(
                name = newName.trim().ifEmpty { "Custom Game Layout" },
                gameTitle = gameTitle.trim().ifEmpty { "Custom Game" },
                keySpacingDp = sourceLayout?.keySpacingDp ?: 4f,
                keyScale = sourceLayout?.keyScale ?: 1f,
                isDefault = false
            )
        )
        val copiedKeys = sourceKeys.map { it.copy(rowId = 0L, layoutId = newId) }
        layoutDao.insertKeys(copiedKeys)
        return newId
    }

    suspend fun deleteLayout(layoutId: Long) {
        if (layoutId != 1L) {
            layoutDao.deleteCustomLayout(layoutId)
        }
    }

    /**
     * Restores ONLY the default 35-key Sounce Gaming Keyboard layout (id = 1)
     * without affecting other saved game layouts or general controller settings.
     */
    suspend fun restoreDefault35KeyLayout() {
        val defaultEntity = CustomLayoutEntity(
            id = 1L,
            name = DefaultSounceLayouts.GAMING_35_KEYBOARD_NAME,
            gameTitle = "Default 35-Key Sounce Preset",
            keySpacingDp = 4f,
            keyScale = 1f,
            isDefault = true,
            updatedAt = System.currentTimeMillis()
        )
        layoutDao.insertLayout(defaultEntity)
        layoutDao.replaceLayoutKeys(
            1L,
            DefaultSounceLayouts.createSounce35Keys().map { it.toEntity(1L) }
        )
    }

    private suspend fun normalizeRowColumns(layoutId: Long) {
        val keys = layoutDao.getKeysForLayout(layoutId)
        val grouped = keys.groupBy { it.row }
        grouped.forEach { (_, rowItems) ->
            rowItems.sortedBy { it.column }.forEachIndexed { newCol, entity ->
                if (entity.column != newCol) {
                    layoutDao.updateKey(entity.copy(column = newCol))
                }
            }
        }
    }

    private suspend fun touchLayoutTimestamp(layoutId: Long) {
        val layout = layoutDao.getLayoutById(layoutId) ?: return
        layoutDao.updateLayout(layout.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun CustomLayoutEntity.toDomain(keys: List<KeySpec>) = KeyboardLayoutProfile(
        id = id,
        name = name,
        gameTitle = gameTitle,
        keys = keys,
        keySpacingDp = keySpacingDp,
        keyScale = keyScale,
        isDefault = isDefault
    )

    private fun CustomKeyEntity.toKeySpec() = KeySpec(
        id = keyId,
        label = label,
        hidUsageCode = hidUsageCode,
        row = row,
        column = column,
        widthWeight = widthWeight,
        heightScale = heightScale,
        isModifier = isModifier,
        modifierMask = modifierMask,
        isFnKey = isFnKey,
        isCapsLock = isCapsLock,
        isCustomAdded = isCustomAdded,
        localTapXPercent = localTapXPercent,
        localTapYPercent = localTapYPercent
    )

    private fun KeySpec.toEntity(layoutId: Long) = CustomKeyEntity(
        layoutId = layoutId,
        keyId = id,
        label = label,
        hidUsageCode = hidUsageCode,
        row = row,
        column = column,
        widthWeight = widthWeight,
        heightScale = heightScale,
        isModifier = isModifier,
        modifierMask = modifierMask,
        isFnKey = isFnKey,
        isCapsLock = isCapsLock,
        isCustomAdded = isCustomAdded,
        localTapXPercent = localTapXPercent,
        localTapYPercent = localTapYPercent
    )
}
