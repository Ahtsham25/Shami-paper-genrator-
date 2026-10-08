package com.example.data

object SeedData {

    fun defaultSubjects(): List<SubjectEntity> {
        val class9 = listOf(
            SubjectEntity(
                id = "c9_chemistry",
                classLevel = "9",
                orderIndex = 0,
                nameEn = "Chemistry",
                nameUr = "کیمسٹری",
                iconKey = "science",
                colorHex = "#0D9488",
                isFreeByDefault = true
            ),
            SubjectEntity(
                id = "c9_physics",
                classLevel = "9",
                orderIndex = 1,
                nameEn = "Physics",
                nameUr = "فزکس",
                iconKey = "bolt",
                colorHex = "#2563EB",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_urdu",
                classLevel = "9",
                orderIndex = 2,
                nameEn = "Urdu",
                nameUr = "اردو",
                iconKey = "menu_book",
                colorHex = "#7C3AED",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_english",
                classLevel = "9",
                orderIndex = 3,
                nameEn = "English",
                nameUr = "انگلش",
                iconKey = "translate",
                colorHex = "#DB2777",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_quran",
                classLevel = "9",
                orderIndex = 4,
                nameEn = "Tarjuma-tul-Quran",
                nameUr = "ترجمۃ القرآن",
                iconKey = "auto_stories",
                colorHex = "#059669",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_islamiat",
                classLevel = "9",
                orderIndex = 5,
                nameEn = "Islamiat",
                nameUr = "اسلامیات",
                iconKey = "mosque",
                colorHex = "#D97706",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_computer",
                classLevel = "9",
                orderIndex = 6,
                nameEn = "Computer",
                nameUr = "کمپیوٹر",
                iconKey = "computer",
                colorHex = "#4F46E5",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c9_biology",
                classLevel = "9",
                orderIndex = 7,
                nameEn = "Biology",
                nameUr = "بائیو",
                iconKey = "biotech",
                colorHex = "#E11D48",
                isFreeByDefault = false
            )
        )

        val class10 = listOf(
            SubjectEntity(
                id = "c10_chemistry",
                classLevel = "10",
                orderIndex = 0,
                nameEn = "Chemistry",
                nameUr = "کیمسٹری",
                iconKey = "science",
                colorHex = "#0D9488",
                isFreeByDefault = true
            ),
            SubjectEntity(
                id = "c10_physics",
                classLevel = "10",
                orderIndex = 1,
                nameEn = "Physics",
                nameUr = "فزکس",
                iconKey = "bolt",
                colorHex = "#2563EB",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_urdu",
                classLevel = "10",
                orderIndex = 2,
                nameEn = "Urdu",
                nameUr = "اردو",
                iconKey = "menu_book",
                colorHex = "#7C3AED",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_english",
                classLevel = "10",
                orderIndex = 3,
                nameEn = "English",
                nameUr = "انگلش",
                iconKey = "translate",
                colorHex = "#DB2777",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_quran",
                classLevel = "10",
                orderIndex = 4,
                nameEn = "Tarjuma-tul-Quran",
                nameUr = "ترجمۃ القرآن",
                iconKey = "auto_stories",
                colorHex = "#059669",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_pakstudies",
                classLevel = "10",
                orderIndex = 5,
                nameEn = "Pakistan Studies",
                nameUr = "مطالعہ پاکستان",
                iconKey = "flag",
                colorHex = "#16A34A",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_computer",
                classLevel = "10",
                orderIndex = 6,
                nameEn = "Computer",
                nameUr = "کمپیوٹر",
                iconKey = "computer",
                colorHex = "#4F46E5",
                isFreeByDefault = false
            ),
            SubjectEntity(
                id = "c10_biology",
                classLevel = "10",
                orderIndex = 7,
                nameEn = "Biology",
                nameUr = "بائیو",
                iconKey = "biotech",
                colorHex = "#E11D48",
                isFreeByDefault = false
            )
        )

        return class9 + class10
    }

    private val subjectChaptersMap: Map<String, List<Pair<String, String>>> = mapOf(
        "c9_chemistry" to listOf(
            "Ch 1: Fundamentals of Chemistry" to "باب 1: کیمسٹری کے بنیادی اصول",
            "Ch 2: Structure of Atoms" to "باب 2: ایٹم کی ساخت",
            "Ch 3: Periodic Table & Periodicity" to "باب 3: پیریوڈک ٹیبل اور خصوصیات",
            "Ch 4: Structure of Molecules" to "باب 4: مالیکیولز کی ساخت"
        ),
        "c9_physics" to listOf(
            "Ch 1: Physical Quantities & Measurement" to "باب 1: طبعی مقداریں اور پیمائش",
            "Ch 2: Kinematics" to "باب 2: کائنی میٹکس (حرکیات)",
            "Ch 3: Dynamics" to "باب 3: ڈائنامکس (قوت اور حرکت)",
            "Ch 4: Turning Effect of Forces" to "باب 4: قوتوں کا گردشی اثر"
        ),
        "c9_urdu" to listOf(
            "Ch 1: Hijrat-e-Nabwi (ﷺ)" to "باب 1: ہجرتِ نبوی ﷺ",
            "Ch 2: Mirza Ghalib Ke Aadaat-o-Khasail" to "باب 2: مرزا غالب کے عادات و خصائل",
            "Ch 3: Kahili" to "باب 3: کاہلی",
            "Ch 4: Shairoon Ke Lateefay" to "باب 4: شاعروں کے لطیفے"
        ),
        "c9_english" to listOf(
            "Unit 1: The Saviour of Mankind" to "یونٹ 1: انسانیت کے نجات دہندہ ﷺ",
            "Unit 2: Patriotism" to "یونٹ 2: حب الوطنی",
            "Unit 3: Media and Its Impact" to "یونٹ 3: میڈیا اور اس کے اثرات",
            "Unit 4: Hazrat Asma (R.A)" to "یونٹ 4: حضرت اسماء رضی اللہ عنہا"
        ),
        "c9_quran" to listOf(
            "Surah Maryam (Introduction & Themes)" to "سورۃ مریم (تعارف اور مضامین)",
            "Surah Ta-Ha (Selected Verses)" to "سورۃ طٰہٰ (منتخب آیات و ترجمہ)",
            "Surah Al-Anbiya (Key Lessons)" to "سورۃ الانبیاء (اہم مضامین)",
            "Surah Al-Hajj (Translation & Tafseer)" to "سورۃ الحج (ترجمہ اور تشریح)"
        ),
        "c9_islamiat" to listOf(
            "Bab 1: Quran Majeed & Hadith-e-Nabwi" to "باب اول: قرآن مجید و حدیث نبوی ﷺ",
            "Bab 2: Imaniyat-o-Ibadat (Aqeedah Tauheed)" to "باب دوم: ایمانیات و عبادات (عقیدہ توحید و رسالت)",
            "Bab 3: Seerat-e-Tayyaba (ﷺ)" to "باب سوم: سیرتِ طیبہ ﷺ",
            "Bab 4: Akhlaq-o-Aadaab" to "باب چہارم: اخلاق و آداب"
        ),
        "c9_computer" to listOf(
            "Unit 1: Problem Solving & Flowcharts" to "یونٹ 1: پرابلم سالونگ اور فلو چارٹس",
            "Unit 2: Binary System & Data Representation" to "یونٹ 2: بائنری سسٹم اور ڈیٹا",
            "Unit 3: Computer Networks" to "یونٹ 3: کمپیوٹر نیٹ ورکس",
            "Unit 4: Data and Privacy" to "یونٹ 4: ڈیٹا اور پرائیویسی"
        ),
        "c9_biology" to listOf(
            "Ch 1: Introduction to Biology" to "باب 1: بائیولوجی کا تعارف",
            "Ch 2: Solving a Biological Problem" to "باب 2: بائیولوجیکل مسئلے کا حل",
            "Ch 3: Biodiversity" to "باب 3: بائیو ڈائیورسٹی (حیاتیاتی تنوع)",
            "Ch 4: Cells and Tissues" to "باب 4: سیلز اور ٹشوز"
        ),
        "c10_chemistry" to listOf(
            "Ch 9: Chemical Equilibrium" to "باب 9: کیمیکل ایکوی لبریم",
            "Ch 10: Acids, Bases and Salts" to "باب 10: ایسڈز، بیسز اور سالٹس",
            "Ch 11: Organic Chemistry" to "باب 11: آرگینک کیمسٹری",
            "Ch 12: Hydrocarbons" to "باب 12: ہائیڈرو کاربنز"
        ),
        "c10_physics" to listOf(
            "Ch 10: Simple Harmonic Motion & Waves" to "باب 10: سمپل ہارمونک موشن اور ویوز",
            "Ch 11: Sound" to "باب 11: آواز (Sound)",
            "Ch 12: Geometrical Optics" to "باب 12: جیومیٹریکل آپٹکس",
            "Ch 13: Electrostatics" to "باب 13: الیکٹرو سٹیٹکس"
        ),
        "c10_urdu" to listOf(
            "Ch 1: Hamd & Naat" to "باب 1: حمد اور نعت",
            "Ch 2: مرزا محمد سعید (Mirza Muhammad Saeed)" to "باب 2: مرزا محمد سعید",
            "Ch 3: نظریہ پاکستان (Nazriya-e-Pakistan)" to "باب 3: نظریہ پاکستان",
            "Ch 4: پرستان کی شہزادی (Paristan Ki Shehzadi)" to "باب 4: پرستان کی شہزادی"
        ),
        "c10_english" to listOf(
            "Unit 1: Hazrat Muhammad (ﷺ) an Embodiment of Justice" to "یونٹ 1: حضرت محمد ﷺ مجسمِ عدل و انصاف",
            "Unit 2: Chinese New Year" to "یونٹ 2: چینی نیا سال",
            "Unit 3: Try Again (Poem & Grammar)" to "یونٹ 3: دوبارہ کوشش کریں",
            "Unit 4: First Aid" to "یونٹ 4: ابتدائی طبی امداد"
        ),
        "c10_quran" to listOf(
            "Surah Al-Inaam (Introduction & Translation)" to "سورۃ الانعام (تعارف اور ترجمہ)",
            "Surah Al-A'raf (Key Themes & Verses)" to "سورۃ الاعراف (اہم مضامین و آیات)",
            "Surah Yunus (Translation & Wisdom)" to "سورۃ یونس (ترجمہ اور حکمت)",
            "Surah Hud & Surah Al-Kahf" to "سورۃ ہود اور سورۃ الکہف"
        ),
        "c10_pakstudies" to listOf(
            "Ch 1: History of Pakistan (1971 to Present)" to "باب 1: تاریخِ پاکستان (1971 تا حال)",
            "Ch 2: Foreign Relations of Pakistan" to "باب 2: پاکستان کے خارجہ تعلقات",
            "Ch 3: Economic Development of Pakistan" to "باب 3: پاکستان کی معاشی ترقی",
            "Ch 4: Population, Society & Culture of Pakistan" to "باب 4: پاکستان کی آبادی، معاشرت اور ثقافت"
        ),
        "c10_computer" to listOf(
            "Unit 1: Introduction to Programming in C" to "یونٹ 1: سی پروگرامنگ کا تعارف",
            "Unit 2: User Interaction & Operators" to "یونٹ 2: یوزر انٹریکشن اور آپریٹرز",
            "Unit 3: Conditional Logic (if-else)" to "یونٹ 3: کنڈیشنل لاجک (if-else)",
            "Unit 4: Data Structures & Loops" to "یونٹ 4: ڈیٹا سٹرکچرز اور لوپس"
        ),
        "c10_biology" to listOf(
            "Ch 10: Gaseous Exchange" to "باب 10: گیسوں کا تبادلہ",
            "Ch 11: Homeostasis" to "باب 11: ہومیو سٹیسس",
            "Ch 12: Coordination and Control" to "باب 12: کوآرڈینیشن اور کنٹرول",
            "Ch 13: Support and Movement" to "باب 13: سہارا اور حرکت"
        )
    )

    fun defaultChapters(subjects: List<SubjectEntity>): List<ChapterEntity> {
        val list = mutableListOf<ChapterEntity>()
        for (subject in subjects) {
            val rawChapters = subjectChaptersMap[subject.id] ?: listOf(
                "Ch 1: Introduction to ${subject.nameEn}" to "باب 1: ${subject.nameUr} کا تعارف",
                "Ch 2: Core Principles of ${subject.nameEn}" to "باب 2: ${subject.nameUr} کے بنیادی اصول",
                "Ch 3: Advanced Concepts" to "باب 3: اہم تصورات"
            )
            rawChapters.forEachIndexed { index, (en, ur) ->
                val chNum = index + 1
                list.add(
                    ChapterEntity(
                        id = "${subject.id}_ch$chNum",
                        subjectId = subject.id,
                        classLevel = subject.classLevel,
                        chapterNumber = chNum,
                        titleEn = en,
                        titleUr = ur,
                        isFreeByDefault = (chNum == 1)
                    )
                )
            }
        }
        return list
    }

    fun defaultQuestions(chapters: List<ChapterEntity>, subjects: List<SubjectEntity>): List<QuestionEntity> {
        val subjectMap = subjects.associateBy { it.id }
        val result = mutableListOf<QuestionEntity>()

        for (ch in chapters) {
            val subj = subjectMap[ch.subjectId] ?: continue
            when {
                ch.id == "c9_chemistry_ch1" -> {
                    result.addAll(chemistryCh1Questions(ch))
                }
                ch.id == "c10_chemistry_ch1" -> {
                    result.addAll(chemistry10Ch1Questions(ch))
                }
                ch.id == "c10_pakstudies_ch1" -> {
                    result.addAll(pakStudies10Ch1Questions(ch))
                }
                else -> {
                    result.addAll(genericRichBilingualQuestions(ch, subj))
                }
            }
        }
        return result
    }

    private fun chemistryCh1Questions(ch: ChapterEntity): List<QuestionEntity> = listOf(
        // MCQs
        QuestionEntity(
            id = "${ch.id}_mcq_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "Industrial chemistry deals with the manufacturing of compounds:",
            questionUr = "انڈسٹریل کیمسٹری میں کمپاؤنڈز کی تیاری کا مطالعہ کیا جاتا ہے:",
            optionAEn = "In the laboratory",
            optionBEn = "On micro scale",
            optionCEn = "On commercial scale",
            optionDEn = "On atomic scale",
            optionAUr = "لیبارٹری میں",
            optionBUr = "مائیکرو پیمانے پر",
            optionCUr = "تجارتی پیمانے پر",
            optionDUr = "ایٹمی پیمانے پر",
            correctOption = "C",
            marks = 1,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "Which one of the following elements is found in most abundance in the Earth's crust?",
            questionUr = "مندرجہ ذیل میں سے کون سا ایلیمنٹ قشرِ ارض (Earth's crust) میں سب سے زیادہ پایا جاتا ہے؟",
            optionAEn = "Oxygen (47%)",
            optionBEn = "Aluminium (7.8%)",
            optionCEn = "Silicon (28%)",
            optionDEn = "Iron (5%)",
            optionAUr = "آکسیجن (47%)",
            optionBUr = "ایلومینیم (7.8%)",
            optionCUr = "سلیکون (28%)",
            optionDUr = "آئرن (5%)",
            correctOption = "A",
            marks = 1,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_3",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "The molar mass of H2SO4 is:",
            questionUr = "سلفیورک ایسڈ (H2SO4) کا مولر ماس کیا ہے؟",
            optionAEn = "98 g/mol",
            optionBEn = "98 amu",
            optionCEn = "9.8 g/mol",
            optionDEn = "49 g/mol",
            optionAUr = "98 گرام فی مول",
            optionBUr = "98 اے ایم یو",
            optionCUr = "9.8 گرام فی مول",
            optionDUr = "49 گرام فی مول",
            correctOption = "A",
            marks = 1,
            sortOrder = 3
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_4",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "How many number of moles are equivalent to 8 grams of CO2?",
            questionUr = "کاربن ڈائی آکسائیڈ (CO2) کے 8 گرام میں کتنے مولز ہوتے ہیں؟",
            optionAEn = "0.15 moles",
            optionBEn = "0.18 moles",
            optionCEn = "0.21 moles",
            optionDEn = "0.24 moles",
            optionAUr = "0.15 مولز",
            optionBUr = "0.18 مولز",
            optionCUr = "0.21 مولز",
            optionDUr = "0.24 مولز",
            correctOption = "B",
            marks = 1,
            sortOrder = 4
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_5",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "Which one of the following is a triatomic molecule?",
            questionUr = "درج ذیل میں سے کون سا ٹرائی ایٹامک مالیکیول ہے؟",
            optionAEn = "H2",
            optionBEn = "N2",
            optionCEn = "CO",
            optionDEn = "H2O",
            optionAUr = "ہائیڈروجن (H2)",
            optionBUr = "نائٹروجن (N2)",
            optionCUr = "کاربن مونو آکسائیڈ (CO)",
            optionDUr = "پانی (H2O)",
            correctOption = "D",
            marks = 1,
            sortOrder = 5
        ),
        // Short Questions
        QuestionEntity(
            id = "${ch.id}_short_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Define Industrial Chemistry and Analytical Chemistry.",
            questionUr = "انڈسٹریل کیمسٹری اور اینالیٹیکل کیمسٹری کی تعریف کریں۔",
            marks = 2,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_short_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Differentiate between Physical Properties and Chemical Properties with examples.",
            questionUr = "طبعی خصوصیات اور کیمیائی خصوصیات میں مثالوں کے ساتھ فرق واضح کریں۔",
            marks = 2,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_short_3",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Define Empirical Formula and Molecular Formula with one example each.",
            questionUr = "ایمپیریکل فارمولا اور مالیکیولر فارمولا کی تعریف کریں اور ایک ایک مثال دیں۔",
            marks = 2,
            sortOrder = 3
        ),
        QuestionEntity(
            id = "${ch.id}_short_4",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "What is Avogadro's Number (NA)? Give its numerical value.",
            questionUr = "ایووگیڈروز نمبر (NA) کیا ہے؟ اس کی عددی قیمت لکھیں۔",
            marks = 2,
            sortOrder = 4
        ),
        QuestionEntity(
            id = "${ch.id}_short_5",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Differentiate between Homoatomic and Heteroatomic molecules.",
            questionUr = "ہومو ایٹامک اور ہیٹرو ایٹامک مالیکیولز میں فرق بیان کریں۔",
            marks = 2,
            sortOrder = 5
        ),
        // Long Questions
        QuestionEntity(
            id = "${ch.id}_long_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "State any five differences between a Compound and a Mixture in detail.",
            questionUr = "کمپاؤنڈ اور مکسچر کے درمیان کوئی سے پانچ تفصیلی فرق بیان کریں۔",
            marks = 5,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_long_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "Define Mole and explain the relationship between Mole, Mass, and Avogadro's Number with examples.",
            questionUr = "مول (Mole) کی تعریف کریں اور مثالوں کی مدد سے مول، ماس اور ایووگیڈروز نمبر کا تعلق واضح کریں۔",
            marks = 5,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_long_3",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "Explain the main branches of Chemistry and their scope in daily life.",
            questionUr = "کیمسٹری کی اہم شاخیں اور روزمرہ زندگی میں ان کا دائرہ کار تفصیل سے لکھیں۔",
            marks = 5,
            sortOrder = 3
        )
    )

    private fun chemistry10Ch1Questions(ch: ChapterEntity): List<QuestionEntity> = listOf(
        QuestionEntity(
            id = "${ch.id}_mcq_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "In an irreversible reaction, dynamic equilibrium:",
            questionUr = "ناقابلِ واپسی (Irreversible) ری ایکشن میں ڈائنامک ایکوی لبریم:",
            optionAEn = "Is established quickly",
            optionBEn = "Never establishes",
            optionCEn = "Establishes after completion",
            optionDEn = "Depends on catalyst",
            optionAUr = "جلد قائم ہوتا ہے",
            optionBUr = "کبھی قائم نہیں ہوتا",
            optionCUr = "مکمل ہونے کے بعد قائم ہوتا ہے",
            optionDUr = "کیٹالسٹ پر منحصر ہے",
            correctOption = "B",
            marks = 1,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "Active mass is represented by square brackets and its unit is:",
            questionUr = "ایکٹو ماس کو بڑی بریکٹس [ ] سے ظاہر کیا جاتا ہے اور اس کا یونٹ ہے:",
            optionAEn = "mol dm⁻³",
            optionBEn = "mol dm³",
            optionCEn = "g cm⁻³",
            optionDEn = "mol⁻¹ dm³",
            optionAUr = "mol dm⁻³",
            optionBUr = "mol dm³",
            optionCUr = "g cm⁻³",
            optionDUr = "mol⁻¹ dm³",
            correctOption = "A",
            marks = 1,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_3",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "Who presented the Law of Mass Action in 1869?",
            questionUr = "1869ء میں لا آف ماس ایکشن کس نے پیش کیا؟",
            optionAEn = "Guldberg and Waage",
            optionBEn = "Bohr and Rutherford",
            optionCEn = "Lewis and Bronsted",
            optionDEn = "Arrhenius",
            optionAUr = "گلڈ برگ اور واگے",
            optionBUr = "بوہر اور ردرفورڈ",
            optionCUr = "لیوس اور برونسٹڈ",
            optionDUr = "آرہینیس",
            correctOption = "A",
            marks = 1,
            sortOrder = 3
        ),
        QuestionEntity(
            id = "${ch.id}_short_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Define Reversible Reaction and Dynamic Equilibrium.",
            questionUr = "ریورسیبل ری ایکشن اور ڈائنامک ایکوی لبریم کی تعریف کریں۔",
            marks = 2,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_short_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Write two macroscopic characteristics of Forward and Reverse reactions.",
            questionUr = "فارورڈ اور ریورس ری ایکشن کی دو دو میکروسکوپک خصوصیات لکھیں۔",
            marks = 2,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_short_3",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "What is meant by Equilibrium Constant (Kc)? How are its units determined?",
            questionUr = "ایکوی لبریم کونسٹنٹ (Kc) سے کیا مراد ہے؟ اس کے یونٹس کیسے معلوم کیے جاتے ہیں؟",
            marks = 2,
            sortOrder = 3
        ),
        QuestionEntity(
            id = "${ch.id}_long_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "State the Law of Mass Action and derive the expression for Equilibrium Constant (Kc) for a general reaction.",
            questionUr = "لا آف ماس ایکشن بیان کریں اور ایک جنرل ری ایکشن کے لیے ایکوی لبریم کونسٹنٹ (Kc) کی مساوات اخذ کریں۔",
            marks = 5,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_long_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "Explain the importance of Equilibrium Constant (Kc) in predicting the direction and extent of a chemical reaction.",
            questionUr = "کیمیائی ری ایکشن کی سمت اور حد کی پیش گوئی کرنے میں ایکوی لبریم کونسٹنٹ (Kc) کی اہمیت واضح کریں۔",
            marks = 5,
            sortOrder = 2
        )
    )

    private fun pakStudies10Ch1Questions(ch: ChapterEntity): List<QuestionEntity> = listOf(
        QuestionEntity(
            id = "${ch.id}_mcq_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "When was the Constitution of 1973 enforced in Pakistan?",
            questionUr = "پاکستان میں 1973ء کا آئین کب نافذ کیا گیا؟",
            optionAEn = "23rd March 1973",
            optionBEn = "14th August 1973",
            optionCEn = "6th September 1973",
            optionDEn = "25th December 1973",
            optionAUr = "23 مارچ 1973ء",
            optionBUr = "14 اگست 1973ء",
            optionCUr = "6 ستمبر 1973ء",
            optionDUr = "25 دسمبر 1973ء",
            correctOption = "B",
            marks = 1,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_mcq_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.MCQ.code,
            questionEn = "When did Pakistan conduct its nuclear tests in Chagai Hills?",
            questionUr = "پاکستان نے چاغی کے مقام پر ایٹمی دھماکے کب کیے؟",
            optionAEn = "28 May 1998",
            optionBEn = "14 August 1997",
            optionCEn = "23 March 1999",
            optionDEn = "11 May 1998",
            optionAUr = "28 مئی 1998ء",
            optionBUr = "14 اگست 1997ء",
            optionCUr = "23 مارچ 1999ء",
            optionDUr = "11 مئی 1998ء",
            correctOption = "A",
            marks = 1,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_short_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "Write any three salient features of the 1973 Constitution of Pakistan.",
            questionUr = "1973ء کے آئین کی کوئی سی تین اہم خصوصیات تحریر کریں۔",
            marks = 2,
            sortOrder = 1
        ),
        QuestionEntity(
            id = "${ch.id}_short_2",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.SHORT.code,
            questionEn = "What is meant by Simla Agreement (1972)? Write two points.",
            questionUr = "اعلانِ شملہ (1972ء) سے کیا مراد ہے؟ دو نکات لکھیں۔",
            marks = 2,
            sortOrder = 2
        ),
        QuestionEntity(
            id = "${ch.id}_long_1",
            chapterId = ch.id,
            subjectId = ch.subjectId,
            classLevel = ch.classLevel,
            type = QuestionType.LONG.code,
            questionEn = "Describe the major Islamic provisions of the Constitution of 1973 in detail.",
            questionUr = "1973ء کے آئین کی اہم اسلامی دفعات تفصیل سے بیان کریں۔",
            marks = 5,
            sortOrder = 1
        )
    )

    private fun genericRichBilingualQuestions(ch: ChapterEntity, subj: SubjectEntity): List<QuestionEntity> {
        return listOf(
            // 4 MCQs
            QuestionEntity(
                id = "${ch.id}_mcq_1",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.MCQ.code,
                questionEn = "Which fundamental principle is studied in ${ch.titleEn} (${subj.nameEn})?",
                questionUr = "${subj.nameUr} کے سبق (${ch.titleUr}) میں کون سا بنیادی اصول بیان کیا گیا ہے؟",
                optionAEn = "Primary Concept & Definition",
                optionBEn = "Standard Law & Application",
                optionCEn = "Experimental Verification",
                optionDEn = "All of these",
                optionAUr = "بنیادی تصور اور تعریف",
                optionBUr = "معیاری قانون اور اطلاق",
                optionCUr = "تجرباتی تصدیق",
                optionDUr = "یہ تمام درست ہیں",
                correctOption = "D",
                marks = 1,
                sortOrder = 1
            ),
            QuestionEntity(
                id = "${ch.id}_mcq_2",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.MCQ.code,
                questionEn = "Identify the SI / standard unit or key term associated with ${ch.titleEn}:",
                questionUr = "${ch.titleUr} سے متعلقہ معیاری اکائی یا اہم اصطلاح کی نشاندہی کریں:",
                optionAEn = "Standard Unit A",
                optionBEn = "Derived Unit B",
                optionCEn = "Base Constant C",
                optionDEn = "Reference Factor D",
                optionAUr = "معیاری اکائی الف",
                optionBUr = "ماخوذ اکائی ب",
                optionCUr = "بنیادی مستقل ج",
                optionDUr = "حوالہ جاتی جزو د",
                correctOption = "A",
                marks = 1,
                sortOrder = 2
            ),
            QuestionEntity(
                id = "${ch.id}_mcq_3",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.MCQ.code,
                questionEn = "Which of the following statement is true according to ${ch.titleEn}?",
                questionUr = "${ch.titleUr} کے مطابق درج ذیل میں سے کون سا بیان درست ہے؟",
                optionAEn = "First postulate is universal",
                optionBEn = "Second law holds under standard conditions",
                optionCEn = "Both A and B",
                optionDEn = "None of the above",
                optionAUr = "پہلا مفروضہ آفاقی ہے",
                optionBUr = "دوسرا قانون معیاری حالات میں لاگو ہوتا ہے",
                optionCUr = "الف اور ب دونوں",
                optionDUr = "ان میں سے کوئی نہیں",
                correctOption = "C",
                marks = 1,
                sortOrder = 3
            ),
            // 3 Short Questions
            QuestionEntity(
                id = "${ch.id}_short_1",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.SHORT.code,
                questionEn = "Define the core concept of ${ch.titleEn} with two suitable examples.",
                questionUr = "${ch.titleUr} کے بنیادی تصور کی تعریف کریں اور دو مناسب مثالیں دیں۔",
                marks = 2,
                sortOrder = 1
            ),
            QuestionEntity(
                id = "${ch.id}_short_2",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.SHORT.code,
                questionEn = "Write down three important characteristics discussed in ${ch.titleEn}.",
                questionUr = "${ch.titleUr} میں بیان کردہ تین اہم خصوصیات تحریر کریں۔",
                marks = 2,
                sortOrder = 2
            ),
            QuestionEntity(
                id = "${ch.id}_short_3",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.SHORT.code,
                questionEn = "Why is ${ch.titleEn} important in Class ${ch.classLevel}th ${subj.nameEn}?",
                questionUr = "جماعت ${if (ch.classLevel == "9") "نہم" else "دہم"} کے مضمون ${subj.nameUr} میں ${ch.titleUr} کی کیا اہمیت ہے؟",
                marks = 2,
                sortOrder = 3
            ),
            // 2 Long Questions
            QuestionEntity(
                id = "${ch.id}_long_1",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.LONG.code,
                questionEn = "Explain the main topic of ${ch.titleEn} in detail along with labelled examples/points.",
                questionUr = "${ch.titleUr} کے مرکزی موضوع کی مثالوں اور اہم نکات کے ساتھ تفصیلی وضاحت کریں۔",
                marks = 5,
                sortOrder = 1
            ),
            QuestionEntity(
                id = "${ch.id}_long_2",
                chapterId = ch.id,
                subjectId = ch.subjectId,
                classLevel = ch.classLevel,
                type = QuestionType.LONG.code,
                questionEn = "Write a comprehensive note on the practical applications and summary of ${ch.titleEn}.",
                questionUr = "${ch.titleUr} کے عملی اطلاقات اور خلاصے پر جامع نوٹ لکھیں۔",
                marks = 5,
                sortOrder = 2
            )
        )
    }
}
