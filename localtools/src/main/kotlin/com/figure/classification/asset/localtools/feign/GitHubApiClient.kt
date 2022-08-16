package com.figure.classification.asset.localtools.feign

import com.figure.classification.asset.localtools.models.github.GitHubReleaseResponse
import feign.Param
import feign.RequestLine

/**
 * Simple client to communicate with GitHub's open api to retrieve an asset for an open source project.
 */
interface GitHubApiClient {
    @RequestLine("GET /repos/{organization}/{repository}/releases/latest")
    fun getLatestRelease(
        @Param("organization") organization: String,
        @Param("repository") repository: String,
    ): GitHubReleaseResponse

    @RequestLine("GET /repos/{organization}/{repository}/releases/tags/{tag}")
    fun getReleaseByTag(
        @Param("organization") organization: String,
        @Param("repository") repository: String,
        @Param("tag") tag: String,
    ): GitHubReleaseResponse

    companion object {
        fun new(): GitHubApiClient = FeignUtil.getBuilder().target(GitHubApiClient::class.java, "https://api.github.com")
    }
}
