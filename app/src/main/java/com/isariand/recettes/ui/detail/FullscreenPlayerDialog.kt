package com.isariand.recettes.ui.detail

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class FullscreenPlayerDialog : DialogFragment() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val pv = PlayerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            useController = true
        }
        return pv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val url = requireArguments().getString("url")!!
        val muted = requireArguments().getBoolean("muted")
        val pos = requireArguments().getLong("pos")
        val autoplay = requireArguments().getBoolean("autoplay")

        val pv = view as PlayerView
        player = ExoPlayer.Builder(requireContext()).build().also { exo ->
            pv.player = exo
            exo.volume = if (muted) 0f else 1f
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.seekTo(pos)
            exo.playWhenReady = autoplay
        }

        pv.setOnClickListener {
            // tap pour quitter si tu veux (optionnel)
        }
    }

    override fun onDestroyView() {
        player?.release()
        player = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(url: String, muted: Boolean, positionMs: Long, wasPlaying: Boolean) =
            FullscreenPlayerDialog().apply {
                arguments = Bundle().apply {
                    putString("url", url)
                    putBoolean("muted", muted)
                    putLong("pos", positionMs)
                    putBoolean("autoplay", wasPlaying)
                }
            }
    }
}
