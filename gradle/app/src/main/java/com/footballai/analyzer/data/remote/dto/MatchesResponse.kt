package com.footballai.analyzer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MatchesResponse(
    val data: List<MatchDto> = emptyList(),
    val pagination: PaginationDto? = null,
    val plan: PlanDto? = null
)

@Serializable
data class MatchDto(
    val id: Long,
    val round: String? = null,
    val date: String? = null,
    val country: CountryDto? = null,
    val homeTeam: TeamDto? = null,
    val awayTeam: TeamDto? = null,
    val league: LeagueDto? = null,
    val state: MatchStateDto? = null
)

@Serializable
data class TeamDto(
    val id: Long? = null,
    val name: String? = null,
    val logo: String? = null,
    val type: String? = null
)

@Serializable
data class LeagueDto(
    val id: Long? = null,
    val name: String? = null,
    val logo: String? = null,
    val season: Int? = null
)

@Serializable
data class CountryDto(
    val code: String? = null,
    val name: String? = null,
    val logo: String? = null
)

@Serializable
data class MatchStateDto(
    val description: String? = null,
    val clock: Int? = null,
    val score: ScoreDto? = null
)

@Serializable
data class ScoreDto(
    val current: String? = null,
    val penalties: String? = null
)

@Serializable
data class PaginationDto(
    val totalCount: Int? = null,
    val offset: Int? = null,
    val limit: Int? = null
)

@Serializable
data class PlanDto(
    val tier: String? = null,
    val message: String? = null
)
