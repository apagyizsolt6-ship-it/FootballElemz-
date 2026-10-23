package com.footballai.analyzer.data.remote

import com.footballai.analyzer.data.remote.dto.FormMatchDto
import com.footballai.analyzer.data.remote.dto.HighlightsResponse
import com.footballai.analyzer.data.remote.dto.LineupsResponse
import com.footballai.analyzer.data.remote.dto.LiveEventDto
import com.footballai.analyzer.data.remote.dto.MatchDetailResponse
import com.footballai.analyzer.data.remote.dto.MatchesResponse
import com.footballai.analyzer.data.remote.dto.StandingsResponse
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
    suspend fun getMatchById(@Path("id") id: Long): List<MatchDetailResponse>

    @GET("statistics/{matchId}")
    suspend fun getStatistics(@Path("matchId") matchId: Long): List<TeamStatisticsDto>

    @GET("lineups/{matchId}")
    suspend fun getLineups(@Path("matchId") matchId: Long): LineupsResponse

    @GET("highlights")
    suspend fun getHighlights(
        @Query("matchId") matchId: Long,
        @Query("limit") limit: Int = 20
    ): HighlightsResponse

    @GET("events/{id}")
    suspend fun getEvents(@Path("id") matchId: Long): List<LiveEventDto>

    @GET("standings")
    suspend fun getStandings(
        @Query("leagueId") leagueId: Long,
        @Query("season") season: Int
    ): StandingsResponse

    @GET("last-five-games")
    suspend fun getLastFiveGames(@Query("teamId") teamId: Long): List<FormMatchDto>

    @GET("head-2-head")
    suspend fun getHeadToHead(
        @Query("teamIdOne") teamIdOne: Long,
        @Query("teamIdTwo") teamIdTwo: Long
    ): List<FormMatchDto>
}
