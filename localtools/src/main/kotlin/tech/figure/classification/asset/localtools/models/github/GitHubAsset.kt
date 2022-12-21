package tech.figure.classification.asset.localtools.models.github

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class GitHubAsset(
    val url: String,
    val id: Long,
    val nodeId: String,
    val name: String,
    val label: String,
    val uploader: GitHubAuthor,
    val contentType: String,
    val state: String,
    val size: Long,
    val downloadCount: Long,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
    val browserDownloadUrl: String,
)
