package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.remote.dto.HighlightsResponse
import com.footballai.analyzer.data.remote.dto.LineupsResponse
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchesResponse
import com.footballai.analyzer.data.remote.dto.StatisticsResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HighlightlyApi {

    @GET("matches")
    suspend fun getMatches(
        @Query("date") date: String,
        @Query("timezone") timezone: String = "Europe/Budapest",
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): MatchesResponse

    @GET("matches/{id}")
    suspend fun getMatchById(
        @Path("id") id: Long
    ): List<MatchDetailResponse>

    @GET("matches/{id}/statistics")
    suspend fun getStatistics(
        @Path("id") id: Long
    ): StatisticsResponse

    @GET("matches/{id}/lineups")
    suspend fun getLineups(
        @Path("id") id: Long
    ): LineupsResponse

    @GET("matches/{id}/highlights")
    suspend fun getHighlights(
        @Path("id") matchId: Long
    ): HighlightsResponse
}
