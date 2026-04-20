package com.garageledger.shortcuts

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuickActionTargetTest {
    @Test
    fun resolvesRouteSegmentsAndEditorRoutes() {
        val target = QuickActionTarget.fromRouteSegment("fuelup")

        assertThat(target).isEqualTo(QuickActionTarget.FUEL_UP)
        assertThat(target?.route()).isEqualTo("quickadd/fuelup")
        assertThat(target?.editorRoute(42L)).isEqualTo("fuelup/42/-1")
    }
}
