package com.kevdadev.musicminds.data.api.models

import com.google.gson.annotations.SerializedName

data class SpotifySearchResponse(
    @SerializedName("tracks")
    val tracks: SpotifyTracks
)

data class SpotifyTracks(
    @SerializedName("items")
    val items: List<SpotifyTrack>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("limit")
    val limit: Int,
    @SerializedName("offset")
    val offset: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?
)

data class SpotifyTrack(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("artists")
    val artists: List<SpotifyArtist>,
    @SerializedName("album")
    val album: SpotifyAlbum,
    @SerializedName("duration_ms")
    val durationMs: Int,
    @SerializedName("preview_url")
    val previewUrl: String?,
    @SerializedName("external_urls")
    val externalUrls: SpotifyExternalUrls
)

data class SpotifyArtist(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("external_urls")
    val externalUrls: SpotifyExternalUrls
)

data class SpotifyAlbum(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("images")
    val images: List<SpotifyImage>,
    @SerializedName("external_urls")
    val externalUrls: SpotifyExternalUrls
)

data class SpotifyImage(
    @SerializedName("url")
    val url: String,
    @SerializedName("height")
    val height: Int?,
    @SerializedName("width")
    val width: Int?
)

data class SpotifyExternalUrls(
    @SerializedName("spotify")
    val spotify: String
)