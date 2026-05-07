package io.github.uditkarode.able.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.uditkarode.able.activities.LibraryDetail
import io.github.uditkarode.able.presentation.library.LibraryMode
import io.github.uditkarode.able.presentation.library.LibraryScreen
import io.github.uditkarode.able.presentation.library.LibraryViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Library : Fragment() {

    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            LibraryScreen(
                onOpenGroup = { label, mode -> openDetail(label, mode) },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun openDetail(label: String, mode: LibraryMode) {
        lifecycleScope.launch {
            val state = viewModel.state.first { !it.isLoading }
            val filter: (io.github.uditkarode.able.model.song.Song) -> Boolean = when (mode) {
                LibraryMode.ARTISTS -> { s -> s.artist.ifBlank { "Unknown" } == label }
                LibraryMode.ALBUMS  -> { s -> s.album.ifBlank { "Unknown" } == label }
            }
            LibraryDetail.pendingSongs = ArrayList(state.songs.filter(filter))
            startActivity(
                Intent(requireContext(), LibraryDetail::class.java).putExtra("title", label)
            )
        }
    }
}
