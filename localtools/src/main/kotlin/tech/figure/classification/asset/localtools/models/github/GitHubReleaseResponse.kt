package tech.figure.classification.asset.localtools.models.github

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class GitHubReleaseResponse(
    val url: String,
    val assetsUrl: String,
    val uploadUrl: String,
    val htmlUrl: String,
    val id: Long,
    val author: GitHubAuthor,
    val nodeId: String,
    val tagName: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val createdAt: OffsetDateTime,
    val publishedAt: OffsetDateTime,
    val assets: List<GitHubAsset>,
    val tarballUrl: String,
    val zipballUrl: String,
    val body: String
)
