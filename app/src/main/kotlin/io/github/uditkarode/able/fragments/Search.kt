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

package io.github.uditkarode.able.fragments

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import io.github.uditkarode.able.model.song.Song
import io.github.uditkarode.able.presentation.search.SearchScreen

class Search : Fragment() {

    interface SongCallback {
        fun sendItem(song: Song, mode: String = "")
    }

    private lateinit var callback: SongCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as Activity as SongCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SearchScreen(onSendItem = { song, mode -> callback.sendItem(song, mode) })
        }
    }
}
