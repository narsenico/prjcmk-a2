package it.amonshore.comikkua.ui.releases.adapter

import it.amonshore.comikkua.data.release.ComicsRelease

typealias OnReleaseClick = (release: ComicsRelease) -> Unit
typealias OnReleaseTogglePurchase = (release: ComicsRelease) -> Unit
typealias OnReleaseToggleOrder = (release: ComicsRelease) -> Unit
typealias OnReleaseMenuClick = (release: ComicsRelease) -> Unit