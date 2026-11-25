class ApiEndpointResolverTest {
    @Test
    fun chatEndpointTrimsTrailingSlashes() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            ApiEndpointResolver.chatCompletions("https://api.example.com/v1///")
        )
    }

    @Test
    fun modelListSupportsOpenCodeGoInferencePath() {
        assertEquals(
            "https://opencode.ai/inference/go/v1/models",
            ApiEndpointResolver.modelList("https://opencode.ai/inference/go/openai/v1")
        )
    }

    @Test
    fun modelListSupportsOpenCodeGoRootPath() {
        assertEquals(
            "https://opencode.ai/inference/go/v1/models",
            ApiEndpointResolver.modelList("https://opencode.ai/inference/go/")
        )
    }

    @Test
    fun modelListUsesDefaultOpenAiPath() {
        assertEquals(
            "https://api.example.com/v1/models",
            ApiEndpointResolver.modelList("https://api.example.com/v1")
        )
    }
}
