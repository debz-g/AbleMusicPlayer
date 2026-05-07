/*
    Copyright 2020 Udit Karode <udit.karode@gmail.com>
    This file is part of AbleMusicPlayer.
    AbleMusicPlayer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, version 3 of the License.
    AbleMusicPlayer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with AbleMusicPlayer.  If not, see <https://www.gnu.org/licenses/>.
*/

package io.github.uditkarode.able.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.data.player.MusicServiceConnection
import io.github.uditkarode.able.presentation.player.PlayerScreen
import io.github.uditkarode.able.presentation.player.PlayerViewModel
import javax.inject.Inject

/**
 * Full-screen player — thin Compose shell backed by [PlayerViewModel].
 * All business logic lives in the ViewModel; this activity only wires
 * lifecycle (bind / unbind) and hosts the Compose content.
 */
@AndroidEntryPoint
class Player : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    @Inject
    lateinit var connection: MusicServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connection.bind(this)

        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState()
                PlayerScreen(
                    state       = state,
                    onIntent    = viewModel::onIntent,
                    onBack      = { finish() },
                    onShowQueue = { /* Phase 2: queue bottom-sheet */ },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        connection.unbind(this)
    }
}
