package com.movtery.petlib

/** Emote phát theo yêu cầu (như bánh xe emote). [id] trùng chỉ số trong pool Rust. */
enum class PetEmote(internal val id: Int) {
    Fidget1(0), Fidget2(1), Fidget3(2),
    Jump(3), Spin(4), Wave(5), Stomp(6), Dance(7),
    Peek(8), Headbang(9), Kick(10), Stretch(11), Taunt(12),
    Clap(13), Bow(14), Shrug(15), Think(16),
}
