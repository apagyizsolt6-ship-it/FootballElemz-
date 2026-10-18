package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.remote.dto.HighlightsResponse
import com.footballai.analyzer.data.remote.dto.LineupsResponse
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchesResponse
import com.footballai.analyzer.data.remote.dto.TeamStatisticsDto
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

    /** Highlightly: GET /statistics/{matchId} — lista a válasz */
    @GET("statistics/{matchId}")
    suspend fun getStatistics(
        @Path("matchId") matchId: Long
    ): List<TeamStatisticsDto>

    /** Highlightly: GET /lineups/{matchId} — homeTeam + awayTeam objektum */
    @GET("lineups/{matchId}")
    suspend fun getLineups(
        @Path("matchId") matchId: Long
    ): LineupsResponse

    /** Highlightly: GET /highlights?matchId= */
    @GET("highlights")
    suspend fun getHighlights(
        @Query("matchId") matchId: Long,
        @Query("limit") limit: Int = 20
    ): HighlightsResponse
}
