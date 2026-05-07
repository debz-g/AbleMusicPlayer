package io.github.uditkarode.able.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.activities.LocalPlaylist
import io.github.uditkarode.able.presentation.playlists.PlaylistsScreen
import io.github.uditkarode.able.presentation.playlists.PlaylistsViewModel

@AndroidEntryPoint
class Playlists : Fragment() {

    private val viewModel: PlaylistsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            PlaylistsScreen(
                onOpenPlaylist = { name ->
                    startActivity(
                        Intent(requireContext(), LocalPlaylist::class.java)
                            .putExtra("name", name)
                    )
                },
                viewModel = viewModel,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }
}
