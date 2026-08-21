package com.example.sharpoclockserver.configuration

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AiConfig {

    // for admin: generating catalog entries in a structured output
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

//    // for mobile: RAG based search over the watches catalog
//    @Bean
//    fun mobileRAGSearchChatClient(
//        chatClientBuilder: ChatClient.Builder,
//        vectorStore: VectorStore
//    ): ChatClient =
//        chatClientBuilder
//            .defaultSystem(
//                "You help customers find watches. Use ONLY the retrieved catalog " +
//                        "context to answer. If nothing matches, say so."
//            )
//            .defaultAdvisors(
//                QuestionAnswerAdvisor.builder(vectorStore).build()
//            )
//            .build()
//
//    // for mobile: watch recommendation system by age, RAG flavored
//    @Bean
//    fun mobileRAGRecommendationChatClient(
//        chatClientBuilder: ChatClient.Builder,
//        vectorStore: VectorStore
//    ): ChatClient =
//        chatClientBuilder
//            .defaultSystem(
//                "You recommend watches from the catalog based on the user's " +
//                        "profile (e.g. age). Return a short ranked list."
//            )
//            .defaultAdvisors(
//                QuestionAnswerAdvisor.builder(vectorStore).build()
//            )
//            .build()
}