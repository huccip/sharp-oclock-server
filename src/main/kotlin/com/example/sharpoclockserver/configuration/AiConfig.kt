package com.example.sharpoclockserver.configuration

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    @Bean
    fun adminListingAssistantChatClient(
        chatClientBuilder: ChatClient.Builder
    ): ChatClient =
        chatClientBuilder
            .defaultSystem(
                "You are an expert horologist helping fill a watch shop catalog. " +
                        "Given a short user prompt, return realistic product details."
            )
            .build()

//    @Bean
//    fun mobileRAGRecommendationChatClient(
//        chatClientBuilder: ChatClient.Builder,
//        vectorStore: VectorStore
//    ): ChatClient =
//        chatClientBuilder
//            .defaultSystem(
//                "You recommend watches from the catalog based on the user's " +
//                        "profile (e.g. age, budget, previous orders, ...Etc). Return a short ranked list."
//            )
//            .defaultAdvisors(
//                QuestionAnswerAdvisor.builder(vectorStore).build()
//            )
//            .build()
}