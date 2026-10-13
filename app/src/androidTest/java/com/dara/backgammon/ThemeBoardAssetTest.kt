package com.dara.backgammon

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Raster regression guard: all themes must retain the exact approved geometry but distinct materials. */
@RunWith(AndroidJUnit4::class)
class ThemeBoardAssetTest {
    @Test fun themeMastersHaveStableGeometryAndDistinctAppearance() {
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        val ids = listOf(
            R.drawable.empty_board_final_surface,
            R.drawable.empty_board_brushed_metal,
            R.drawable.empty_board_obsidian_ivory,
            R.drawable.empty_board_crystal_glass,
            R.drawable.empty_board_royal_sapphire
        )
        val signatures = ids.map { id ->
            val bitmap = resources.openRawResource(id).use { BitmapFactory.decodeStream(it) }
            assertEquals(1160, bitmap.width)
            assertEquals(684, bitmap.height)
            var signature = 17L
            for (y in 20 until bitmap.height step 47) for (x in 20 until bitmap.width step 53) {
                signature = signature * 31L + bitmap.getPixel(x, y)
            }
            bitmap.recycle()
            signature
        }
        assertEquals("Every theme needs its own rendered material", ids.size, signatures.distinct().size)
        assertTrue(signatures.none { it == 0L })
    }
}
