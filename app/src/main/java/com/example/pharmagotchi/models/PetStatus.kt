package com.example.pharmagotchi.models

enum class PetEmotion {
    HAPPY,
    SAD,
    CONFUSED,
    IN_PAIN
}

data class PetStatus(
    val emotion: PetEmotion,
    val reason: String
)
