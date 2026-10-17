package com.englishtalk.app.data

/** Built-in learning content. AI features generate more on demand. */
object Catalog {

    val scenarios = listOf(
        Scenario(
            id = "free_talk", emoji = "💬", title = "Free Talk", titleVi = "Trò chuyện tự do",
            description = "An open, friendly chat about any topic the learner likes.",
            aiRole = "a friendly native English-speaking friend", userRole = "themself",
            goal = "Speak freely and fluently for several minutes.", level = "All", customTopic = true,
        ),
        Scenario(
            id = "coffee", emoji = "☕", title = "Ordering at a Café", titleVi = "Gọi đồ ở quán cà phê",
            description = "A busy coffee shop in New York in the morning.",
            aiRole = "a cheerful barista", userRole = "a customer",
            goal = "Order a drink and a snack, customize it, and pay.", level = "A2",
        ),
        Scenario(
            id = "small_talk", emoji = "👋", title = "Small Talk with a Neighbor", titleVi = "Nói chuyện phiếm với hàng xóm",
            description = "Meeting a new neighbor in the hallway of an apartment building.",
            aiRole = "a talkative, friendly neighbor who just moved in", userRole = "a resident",
            goal = "Introduce yourself, chat about the neighborhood and weekend plans.", level = "A2",
        ),
        Scenario(
            id = "airport", emoji = "✈️", title = "Airport Check-in", titleVi = "Làm thủ tục sân bay",
            description = "Checking in for an international flight at the airline counter.",
            aiRole = "an airline check-in agent", userRole = "a passenger",
            goal = "Check in, ask about baggage allowance and choose a seat.", level = "B1",
        ),
        Scenario(
            id = "hotel", emoji = "🏨", title = "Hotel Front Desk", titleVi = "Quầy lễ tân khách sạn",
            description = "Arriving at a hotel and there is a problem with the booking.",
            aiRole = "a polite hotel receptionist", userRole = "a guest",
            goal = "Check in, solve the booking problem and ask for recommendations.", level = "B1",
        ),
        Scenario(
            id = "doctor", emoji = "🩺", title = "Seeing a Doctor", titleVi = "Đi khám bác sĩ",
            description = "A visit to a general practitioner because you feel unwell.",
            aiRole = "a caring family doctor", userRole = "a patient",
            goal = "Describe symptoms, answer questions and understand the advice.", level = "B1",
        ),
        Scenario(
            id = "shopping", emoji = "🛍️", title = "Returning an Item", titleVi = "Đổi trả hàng",
            description = "Returning a jacket that doesn't fit at a clothing store.",
            aiRole = "a store clerk who follows the return policy strictly", userRole = "a customer",
            goal = "Explain the problem and negotiate an exchange or refund.", level = "B1",
        ),
        Scenario(
            id = "job_interview", emoji = "💼", title = "Job Interview", titleVi = "Phỏng vấn xin việc",
            description = "A job interview for a position the learner wants at an international company.",
            aiRole = "a professional but friendly hiring manager", userRole = "a job candidate",
            goal = "Introduce yourself, talk about experience, strengths and ask good questions.", level = "B2",
        ),
        Scenario(
            id = "meeting", emoji = "📊", title = "Team Meeting", titleVi = "Họp nhóm công việc",
            description = "A weekly stand-up meeting in a software team.",
            aiRole = "a team lead running the meeting", userRole = "a team member",
            goal = "Give a status update, raise a blocker and agree on next steps.", level = "B2",
        ),
        Scenario(
            id = "debate", emoji = "🎤", title = "Friendly Debate", titleVi = "Tranh luận thân thiện",
            description = "Debating whether remote work is better than working in an office.",
            aiRole = "a sharp debate partner who takes the opposite side", userRole = "a debater",
            goal = "Give opinions, support them with reasons and respond to counterarguments.", level = "C1",
        ),
    )

    val soundLessons = listOf(
        SoundLesson(
            id = "th", title = "/θ/ và /ð/ – âm TH", subtitle = "think, this, three, mother",
            tip = "Đặt đầu lưỡi giữa hai hàm răng rồi thổi hơi ra. /θ/ không rung dây thanh, /ð/ có rung.",
            sentences = listOf(
                PracticeSentence("I think this is the third Thursday.", "Tôi nghĩ đây là thứ Năm thứ ba."),
                PracticeSentence("My mother and father live together.", "Bố mẹ tôi sống cùng nhau."),
                PracticeSentence("Thank you for those three things.", "Cảm ơn bạn vì ba thứ đó."),
                PracticeSentence("They thought about it with their brothers.", "Họ đã nghĩ về điều đó cùng anh em mình."),
            ),
        ),
        SoundLesson(
            id = "final", title = "Phụ âm cuối", subtitle = "worked, likes, bag, friend",
            tip = "Người Việt hay bỏ âm cuối. Hãy phát âm rõ /t/, /d/, /s/, /z/, /k/ ở cuối từ, đặc biệt đuôi -ed và -s.",
            sentences = listOf(
                PracticeSentence("She likes cats and dogs.", "Cô ấy thích mèo và chó."),
                PracticeSentence("I worked hard and finished it last night.", "Tôi đã làm việc chăm chỉ và hoàn thành nó tối qua."),
                PracticeSentence("He asked his friend for a big bag.", "Anh ấy xin bạn một cái túi to."),
                PracticeSentence("The kids played outside and laughed.", "Bọn trẻ chơi ngoài trời và cười đùa."),
            ),
        ),
        SoundLesson(
            id = "rl", title = "/r/ và /l/", subtitle = "right, light, really, world",
            tip = "/r/: cong lưỡi về sau, không chạm vòm miệng. /l/: đầu lưỡi chạm lợi trên.",
            sentences = listOf(
                PracticeSentence("The red light is on the right.", "Đèn đỏ ở bên phải."),
                PracticeSentence("I really love reading in the library.", "Tôi rất thích đọc sách ở thư viện."),
                PracticeSentence("Larry rarely rolls the ball.", "Larry hiếm khi lăn quả bóng."),
                PracticeSentence("The world is a wonderful place.", "Thế giới là một nơi tuyệt vời."),
            ),
        ),
        SoundLesson(
            id = "vw", title = "/v/ và /w/", subtitle = "very, west, wine, vine",
            tip = "/v/: răng trên chạm môi dưới và rung. /w/: tròn môi như chuẩn bị nói 'u'.",
            sentences = listOf(
                PracticeSentence("We visited a very wet village.", "Chúng tôi đến thăm một ngôi làng rất ẩm ướt."),
                PracticeSentence("Vicky wants a glass of wine.", "Vicky muốn một ly rượu vang."),
                PracticeSentence("The van went west over the river.", "Chiếc xe tải đi về phía tây qua sông."),
            ),
        ),
        SoundLesson(
            id = "ssh", title = "/s/ và /ʃ/", subtitle = "see, she, sip, ship",
            tip = "/s/: lưỡi gần lợi, khe hẹp. /ʃ/: lưỡi lùi về sau, môi hơi tròn như 'suỵt'.",
            sentences = listOf(
                PracticeSentence("She sells seashells by the seashore.", "Cô ấy bán vỏ sò bên bờ biển."),
                PracticeSentence("Sit in the seat on the ship.", "Ngồi vào ghế trên tàu."),
                PracticeSentence("I should wash my shirt and socks.", "Tôi nên giặt áo sơ mi và tất."),
            ),
        ),
        SoundLesson(
            id = "vowels", title = "Nguyên âm ngắn – dài", subtitle = "ship/sheep, full/fool",
            tip = "/iː/ và /uː/ kéo dài, căng môi. /ɪ/ và /ʊ/ ngắn, thả lỏng.",
            sentences = listOf(
                PracticeSentence("The sheep is on the ship.", "Con cừu ở trên tàu."),
                PracticeSentence("Please leave the green bean.", "Làm ơn để lại hạt đậu xanh."),
                PracticeSentence("He took a good look at the pool.", "Anh ấy nhìn kỹ hồ bơi."),
            ),
        ),
        SoundLesson(
            id = "linking", title = "Nối âm & ngữ điệu", subtitle = "Nói trôi chảy như người bản xứ",
            tip = "Nối phụ âm cuối với nguyên âm đầu của từ sau: 'pick it up' → /pɪ.kɪ.tʌp/. Nhấn vào từ mang nghĩa chính.",
            sentences = listOf(
                PracticeSentence("Can I have a cup of tea?", "Cho tôi một tách trà được không?"),
                PracticeSentence("Pick it up and put it on.", "Nhặt nó lên và mặc vào."),
                PracticeSentence("What are you up to this evening?", "Tối nay bạn định làm gì?"),
                PracticeSentence("I'm not sure, but I'll check it out.", "Tôi không chắc, nhưng tôi sẽ xem thử."),
            ),
        ),
        SoundLesson(
            id = "daily", title = "Câu giao tiếp hằng ngày", subtitle = "Shadowing các câu thông dụng",
            tip = "Nghe tốc độ chậm, rồi nhại lại theo đúng nhịp, ngữ điệu và chỗ nhấn của người bản xứ.",
            sentences = listOf(
                PracticeSentence("How's it going?", "Dạo này thế nào?"),
                PracticeSentence("I'm good, thanks. How about you?", "Tôi ổn, cảm ơn. Còn bạn?"),
                PracticeSentence("Could you say that again, please?", "Bạn nói lại được không?"),
                PracticeSentence("No worries, take your time.", "Không sao đâu, cứ từ từ."),
                PracticeSentence("That sounds like a great idea.", "Nghe có vẻ là ý tưởng hay đấy."),
                PracticeSentence("I'm looking forward to seeing you.", "Tôi mong được gặp bạn."),
            ),
        ),
    )

    val decks = listOf(
        VocabDeck(
            id = "daily", emoji = "🏠", title = "Cuộc sống hằng ngày",
            words = listOf(
                VocabWord("hang out", "/hæŋ aʊt/", "đi chơi, tụ tập", "Do you want to hang out this weekend?"),
                VocabWord("run errands", "/rʌn ˈerəndz/", "đi làm việc vặt", "I need to run some errands after work."),
                VocabWord("grab a bite", "/ɡræb ə baɪt/", "ăn nhanh", "Let's grab a bite before the movie."),
                VocabWord("chores", "/tʃɔːrz/", "việc nhà", "I usually do my chores on Saturday morning."),
                VocabWord("commute", "/kəˈmjuːt/", "đi lại (đi làm)", "My commute takes about forty minutes."),
                VocabWord("exhausted", "/ɪɡˈzɔːstɪd/", "kiệt sức", "I'm exhausted after that long day."),
                VocabWord("catch up", "/kætʃ ʌp/", "hàn huyên, cập nhật tin tức", "We should catch up over coffee soon."),
                VocabWord("sleep in", "/sliːp ɪn/", "ngủ nướng", "I love to sleep in on Sundays."),
            ),
        ),
        VocabDeck(
            id = "work", emoji = "💼", title = "Công việc & văn phòng",
            words = listOf(
                VocabWord("deadline", "/ˈdedlaɪn/", "hạn chót", "The deadline for the report is Friday."),
                VocabWord("follow up", "/ˈfɑːloʊ ʌp/", "theo dõi, liên hệ lại", "I'll follow up with the client tomorrow."),
                VocabWord("get the ball rolling", "/ɡet ðə bɔːl ˈroʊlɪŋ/", "bắt đầu triển khai", "Let's get the ball rolling on this project."),
                VocabWord("on the same page", "/ɑːn ðə seɪm peɪdʒ/", "cùng quan điểm, thống nhất", "Let's make sure we're all on the same page."),
                VocabWord("colleague", "/ˈkɑːliːɡ/", "đồng nghiệp", "My colleague helped me with the slides."),
                VocabWord("promotion", "/prəˈmoʊʃn/", "sự thăng chức", "She got a promotion last month."),
                VocabWord("workload", "/ˈwɜːrkloʊd/", "khối lượng công việc", "My workload is crazy this week."),
                VocabWord("touch base", "/tʌtʃ beɪs/", "liên lạc nhanh", "Let's touch base next Monday."),
            ),
        ),
        VocabDeck(
            id = "travel", emoji = "🧳", title = "Du lịch",
            words = listOf(
                VocabWord("itinerary", "/aɪˈtɪnəreri/", "lịch trình", "Can you send me the itinerary?"),
                VocabWord("layover", "/ˈleɪoʊvər/", "quá cảnh", "We have a three-hour layover in Tokyo."),
                VocabWord("carry-on", "/ˈkæri ɑːn/", "hành lý xách tay", "Is this bag okay as a carry-on?"),
                VocabWord("sightseeing", "/ˈsaɪtsiːɪŋ/", "tham quan", "We went sightseeing all day."),
                VocabWord("check out", "/tʃek aʊt/", "trả phòng; xem thử", "What time do we need to check out?"),
                VocabWord("jet lag", "/ˈdʒet læɡ/", "mệt do lệch múi giờ", "I still have terrible jet lag."),
                VocabWord("off the beaten path", "/ɔːf ðə ˈbiːtn pæθ/", "nơi ít người biết", "We love finding places off the beaten path."),
                VocabWord("souvenir", "/ˌsuːvəˈnɪr/", "quà lưu niệm", "I bought a souvenir for my mom."),
            ),
        ),
        VocabDeck(
            id = "feelings", emoji = "😊", title = "Cảm xúc & tính cách",
            words = listOf(
                VocabWord("thrilled", "/θrɪld/", "rất vui mừng", "I'm thrilled to hear the news!"),
                VocabWord("frustrated", "/ˈfrʌstreɪtɪd/", "bực bội", "He felt frustrated with the slow internet."),
                VocabWord("easygoing", "/ˌiːziˈɡoʊɪŋ/", "dễ tính", "My roommate is really easygoing."),
                VocabWord("nervous", "/ˈnɜːrvəs/", "lo lắng, hồi hộp", "I always get nervous before a presentation."),
                VocabWord("down to earth", "/daʊn tə ɜːrθ/", "giản dị, thực tế", "Despite being famous, she's very down to earth."),
                VocabWord("overwhelmed", "/ˌoʊvərˈwelmd/", "choáng ngợp", "I feel overwhelmed by all this work."),
                VocabWord("relieved", "/rɪˈliːvd/", "nhẹ nhõm", "I was so relieved when I passed the test."),
                VocabWord("moody", "/ˈmuːdi/", "thất thường", "He gets moody when he's hungry."),
            ),
        ),
    )
}
