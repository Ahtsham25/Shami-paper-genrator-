package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Bi-directional Auto-Translator between Urdu Medium and English Medium for questions & MCQ options:
 * - If the admin uploads/pastes Urdu Medium only -> English Medium is auto-generated (and Bilingual shows both).
 * - If the admin uploads/pastes English Medium only -> Urdu Medium is auto-generated (and Bilingual shows both).
 * - Works 100% offline with curriculum tables, sentence grammar rules, scientific glossaries, and phonetic script conversion,
 *   and also upgrades novel questions via online translation when connected.
 */
object UrduEnglishAutoTranslator {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private val leadingUrduQNumRegex = Regex(
        "^\\s*(?:[•*\\-–—]+\\s*)?(?:سوال\\s*(?:نمبر)?[\\s\\-–—.:]*[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[:.)\\-۔]*|س[\\s\\-–—.:]+[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[:.)\\-۔]*|Q(?:uestion)?[\\s\\-–—.#]*\\d+[:.)\\-]*|\\([\\d\\u06F0-\\u06F9\\u0660-\\u0669]+\\)|[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔])\\s*",
        RegexOption.IGNORE_CASE
    )

    private val latinLettersRegex = Regex("[A-Za-z]+")
    private val parensBlockRegex = Regex("\\([^)]*\\)")

    private fun isPunctuationOrWhitespace(ch: Char): Boolean {
        return ch.isWhitespace() ||
            ch == '؟' || ch == '?' || ch == '.' || ch == '!' ||
            ch == '،' || ch == ',' || ch == ':' || ch == ';' ||
            ch == '-' || ch == '–' || ch == '—' || ch == '/' ||
            ch == '\\' || ch == '(' || ch == ')' || ch == '[' ||
            ch == ']' || ch == '"' || ch == '\'' || ch == '۔'
    }

    private fun normalizeKey(text: String): String {
        val stripped = stripLeadingQuestionNumber(text)
        val sb = StringBuilder(stripped.length)
        var lastWasSpace = true
        for (rawCh in stripped) {
            val code = rawCh.code
            if (code in 0x064B..0x065F || code == 0x0670 || code in 0x06D6..0x06ED) {
                continue
            }
            val ch = when (rawCh) {
                'ۂ' -> 'ہ'
                'ة' -> 'ۃ'
                'ي' -> 'ی'
                'ك' -> 'ک'
                else -> rawCh
            }
            if (isPunctuationOrWhitespace(ch)) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(ch)
                lastWasSpace = false
            }
        }
        return sb.toString().trim()
    }

    private fun normalizeEnKey(text: String): String {
        val stripped = stripLeadingQuestionNumber(text).lowercase()
        val sb = StringBuilder(stripped.length)
        var lastWasSpace = true
        for (ch in stripped) {
            if (isPunctuationOrWhitespace(ch)) {
                if (!lastWasSpace) {
                    sb.append(' ')
                    lastWasSpace = true
                }
            } else {
                sb.append(ch)
                lastWasSpace = false
            }
        }
        return sb.toString().trim()
    }

    fun stripLeadingQuestionNumber(raw: String): String {
        var cur = raw.trim()
        repeat(3) {
            val next = cur.replaceFirst(leadingUrduQNumRegex, "").trim()
            if (next == cur) return@repeat
            cur = next
        }
        return cur
    }

    // ========================================================================
    // 1. EXACT CURRICULUM QUESTION & MCQ OPTION TRANSLATION TABLES
    // ========================================================================
    private val rawQuestionPairsUrEn: List<Pair<String, String>> = listOf(
        // Class 10 Biology Ch 1 (Gaseous Exchange & Digestive MCQs)
        "ایزائمنز کا کون سا گروپ سٹارچ اور دوسرے کاربوہائیڈریٹس کو توڑتا ہے؟" to
            "Which group of enzymes breaks down starch and other carbohydrates?",
        "پینکریاز ڈائی جیسٹو ایزائمنز بناتا ہے اور انہیں خارج کرتا ہے:" to
            "The pancreas produces digestive enzymes and releases them into the:",
        "معدے میں پیپسی نو جن کس کے عمل سے پیپسن میں تبدیل ہو جاتا ہے؟" to
            "In the stomach, pepsinogen is converted into pepsin by the action of:",
        "ڈائی جیسٹو سسٹم کے کون سے حصے میں کاربوہائیڈریٹس لپڈز اور پروٹینز کو ڈائی جیسٹ کیا جاتا ہے؟" to
            "In which part of the digestive system are carbohydrates, lipids, and proteins digested?",
        "ایزائم ٹرپسن کس رطوبت میں موجود ہوتا ہے؟" to
            "In which secretion is the enzyme trypsin present?",
        "گال بلیڈر سے بائل کامن بائل ڈکٹ کے ذریعے کہاں پہنچتا ہے؟" to
            "Where does bile from the gall bladder reach through the common bile duct?",
        "سمال انٹسٹائن میں بہت زیادہ تہیں ہوتی ہیں اور وہاں ولائی اور مائیکرو ولائی کیوں ہوتے ہیں؟" to
            "Why does the small intestine have many folds containing villi and microvilli?",
        "سیلولر ریسپیریشن کی تعریف لکھیں اور اہمیت بیان کریں۔" to
            "Define cellular respiration and state its importance.",
        "گیسوں کا تبادلہ کیا ہے؟" to
            "What is gaseous exchange?",
        "تنفس (بریدھنگ) کی تعریف لکھیں۔" to
            "Define breathing (ventilation).",
        "بریدھنگ اور ریسپیریشن میں فرق بتائیے یا بریدھنگ یعنی تنفس اور ریسپیریشن کو ہم مترادف کیوں نہیں تصور کیا جاتا؟" to
            "Differentiate between breathing and respiration, or why are breathing and respiration not considered synonymous?",
        "انسانوں اور جانوروں میں تنفس اور گیسوں کا تبادلہ کیسے ہوتا ہے؟" to
            "How do breathing and gaseous exchange take place in humans and animals?",
        "ریسپیریٹری سسٹم کی ذمہ داری لکھیں نیز یہ کتنے حصوں پر مشتمل ہوتا ہے نام لکھیں یا ریسپیریٹری سسٹم کے دو حصوں کے نام تحریر کیجیے۔" to
            "Write the function of the respiratory system and name the two main parts it consists of.",
        "ہوا کے راستے سے کیا مراد ہے؟ اس کی تعریف لکھیں۔" to
            "What is meant by the air passageway? Define it.",
        "ہوا کا راستہ کن حصوں پر مشتمل ہوتا ہے؟ نام لکھیں۔" to
            "Which parts does the air passageway consist of? Write their names.",
        "نیزل کیویٹی کیا ہے؟ اس کی اہمیت لکھیں نیزل کیویٹی کے دو افعال تحریر کیجیے۔" to
            "What is the nasal cavity? Write its importance and state two functions of the nasal cavity.",
        "نیزل کیویٹی میں موجود مایع یعنی میوکس اور باریک بالوں کا کیا فائدہ ہے یا ناک کے اندر بال اور میوکس کیا کام کرتے ہیں یا ناک میں موجود میوکس کا کیا کام ہے؟" to
            "What is the function and benefit of mucus and fine hairs present inside the nasal cavity (nose)?",
        "سانس لینا منہ سے بہتر ہے یا ناک سے؟" to
            "Is it better to breathe through the nose or the mouth?",
        "منہ سے سانس لینا کیوں مناسب نہیں سمجھا جاتا؟" to
            "Why is breathing through the mouth not considered advisable?",
        "فیرنکس کیا ہے یا ہوا کے راستے میں فیرنکس کا کیا کام ہے یا عمل تنفس میں فیرنکس کا کیا کردار ہے؟" to
            "What is the pharynx, and what is its role in the air passageway during breathing?",
        "گلاٹس اور ایپی گلاٹس میں فرق لکھیں۔" to
            "Differentiate between glottis and epiglottis.",
        "ایپی گلاٹس کیا کردار ادا کرتا ہے؟" to
            "What role does the epiglottis play?",
        "ہماری بول چال کی آواز کیسے پیدا ہوتی ہے؟" to
            "How is our speech sound (voice) produced?",
        "ٹریکیا کی تعریف لکھیں۔" to
            "Define trachea (windpipe).",
        "ٹریکیا میں میوکس اور سیلیا کیا کردار ادا کرتے ہیں؟ ٹریکیا اور برونکائی کی اندرونی دیواروں میں موجود سیلیا کا فعل تحریر کریں۔" to
            "What role do mucus and cilia play in the trachea? Write the function of cilia present in the inner walls of trachea and bronchi.",
        "ٹریکیا اور برونکائی کی دیواروں میں موجود کارٹیلج کا کیا کام ہے یا ٹریکیا میں کارٹیلج کے رنگز یا گھیرے کیوں اہم ہیں یا ٹریکیا میں کارٹیلج کے رنگز کیوں اہم ہیں ٹریکیا کیا ہے اس میں C شکل کے گھیروں کا کیا کردار ہے؟" to
            "What is the function of cartilage rings in the walls of trachea and bronchi? What is trachea and what is the role of C-shaped cartilage rings in it?",
        "ٹریکیا کی ساخت برونکائی کی ساخت سے کیسے مختلف ہے؟" to
            "How does the structure of the trachea differ from the structure of bronchi?",
        "ایلو یولر ڈکٹس اور ایلو یولائی کیا ہیں؟" to
            "What are alveolar ducts and alveoli?",
        "برونکیولز کی تعریف لکھیں۔" to
            "Define bronchioles.",
        "ایلو یولائی کیا ہیں ان کا فعل لکھیں ایلو یولس کی ساخت اور فعل لکھیے۔" to
            "What are alveoli? Describe the structure and function of an alveolus.",
        "انٹر کاسٹل مسلز سے کیا مراد ہے؟ انسانوں میں ان کی تعداد کتنی ہوتی ہے؟" to
            "What is meant by intercostal muscles, and how many pairs are present in humans?",
        "نیزل کیویٹی سے لے کر ایلو یولائی تک ہوا کا راستہ بیان کریں۔" to
            "Describe the pathway of air from the nasal cavity to the alveoli.",
        "انسانی جسم میں کتنے پھیپھڑے ہوتے ہیں اور کہاں واقع ہوتے ہیں؟" to
            "How many lungs are there in the human body and where are they located?",
        "ڈایا فرام کسے کہتے ہیں؟" to
            "What is the diaphragm?",
        "پھیپھڑوں میں موجود ممبرین کا کام اور نام لکھیں پھیپھڑے کن ممبرینز میں لپٹے ہوتے ہیں بیرونی اور اندرونی پلیورل ممبرینز کا کیا فعل ہوتا ہے پھیپھڑوں میں موجود ممبرین کا نام اور اس میں موجود فلوئڈ کا نام لکھیں یا پلیورا میں موجود پلیورل فلوئڈ کا فائدہ لکھیں۔" to
            "Write the name and function of the membranes enclosing the lungs (outer and inner pleural membranes) and state the benefit of pleural fluid.",
        "تنفس کی تعریف لکھیں اور اس کے مراحل کے نام لکھیں۔" to
            "Define breathing and write the names of its two phases.",
        "ریسپیریشن گیسوں کا تبادلہ اور تنفس کے درمیان تعلق ڈایا گرام کی مدد سے واضح کریں۔" to
            "Explain the relationship between cellular respiration, gaseous exchange, and breathing with the help of a diagram.",
        "لیرنکس کو وائس باکس آلہ صوت کیوں کہا جاتا ہے یا ہماری بول چال کی آواز کیسے پیدا ہوتی ہے یا لیرنکس کیا ہے اس میں آواز کیسے پیدا ہوتی ہے یا ووکل کارڈز کیا ہیں ان کا کیا فعل ہے یا ووکل کارڈز سے آواز کیسے پیدا ہوتی ہے لیرنکس کی ساخت اور فعل تحریر کیجیے۔" to
            "Why is the larynx called the voice box? Describe the structure and function of the larynx and explain how vocal cords produce sound.",
        "ٹریکیا کی ساخت کیبل شدہ ڈایا گرام کی مدد سے واضح کریں۔" to
            "Explain the structure of the trachea with the help of a labelled diagram.",
        "ٹریکیا کی ساخت لیبل شدہ ڈایا گرام کی مدد سے واضح کریں۔" to
            "Explain the structure of the trachea with the help of a labelled diagram.",
        "پھیپھڑوں کی ساخت بیان کریں دائیں اور بائیں پھیپھڑے میں فرق بیان کریں۔" to
            "Describe the structure of human lungs and differentiate between the right and left lung.",
        "پھیپھڑوں میں خون کی گردش مختصر الفاظ میں بیان کریں۔" to
            "Briefly describe the pulmonary blood circulation in the lungs.",
        "انہیلیشن سے کیا مراد ہے؟ اس عمل میں ہونے والی تبدیلیوں کی مختصر وضاحت لکھیں۔" to
            "What is meant by inhalation (inspiration)? Explain the changes that take place during this process.",
        "ایگز ہیلیشن سے کیا مراد ہے؟ اس عمل میں ہونے والی تبدیلیوں کی مختصر وضاحت کریں۔" to
            "What is meant by exhalation (expiration)? Explain the changes that take place during this process.",
        "نارمل حالات اور ورزش کے دوران سانس کی رفتار کا موازنہ کیجیے۔" to
            "Compare the rate of breathing under normal conditions and during exercise.",

        // Class 9 Tarjuma-tul-Quran Ch 1 (Surah Maryam)
        "حبشہ کے بادشاہ کو کہا جاتا تھا:" to
            "The King of Abyssinia (Habsha) was called:",
        "شاہ حبشہ کے دربار میں تلاوت کی گئی:" to
            "Which Surah was recited in the court of the King of Abyssinia?",
        "نجاشی کے دربار میں سورۃ مریم کی تلاوت کی:" to
            "Who recited Surah Maryam in the court of Najashi?",
        "سورۃ مریم میں انبیاء کرام علیہ السلام کا ذکر ہے:" to
            "How many Prophets (A.S) are mentioned in Surah Maryam?",
        "معجزانہ طریقے سے تخلیق ہوئی:" to
            "Whose birth took place in a miraculous manner?",
        "سورۃ مریم کو یہ نام کیوں دیا گیا؟" to
            "Why was Surah Maryam given this name?",
        "سورۃ مریم کا خلاصہ لکھیں۔" to
            "Write the summary of Surah Maryam.",
        "سورۃ مریم کے اہم علمی و عملی نکات میں سے دو تحریر کیجیے۔" to
            "Write any two important academic and practical points of Surah Maryam.",
        "ہجرت حبشہ کا سبب کیا تھا؟" to
            "What was the cause of the migration to Abyssinia (Hijrat-e-Habsha)?",
        "سورۃ مریم کی تلاوت سن کر نجاشی نے کیا کہا؟" to
            "What did Najashi say after listening to the recitation of Surah Maryam?",
        "سورۃ مریم پر ایک مفصل نوٹ تحریر کریں۔" to
            "Write a comprehensive note on Surah Maryam.",
        "ٹریکیا کی لمبائی تقریباً کتنی ہوتی ہے؟" to
            "What is the approximate length of the trachea?",
        "ٹریکیا کی لمبائی کتنی ہوتی ہے؟" to
            "What is the length of the trachea?",
        "سلیولر ریسپائریشن اور تنفس میں کیا فرق ہے؟" to
            "What is the difference between cellular respiration and breathing?",
        "سٹومیٹا اور لینٹی سیلز میں کیا فرق ہے؟" to
            "What is the difference between stomata and lenticels?",
        "انڈسٹریل کیمسٹری کی تعریف کریں۔" to
            "Define Industrial Chemistry.",
        "ایووگیڈروز نمبر کیا ہے؟" to
            "What is Avogadro's Number?"
    )

    private val exactQuestionsUrToEn: Map<String, String> by lazy {
        buildMap {
            for ((k, v) in rawQuestionPairsUrEn) {
                put(normalizeKey(k), v)
                val withoutLatin = normalizeKey(k.replace(latinLettersRegex, " "))
                if (withoutLatin.isNotBlank()) {
                    put(withoutLatin, v)
                }
            }
        }
    }

    private val exactQuestionsEnToUr: Map<String, String> by lazy {
        buildMap {
            for ((ur, en) in rawQuestionPairsUrEn) {
                val cleanUr = BulkQuestionParser.stripEnglishGlossFromUrdu(ur)
                val normEn = normalizeEnKey(en)
                if (normEn.isNotBlank() && !containsKey(normEn)) {
                    put(normEn, cleanUr)
                }
                val withoutParens = normalizeEnKey(en.replace(parensBlockRegex, " "))
                if (withoutParens.isNotBlank() && !containsKey(withoutParens)) {
                    put(withoutParens, cleanUr)
                }
            }
        }
    }

    private val rawOptionPairsUrEn: List<Pair<String, String>> = listOf(
        // Biology / Chemistry / Physics Options
        "پروٹیئز" to "Protease",
        "لائی پیز" to "Lipase",
        "ایمائی لایز" to "Amylase",
        "اماے لیز" to "Amylase",
        "پیپسن" to "Pepsin",
        "ٹرپسن" to "Trypsin",
        "کولون میں" to "In the colon",
        "کولون" to "Colon",
        "گال بلیڈر میں" to "In the gall bladder",
        "گال بلیڈر" to "Gall bladder",
        "جگر میں" to "In the liver",
        "جگر" to "Liver",
        "ڈیوڈینم میں" to "In the duodenum",
        "ڈیوڈینم" to "Duodenum",
        "بائل نمکیات" to "Bile salts",
        "ہارمونز" to "Hormones",
        "بائی کاربونیٹ" to "Bicarbonate",
        "اورل کیویٹی" to "Oral cavity",
        "معدہ" to "Stomach",
        "سمال انٹسٹائن" to "Small intestine",
        "لارج انٹسٹائن" to "Large intestine",
        "پینکریاٹک جوس" to "Pancreatic juice",
        "بائل" to "Bile",
        "گیسٹرک جوس" to "Gastric juice",
        "سیلائیوا" to "Saliva",
        "پینکریاز" to "Pancreas",
        "خوراک کے گزرنے کی رفتار کو کم کرنے کے لیے" to "To slow down the movement of food",
        "خوراک کا انجذاب تیز کرنے کے لیے" to "To speed up the absorption of food",
        "کائم کے پیچھے کی طرف بہاؤ کو روکنے کے لیے" to "To prevent the backward flow of chyme",
        "ایزائمنز پیدا کرنے کے لیے" to "To produce enzymes",
        "میٹر رول" to "Metre rule",
        "پیمائشی فیتہ" to "Measuring tape",
        "ورنیئر کیلیپرز" to "Vernier Callipers",
        "مائیکرومیٹر سکرو گیج" to "Micrometer screw gauge",
        "روشنی کا" to "Light",
        "وقت کا" to "Time",
        "فاصلے کا" to "Distance",
        "فاصلہ" to "Distance",
        "سپیڈ کا" to "Speed",
        "کثافت" to "Density",
        "کثافت ڈینسٹی" to "Density",
        "رنگ" to "Colour",
        "ٹمپریچر" to "Temperature",
        "ملی لیٹر" to "Millilitre",
        "لیٹر" to "Litre",
        "کلو گرام" to "Kilogram",
        "مکعب میٹر" to "Cubic metre",
        "مائع کا ماس" to "Mass of a liquid",
        "ٹھوس کا ماس" to "Mass of a solid",
        "مائع کا والیم" to "Volume of a liquid",
        "ٹھوس کا والیم" to "Volume of a solid",
        "زیرو ایرر چیک کریں" to "Check for the zero error",
        "منسکیس کو سطح کے نیچے سے دیکھیں" to "Look at the meniscus from below the level of the water surface",
        "ایک سے زیادہ سمتوں سے دیکھیں" to "Take several readings by looking from more than one direction",
        "آنکھ کو منسکیس کے نچلے حصے کی سیدھ میں رکھیں" to "Position the eye in line with the bottom of the meniscus",

        // Quran / Islamiat Options
        "نجاشی" to "Najashi (Negus)",
        "قیصر" to "Qaisar (Caesar)",
        "کسری" to "Kisra (Chosroes)",
        "کسریٰ" to "Kisra (Chosroes)",
        "عزیز" to "Aziz",
        "سورۃ البقرہ" to "Surah Al-Baqarah",
        "سورۃ آل عمران" to "Surah Aal-e-Imran",
        "سورۃ مریم" to "Surah Maryam",
        "سورۃ یسین" to "Surah Yaseen",
        "سورۃ یٰسین" to "Surah Yaseen",
        "حضرت ابوبکر صدیق رضی اللہ عنہ نے" to "Hazrat Abu Bakr Siddiq (R.A)",
        "حضرت ابوبکر صدیق رضی اللہ عنہ" to "Hazrat Abu Bakr Siddiq (R.A)",
        "حضرت جعفر طیار رضی اللہ عنہ نے" to "Hazrat Ja'far Tayyar (R.A)",
        "حضرت جعفر طیار رضی اللہ عنہ" to "Hazrat Ja'far Tayyar (R.A)",
        "حضرت علی رضی اللہ عنہ نے" to "Hazrat Ali (R.A)",
        "حضرت علی رضی اللہ عنہ" to "Hazrat Ali (R.A)",
        "حضرت عثمان غنی رضی اللہ عنہ نے" to "Hazrat Usman Ghani (R.A)",
        "حضرت عثمان غنی رضی اللہ عنہ" to "Hazrat Usman Ghani (R.A)",
        "حضرت موسی علیہ السلام کی" to "Hazrat Musa (A.S)",
        "حضرت موسیٰ علیہ السلام کی" to "Hazrat Musa (A.S)",
        "حضرت عیسی علیہ السلام کی" to "Hazrat Isa (A.S)",
        "حضرت عیسیٰ علیہ السلام کی" to "Hazrat Isa (A.S)",
        "حضرت ابراہیم علیہ السلام کی" to "Hazrat Ibrahim (A.S)",
        "حضرت نوح علیہ السلام کی" to "Hazrat Nuh (A.S)",
        "پانچ" to "Five (5)",
        "چھ" to "Six (6)",
        "سات" to "Seven (7)",
        "آٹھ" to "Eight (8)",
        "ایک" to "One (1)",
        "دو" to "Two (2)",
        "تین" to "Three (3)",
        "چار" to "Four (4)",
        "دس" to "Ten (10)"
    )

    private val exactOptionsUrToEn: Map<String, String> by lazy {
        rawOptionPairsUrEn.associate { (k, v) -> normalizeKey(k) to v }
    }

    private val exactOptionsEnToUr: Map<String, String> by lazy {
        buildMap {
            for ((ur, en) in rawOptionPairsUrEn) {
                val normEn = normalizeEnKey(en)
                if (normEn.isNotBlank() && !containsKey(normEn)) {
                    put(normEn, ur)
                }
                val withoutParens = normalizeEnKey(en.replace(parensBlockRegex, " "))
                if (withoutParens.isNotBlank() && !containsKey(withoutParens)) {
                    put(withoutParens, ur)
                }
            }
        }
    }

    // ========================================================================
    // 2. ACADEMIC VOCABULARY & PHRASE TABLE FOR RULE-BASED OFFLINE TRANSLATION
    // ========================================================================
    private val phraseGlossaryUrToEn = listOf(
        "سیلولر ریسپیریشن" to "cellular respiration",
        "سلیولر ریسپائریشن" to "cellular respiration",
        "سیلولر ریسپائریشن" to "cellular respiration",
        "سلیولر ریسپیریشن" to "cellular respiration",
        "ایروبک ریسپیریشن" to "aerobic respiration",
        "ایروبک ریسپائریشن" to "aerobic respiration",
        "این ایروبک ریسپیریشن" to "anaerobic respiration",
        "این ایروبک ریسپائریشن" to "anaerobic respiration",
        "گیسوں کا تبادلہ" to "gaseous exchange",
        "گیسوں کے تبادلے" to "gaseous exchange",
        "ریسپیریٹری سسٹم" to "respiratory system",
        "ریسپائریٹری سسٹم" to "respiratory system",
        "ڈائی جیسٹو سسٹم" to "digestive system",
        "نیزل کیویٹی" to "nasal cavity",
        "اورل کیویٹی" to "oral cavity",
        "ایلو یولر ڈکٹس" to "alveolar ducts",
        "ایلو یولائی" to "alveoli",
        "ایلو یولس" to "alveolus",
        "انٹر کاسٹل مسلز" to "intercostal muscles",
        "پلیورل ممبرینز" to "pleural membranes",
        "پلیورل فلوئڈ" to "pleural fluid",
        "ووکل کارڈز" to "vocal cords",
        "وائس باکس" to "voice box",
        "آلہ صوت" to "voice box",
        "ایپی گلاٹس" to "epiglottis",
        "برونکیولز" to "bronchioles",
        "برونکائی" to "bronchi",
        "ٹریکیا" to "trachea",
        "فیرنکس" to "pharynx",
        "لیرنکس" to "larynx",
        "گلاٹس" to "glottis",
        "ڈایا فرام" to "diaphragm",
        "ڈایا گرام" to "diagram",
        "لیبل شدہ" to "labelled",
        "کیبل شدہ" to "labelled",
        "کارٹیلج" to "cartilage",
        "سیلیا" to "cilia",
        "میوکس" to "mucus",
        "پھیپھڑوں" to "lungs",
        "پھیپھڑے" to "lungs",
        "انہیلیشن" to "inhalation",
        "ایگز ہیلیشن" to "exhalation",
        "بریدھنگ" to "breathing",
        "ریسپیریشن" to "respiration",
        "ریسپائریشن" to "respiration",
        "عمل تنفس" to "breathing process",
        "تنفس" to "breathing",
        "سانس لینا" to "breathing",
        "سانس کی رفتار" to "breathing rate",
        "ہوا کا راستہ" to "air passageway",
        "ہوا کے راستے" to "air passageway",
        "خون کی گردش" to "blood circulation",
        "کاربوہائیڈریٹس" to "carbohydrates",
        "پروٹینز" to "proteins",
        "لپڈز" to "lipids",
        "ایزائمنز" to "enzymes",
        "ایزائم" to "enzyme",
        "انزائمز" to "enzymes",
        "انزائم" to "enzyme",
        "سٹوماٹا" to "stomata",
        "سٹومیٹا" to "stomata",
        "لینٹی سیلز" to "lenticels",
        "کیوٹیکل" to "cuticle",
        "میسوفل" to "mesophyll",
        "فوٹو سنتھیسز" to "photosynthesis",
        "ہومیو سٹیسز" to "homeostasis",
        "اوسمو ریگولیشن" to "osmoregulation",
        "تھرمو ریگولیشن" to "thermoregulation",
        "ایکسکریشن" to "excretion",
        "سینٹی میٹر" to "cm",
        "ملی میٹر" to "mm",
        "کلو میٹر" to "km",
        "کلو گرام" to "kg",
        "ملی لیٹر" to "mL",
        "مکعب میٹر" to "m³",
        "گردے" to "kidneys",
        "نیفرون" to "nephron",
        "ڈائلیسز" to "dialysis",
        "برونکائٹس" to "bronchitis",
        "ایمفیسیما" to "emphysema",
        "نمونیا" to "pneumonia",
        "دمہ" to "asthma",
        "پھیپھڑوں کا کینسر" to "lung cancer",
        "تمباکو نوشی" to "smoking",
        "سورۃ مریم" to "Surah Maryam",
        "سُوْرَةُ مَرْيَمَ" to "Surah Maryam",
        "ہجرت حبشہ" to "Migration to Abyssinia (Hijrat-e-Habsha)",
        "شاہ حبشہ" to "King of Abyssinia",
        "حبشہ" to "Abyssinia",
        "نجاشی" to "Najashi",
        "انبیاء کرام علیہ السلام" to "Prophets (A.S)",
        "حضرت عیسیٰ علیہ السلام" to "Hazrat Isa (A.S)",
        "حضرت موسیٰ علیہ السلام" to "Hazrat Musa (A.S)",
        "حضرت ابراہیم علیہ السلام" to "Hazrat Ibrahim (A.S)",
        "حضرت نوح علیہ السلام" to "Hazrat Nuh (A.S)",
        "حضرت جعفر طیار رضی اللہ عنہ" to "Hazrat Ja'far Tayyar (R.A)",
        "حضرت ابوبکر صدیق رضی اللہ عنہ" to "Hazrat Abu Bakr Siddiq (R.A)",
        "حضرت علی رضی اللہ عنہ" to "Hazrat Ali (R.A)",
        "حضرت عثمان غنی رضی اللہ عنہ" to "Hazrat Usman Ghani (R.A)",
        "علمی و عملی نکات" to "academic and practical points",
        "مرکزی خیال" to "central idea",
        "خلاصہ" to "summary",
        "طبیعی مقداریں" to "physical quantities",
        "بنیادی مقداریں" to "base quantities",
        "ماخوذ مقداریں" to "derived quantities",
        "بنیادی یونٹس" to "base units",
        "ماخوذ یونٹس" to "derived units",
        "سائنسی ترقیم" to "scientific notation",
        "پریفکسز" to "prefixes",
        "ورنیئر کیلیپرز" to "Vernier Callipers",
        "سکرو گیج" to "screw gauge",
        "زیرو ایرر" to "zero error",
        "لیسٹ کاؤنٹ" to "least count",
        "نمایاں ہندسے" to "significant figures",
        "سکیلر" to "scalar",
        "ویکٹر" to "vector",
        "سپیڈ" to "speed",
        "ویلاسٹی" to "velocity",
        "ایکسلریشن" to "acceleration",
        "مومینٹم" to "momentum",
        "نیوٹن کے قوانین حرکت" to "Newton's laws of motion",
        "رگڑ" to "friction",
        "ٹارک" to "torque",
        "سینٹر آف ماس" to "centre of mass",
        "گریوی ٹیشن" to "gravitation",
        "ورک" to "work",
        "انرجی" to "energy",
        "پاور" to "power",
        "کائنیٹک انرجی" to "kinetic energy",
        "پوٹینشل انرجی" to "potential energy",
        "ہک کا قانون" to "Hooke's law",
        "پریشر" to "pressure",
        "آرشمیدس کا اصول" to "Archimedes' principle",
        "پاسکل کا قانون" to "Pascal's law",
        "حرارت" to "heat",
        "ٹمپریچر" to "temperature",
        "مخصوص حرارتی گنجائش" to "specific heat capacity",
        "حرارت مخفی" to "latent heat",
        "سمپل ہارمونک موشن" to "simple harmonic motion",
        "فریکوئنسی" to "frequency",
        "ویو لینتھ" to "wavelength",
        "ٹائم پیریڈ" to "time period",
        "کولمب کا قانون" to "Coulomb's law",
        "اوہم کا قانون" to "Ohm's law",
        "کپیسٹر" to "capacitor",
        "ٹرانسفارمر" to "transformer",
        "آئسوٹوپس" to "isotopes",
        "الیکٹرانک کنفگریشن" to "electronic configuration",
        "آئنائزیشن انرجی" to "ionization energy",
        "الیکٹرو نیگیٹیوٹی" to "electronegativity",
        "الیکٹران ایفینیٹی" to "electron affinity",
        "کوویلنٹ بانڈ" to "covalent bond",
        "آئنک بانڈ" to "ionic bond",
        "ہائیڈروجن بانڈنگ" to "hydrogen bonding",
        "بوائل کا قانون" to "Boyle's law",
        "چارلس کا قانون" to "Charles's law",
        "ایووگیڈروز نمبر" to "Avogadro's number",
        "انڈسٹریل کیمسٹری" to "industrial chemistry",
        "آرگینک کیمسٹری" to "organic chemistry",
        "ان آرگینک کیمسٹری" to "inorganic chemistry",
        "فزیکل کیمسٹری" to "physical chemistry",
        "بائیو کیمسٹری" to "biochemistry",
        "نیوکلیئر کیمسٹری" to "nuclear chemistry",
        "انوائرنمنٹل کیمسٹری" to "environmental chemistry",
        "اینالیٹیکل کیمسٹری" to "analytical chemistry",
        "مولر ماس" to "molar mass",
        "مالیکیولر ماس" to "molecular mass",
        "فارمولا ماس" to "formula mass",
        "اٹامک نمبر" to "atomic number",
        "ماس نمبر" to "mass number",
        "ریلیٹو اٹامک ماس" to "relative atomic mass",
        "کیمیائی فارمولا" to "chemical formula",
        "ایمپیریکل فارمولا" to "empirical formula",
        "مالیکیولر فارمولا" to "molecular formula",
        "آزاد ریڈیکل" to "free radical",
        "مول" to "mole",
        "مولیریٹی" to "molarity",
        "سولوبیلٹی" to "solubility",
        "آکسیڈیشن" to "oxidation",
        "ریڈکشن" to "reduction",
        "الیکٹرولائٹس" to "electrolytes",
        "کیمیکل ایکوی لبریم" to "chemical equilibrium",
        "لا آف ماس ایکشن" to "Law of Mass Action",
        "ایسڈ" to "acid",
        "بیس" to "base",
        "سالٹس" to "salts",
        "نیوٹرلائزیشن" to "neutralization",
        "ہائیڈرو کاربنز" to "hydrocarbons",
        "فنکشنل گروپ" to "functional group",
        "ہومولوگس سیریز" to "homologous series",
        "وٹامنز" to "vitamins",
        "گرین ہاؤس ایفیکٹ" to "greenhouse effect",
        "ایسڈ رین" to "acid rain",
        "ہارڈ واٹر" to "hard water",
        "سوفٹ واٹر" to "soft water"
    )

    private val phraseGlossaryEnToUr: List<Pair<String, String>> by lazy {
        phraseGlossaryUrToEn
            .map { (ur, en) -> en.lowercase() to ur }
            .distinctBy { it.first }
            .sortedByDescending { it.first.length }
    }

    private val wordGlossaryUrToEn = mapOf(
        "تعریف" to "definition",
        "اہمیت" to "importance",
        "ساخت" to "structure",
        "فعل" to "function",
        "افعال" to "functions",
        "کردار" to "role",
        "کام" to "function",
        "فائدہ" to "benefit",
        "فوائد" to "benefits",
        "فرق" to "difference",
        "موازنہ" to "comparison",
        "مراحل" to "stages",
        "حصوں" to "parts",
        "حصے" to "parts",
        "نام" to "names",
        "وجہ" to "reason",
        "سبب" to "cause",
        "وجوہات" to "causes",
        "علامات" to "symptoms",
        "علاج" to "treatment",
        "احتیاط" to "precaution",
        "مثال" to "example",
        "مثالیں" to "examples",
        "خصوصیات" to "characteristics",
        "اصول" to "principle",
        "قانون" to "law",
        "فارمولا" to "formula",
        "مساوات" to "equation",
        "اکائی" to "unit",
        "یونٹ" to "unit",
        "پودوں" to "plants",
        "پودے" to "plants",
        "جانوروں" to "animals",
        "انسانوں" to "humans",
        "انسانی" to "human",
        "جسم" to "body",
        "خلیہ" to "cell",
        "سیل" to "cell",
        "خون" to "blood",
        "ہوا" to "air",
        "پانی" to "water",
        "خوراک" to "food",
        "آواز" to "sound",
        "ناک" to "nose",
        "منہ" to "mouth",
        "بال" to "hairs",
        "بالوں" to "hairs",
        "تعداد" to "number",
        "مقدار" to "quantity",
        "تبدیلیوں" to "changes",
        "وضاحت" to "explanation",
        "مختصر" to "brief",
        "مفصل" to "detailed",
        "تفصیلی" to "detailed",
        "نوٹ" to "note",
        "دائیں" to "right",
        "بائیں" to "left",
        "اندرونی" to "inner",
        "بیرونی" to "outer",
        "دیواروں" to "walls",
        "رفتار" to "rate",
        "نارمل" to "normal",
        "حالات" to "conditions",
        "ورزش" to "exercise",
        "دوران" to "during"
    )

    private val wordGlossaryEnToUr: Map<String, String> by lazy {
        val base = mutableMapOf<String, String>()
        for ((ur, en) in wordGlossaryUrToEn) {
            base.putIfAbsent(en.lowercase(), ur)
        }
        val extra = mapOf(
            "atom" to "ایٹم",
            "atoms" to "ایٹمز",
            "element" to "ایلیمنٹ",
            "elements" to "ایلیمنٹس",
            "compound" to "کمپاؤنڈ",
            "compounds" to "کمپاؤنڈز",
            "mixture" to "مکسچر",
            "mixtures" to "مکسچرز",
            "molecule" to "مالیکیول",
            "molecules" to "مالیکیولز",
            "ion" to "آئن",
            "ions" to "آئنز",
            "electron" to "الیکٹران",
            "electrons" to "الیکٹرانز",
            "proton" to "پروٹون",
            "protons" to "پروٹونز",
            "neutron" to "نیوٹران",
            "neutrons" to "نیوٹرانز",
            "nucleus" to "نیوکلیس",
            "shell" to "شیل",
            "subshell" to "سب شیل",
            "valency" to "ویلنسی",
            "metal" to "دھات",
            "metals" to "دھاتیں",
            "nonmetal" to "غیر دھات",
            "nonmetals" to "غیر دھاتیں",
            "gas" to "گیس",
            "gases" to "گیسیں",
            "liquid" to "مائع",
            "solid" to "ٹھوس",
            "solution" to "سلوشن",
            "solute" to "سولیوٹ",
            "solvent" to "سالوینٹ",
            "force" to "فورس",
            "mass" to "ماس",
            "weight" to "وزن",
            "length" to "لمبائی",
            "time" to "وقت",
            "area" to "رقبہ",
            "volume" to "والیم",
            "density" to "کثافت",
            "motion" to "حرکت",
            "rest" to "سکون",
            "distance" to "فاصلہ",
            "displacement" to "ڈسپلیسمنٹ",
            "gravity" to "کشش ثقل",
            "current" to "کرنٹ",
            "voltage" to "وولٹیج",
            "resistance" to "مزاحمت",
            "charge" to "چارج",
            "field" to "فیلڈ",
            "wave" to "لہر",
            "waves" to "لہریں",
            "light" to "روشنی",
            "lens" to "لینز",
            "mirror" to "آئینہ",
            "heart" to "دل",
            "brain" to "دماغ",
            "lung" to "پھیپھڑا",
            "lungs" to "پھیپھڑے",
            "kidney" to "گردہ",
            "kidneys" to "گردے",
            "bone" to "ہڈی",
            "bones" to "ہڈیاں",
            "muscle" to "مسل",
            "muscles" to "مسلز",
            "tissue" to "ٹشو",
            "tissues" to "ٹشوز",
            "organ" to "عضو",
            "organs" to "اعضاء",
            "blood" to "خون",
            "oxygen" to "آکسیجن",
            "carbon" to "کاربن",
            "dioxide" to "ڈائی آکسائیڈ",
            "hydrogen" to "ہائیڈروجن",
            "nitrogen" to "نائٹروجن",
            "glucose" to "گلوکوز",
            "starch" to "سٹارچ",
            "windpipe" to "ہوا کی نالی",
            "gullet" to "خوراک کی نالی",
            "ventilation" to "تنفس",
            "inspiration" to "انہیلیشن",
            "expiration" to "ایگز ہیلیشن",
            "computer" to "کمپیوٹر",
            "hardware" to "ہارڈویئر",
            "software" to "سافٹ ویئر",
            "network" to "نیٹ ورک",
            "algorithm" to "الگورتھم",
            "flowchart" to "فلو چارٹ",
            "variable" to "ویری ایبل",
            "data" to "ڈیٹا",
            "memory" to "میموری",
            "matrix" to "کالب",
            "matrices" to "کالب",
            "logarithm" to "لوگارتھم",
            "theorem" to "مسئلہ",
            "triangle" to "مثلث",
            "circle" to "دائرہ",
            "ratio" to "نسبت",
            "proportion" to "تناسب",
            "quadratic" to "دو درجی",
            "set" to "سیٹ",
            "sets" to "سیٹس",
            "two" to "دو",
            "three" to "تین",
            "four" to "چار",
            "five" to "پانچ",
            "uses" to "استعمالات",
            "use" to "استعمال",
            "properties" to "خصوصیات",
            "property" to "خصوصیت",
            "types" to "اقسام",
            "type" to "قسم",
            "advantages" to "فوائد",
            "advantage" to "فائدہ"
        )
        base.putAll(extra)
        base
    }

    private val unitEnToUrMap = mapOf(
        "cm" to "سینٹی میٹر",
        "mm" to "ملی میٹر",
        "km" to "کلو میٹر",
        "m" to "میٹر",
        "kg" to "کلو گرام",
        "g" to "گرام",
        "mg" to "ملی گرام",
        "ml" to "ملی لیٹر",
        "l" to "لیٹر",
        "s" to "سیکنڈ",
        "ms" to "ملی سیکنڈ",
        "n" to "نیوٹن",
        "j" to "جول",
        "kj" to "کلو جول",
        "w" to "واٹ",
        "kw" to "کلو واٹ",
        "pa" to "پاسکل",
        "hz" to "ہرٹز",
        "v" to "وولٹ",
        "a" to "ایمپیئر",
        "g/mol" to "گرام فی مول",
        "m/s" to "میٹر فی سیکنڈ",
        "m/s2" to "میٹر فی مربع سیکنڈ",
        "m³" to "مکعب میٹر",
        "cm³" to "مکعب سینٹی میٹر"
    )

    // ========================================================================
    // 3. URDU -> ENGLISH OFFLINE TRANSLATION
    // ========================================================================

    /**
     * Synchronously translates an Urdu question or MCQ option into English.
     * Works 100% offline with zero latency and guarantees Latin/English output.
     */
    fun translateUrduToEnglishOffline(rawUrdu: String, isOption: Boolean = false): String {
        val cleaned = if (isOption) {
            BulkQuestionParser.cleanOptionPrefix(rawUrdu)
        } else {
            stripLeadingQuestionNumber(rawUrdu)
        }.trim()

        if (cleaned.isBlank()) return ""
        if (!BulkQuestionParser.containsUrdu(cleaned)) return cleaned

        val normKey = normalizeKey(cleaned)
        if (isOption) {
            exactOptionsUrToEn[normKey]?.let { return it }
            exactQuestionsUrToEn[normKey]?.let { return it }
        } else {
            exactQuestionsUrToEn[normKey]?.let { return it }
            exactOptionsUrToEn[normKey]?.let { return it }
        }

        if (isOption) {
            return translateUrduClauseOrTermToEnglish(cleaned, isQuestion = false)
        }

        return translateUrduSentenceRuleBased(cleaned)
    }

    private fun translateUrduSentenceRuleBased(urdu: String): String {
        val s = stripLeadingQuestionNumber(urdu)
            .removeSuffix("۔")
            .removeSuffix("؟")
            .removeSuffix("?")
            .removeSuffix(".")
            .trim()

        // 1. "X کی تعریف لکھیں اور اہمیت بیان کریں"
        Regex("^(.+?)\\s+کی\\s+تعریف\\s+(?:لکھیں|کریں|تحریر کریں)\\s+اور\\s+(?:اس کی\\s+)?اہمیت\\s+(?:بیان کریں|لکھیں).*$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Define $topic and state its importance."
            }

        // 2. "X کی تعریف لکھیں اور اس کے مراحل کے نام لکھیں"
        Regex("^(.+?)\\s+کی\\s+تعریف\\s+(?:لکھیں|کریں)\\s+اور\\s+(.+?)\\s+کے\\s+نام\\s+لکھیں.*$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                val sub = translateUrduClauseOrTermToEnglish(m.groupValues[2], isQuestion = false)
                return "Define $topic and write the names of $sub."
            }

        // 3. "X اور Y میں فرق لکھیں / بیان کریں / بتائیے / کیا فرق ہے"
        Regex("^(.+?)\\s+اور\\s+(.+?)\\s+(?:میں|کے درمیان)\\s+(کیا\\s+)?فرق\\s+(?:ہے|لکھیں|بیان کریں|واضح کریں|تحریر کریں|بتائیے).*$")
            .find(s)?.let { m ->
                val t1 = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false).lowercase()
                val t2 = translateUrduClauseOrTermToEnglish(m.groupValues[2].substringBefore("یا").trim(), isQuestion = false).lowercase()
                return if (m.groupValues[3].isNotBlank() || s.contains("کیا فرق ہے")) {
                    "What is the difference between $t1 and $t2?"
                } else {
                    "Differentiate between $t1 and $t2."
                }
            }

        // 4. "X کی تعریف لکھیں / کریں"
        Regex("^(.+?)\\s+کی\\s+تعریف\\s*(?:لکھیں|کریں|تحریر کریں|کیجیے|بیان کریں)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Define $topic."
            }

        // 5. "X سے کیا مراد ہے؟ ..."
        Regex("^(.+?)\\s+سے\\s+کیا\\s+مراد\\s+ہے[؟?.۔]?\\s*(.*)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                val rest = m.groupValues[2].trim()
                return if (rest.isNotBlank()) {
                    val restEn = translateUrduClauseOrTermToEnglish(rest, isQuestion = true)
                    "What is meant by $topic? $restEn"
                } else {
                    "What is meant by $topic?"
                }
            }

        // 6. "X کسے کہتے ہیں"
        Regex("^(.+?)\\s+کسے\\s+کہتے\\s+ہیں$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "What is $topic?"
            }

        // 7. "X کا خلاصہ لکھیں"
        Regex("^(.+?)\\s+کا\\s+خلاصہ\\s+(?:لکھیں|تحریر کریں|بیان کریں)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Write the summary of $topic."
            }

        // 8. "X پر ایک مفصل نوٹ تحریر کریں"
        Regex("^(.+?)\\s+پر\\s+(?:ایک\\s+)?(?:مفصل|جامع|تفصیلی)?\\s*نوٹ\\s+(?:لکھیں|تحریر کریں|لکھیے)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Write a detailed note on $topic."
            }

        // 9. "X کی ساخت اور فعل تحریر کیجیے / لکھیں"
        Regex("^(.+?)\\s+کی\\s+ساخت\\s+اور\\s+فعل\\s+(?:لکھیں|لکھیے|تحریر کریں|تحریر کیجیے|بیان کریں)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Describe the structure and function of $topic."
            }

        // 10. "X کی ساخت ... ڈایا گرام کی مدد سے واضح کریں"
        Regex("^(.+?)\\s+کی\\s+ساخت.*ڈایا\\s*گرام\\s+کی\\s+مدد\\s+سے\\s+واضح\\s+کریں$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Explain the structure of $topic with the help of a labelled diagram."
            }

        // 11. "X کی ساخت بیان کریں"
        Regex("^(.+?)\\s+کی\\s+ساخت\\s+(?:بیان کریں|واضح کریں|لکھیں)(.*)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Describe the structure of $topic."
            }

        // 12. "X کا موازنہ کیجیے"
        Regex("^(.+?)\\s+کا\\s+موازنہ\\s+(?:کریں|کیجیے)$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "Compare $topic."
            }

        // 13. "X کیا کردار ادا کرتا ہے" / "X کا کیا کام ہے"
        Regex("^(.+?)\\s+(?:کیا\\s+کردار\\s+ادا\\s+کرت[اےی]\\s+ہے|کا\\s+کیا\\s+(?:کام|کردار|فائدہ|فعل)\\s+ہے).*$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                return "What is the function and role of $topic?"
            }

        // 14. "X کیا ہے / کیا ہیں"
        Regex("^(.+?)\\s+کیا\\s+(?:ہے|ہیں)(?:[؟?.۔]\\s*(.*))?$")
            .find(s)?.let { m ->
                val topic = translateUrduClauseOrTermToEnglish(m.groupValues[1], isQuestion = false)
                val isPlural = s.contains("کیا ہیں")
                val verb = if (isPlural) "What are" else "What is"
                return "$verb $topic?"
            }

        return translateUrduClauseOrTermToEnglish(s, isQuestion = true)
    }

    private fun translateUrduClauseOrTermToEnglish(raw: String, isQuestion: Boolean): String {
        val parenEnglish = Regex("\\(([A-Za-z][A-Za-z0-9\\s\\-,/]+)\\)").find(raw)?.groupValues?.get(1)?.trim()

        var working = raw
        for ((urPhrase, enPhrase) in phraseGlossaryUrToEn) {
            if (working.contains(urPhrase)) {
                working = working.replace(urPhrase, " $enPhrase ")
            }
        }

        working = working.replace(Regex("\\(([A-Za-z][A-Za-z0-9\\s\\-,/]+)\\)"), " $1 ")

        val stopWords = setOf(
            "کی", "کا", "کے", "کو", "میں", "سے", "پر", "اور", "یا", "نے", "تو",
            "ہے", "ہیں", "تھا", "تھی", "تھے", "ہوتا", "ہوتی", "ہوتے", "جا", "جاتا",
            "لکھیں", "لکھیے", "تحریر", "کریں", "کیجیے", "بیان", "واضح", "مختصر",
            "مندرجہ", "ذیل", "درج", "شکل", "مدد", "ساتھ", "لیے", "طور", "کس", "کون", "کیا"
        )

        val tokens = working.split(Regex("[\\s،,۔؟?.!:/\\-]+")).filter { it.isNotBlank() }
        val outTokens = mutableListOf<String>()

        for (tok in tokens) {
            if (!BulkQuestionParser.containsUrdu(tok)) {
                outTokens.add(tok)
                continue
            }
            if (tok in stopWords) {
                if (tok == "اور") outTokens.add("and")
                else if (tok == "یا") outTokens.add("or")
                else if (tok == "میں") outTokens.add("in")
                continue
            }
            val mapped = exactOptionsUrToEn[normalizeKey(tok)]
                ?: wordGlossaryUrToEn[tok]
                ?: transliterateUrduWordToLatin(tok)
            if (mapped.isNotBlank()) {
                outTokens.add(mapped)
            }
        }

        val joined = outTokens
            .joinToString(" ")
            .replace(Regex("\\b(\\w+)\\s+\\1\\b", RegexOption.IGNORE_CASE), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()

        val result = when {
            joined.isNotBlank() -> joined
            !parenEnglish.isNullOrBlank() -> parenEnglish
            else -> transliterateUrduWordToLatin(raw)
        }

        val capitalized = if (result.firstOrNull()?.isDigit() == true) {
            result
        } else {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return if (isQuestion && !capitalized.endsWith(".") && !capitalized.endsWith("?") && !capitalized.endsWith(":")) {
            if (raw.contains("؟") || raw.contains("کیوں") || raw.contains("کیسے") || raw.contains("کہاں") || raw.contains("کیا")) {
                "$capitalized?"
            } else {
                "$capitalized."
            }
        } else {
            capitalized
        }
    }

    private fun transliterateUrduWordToLatin(urduWord: String): String {
        val charMap = mapOf(
            'ا' to "a", 'آ' to "aa", 'ب' to "b", 'پ' to "p", 'ت' to "t", 'ٹ' to "t",
            'ث' to "s", 'ج' to "j", 'چ' to "ch", 'ح' to "h", 'خ' to "kh", 'د' to "d",
            'ڈ' to "d", 'ذ' to "z", 'ر' to "r", 'ڑ' to "r", 'ز' to "z", 'ژ' to "zh",
            'س' to "s", 'ش' to "sh", 'ص' to "s", 'ض' to "z", 'ط' to "t", 'ظ' to "z",
            'ع' to "a", 'غ' to "gh", 'ف' to "f", 'ق' to "q", 'ک' to "k", 'ك' to "k",
            'گ' to "g", 'ل' to "l", 'م' to "m", 'ن' to "n", 'ں' to "n", 'و' to "o",
            'ہ' to "h", 'ھ' to "h", 'ۃ' to "h", 'ة' to "h", 'ء' to "", 'ئ' to "i",
            'ی' to "i", 'ي' to "i", 'ے' to "e",
            '۰' to "0", '۱' to "1", '۲' to "2", '۳' to "3", '۴' to "4",
            '۵' to "5", '۶' to "6", '۷' to "7", '۸' to "8", '۹' to "9"
        )
        val sb = StringBuilder()
        for (ch in urduWord) {
            when {
                ch in charMap -> sb.append(charMap[ch])
                ch.code in 0x064B..0x065F || ch.code == 0x0670 -> {} // ignore diacritics
                !BulkQuestionParser.containsUrdu(ch.toString()) -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    // ========================================================================
    // 4. ENGLISH -> URDU OFFLINE TRANSLATION
    // ========================================================================

    /**
     * Synchronously translates an English question or MCQ option into Urdu script.
     * Works 100% offline with zero latency and guarantees clean Urdu script output.
     */
    fun translateEnglishToUrduOffline(rawEnglish: String, isOption: Boolean = false): String {
        val cleaned = if (isOption) {
            BulkQuestionParser.cleanOptionPrefix(rawEnglish)
        } else {
            stripLeadingQuestionNumber(rawEnglish)
        }.trim()

        if (cleaned.isBlank()) return ""
        if (BulkQuestionParser.containsUrdu(cleaned) && !BulkQuestionParser.containsEnglish(cleaned)) {
            return cleaned
        }

        val normKey = normalizeEnKey(cleaned)
        if (isOption) {
            exactOptionsEnToUr[normKey]?.let { return it }
            exactQuestionsEnToUr[normKey]?.let { return it }
        } else {
            exactQuestionsEnToUr[normKey]?.let { return it }
            exactOptionsEnToUr[normKey]?.let { return it }
        }

        if (isOption) {
            return translateEnglishClauseOrTermToUrdu(cleaned, isQuestion = false)
        }

        return translateEnglishSentenceRuleBased(cleaned)
    }

    private fun translateEnglishSentenceRuleBased(english: String): String {
        val s = stripLeadingQuestionNumber(english)
            .removeSuffix("?")
            .removeSuffix(".")
            .removeSuffix(":")
            .trim()

        // 1. "Define X and state/write/explain its importance"
        Regex("^define\\s+(.+?)\\s+and\\s+(?:state|write|explain|describe)\\s+(?:its\\s+)?importance$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی تعریف لکھیں اور اہمیت بیان کریں۔"
            }

        // 2. "Define X and write the names of Y"
        Regex("^define\\s+(.+?)\\s+and\\s+write\\s+(?:the\\s+)?names\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                val sub = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return "$topic کی تعریف لکھیں اور $sub کے نام لکھیں۔"
            }

        // 3. "Define X and give/write its example(s)"
        Regex("^define\\s+(.+?)\\s+and\\s+(?:give|write)\\s+(?:its\\s+|an\\s+)?examples?$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی تعریف لکھیں اور مثال دیں۔"
            }

        // 4. "Define X"
        Regex("^define\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی تعریف لکھیں۔"
            }

        // 5. "What is the difference between X and Y"
        Regex("^what\\s+is\\s+the\\s+difference\\s+between\\s+(.+?)\\s+and\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val t1 = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                val t2 = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return "$t1 اور $t2 میں کیا فرق ہے؟"
            }

        // 6. "Differentiate between X and Y" / "Write the difference between X and Y"
        Regex("^(?:differentiate|distinguish|write\\s+the\\s+difference)\\s+between\\s+(.+?)\\s+and\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val t1 = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                val t2 = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return "$t1 اور $t2 میں فرق لکھیں۔"
            }

        // 7. "What is meant by X" / "What do you mean by X"
        Regex("^what\\s+(?:is\\s+meant|do\\s+you\\s+mean)\\s+by\\s+(.+?)(?:\\?\\s*(.+))?$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                val rest = m.groupValues[2].trim()
                return if (rest.isNotBlank()) {
                    val restUr = translateEnglishSentenceRuleBased(rest)
                    "$topic سے کیا مراد ہے؟ $restUr"
                } else {
                    "$topic سے کیا مراد ہے؟"
                }
            }

        // 8. "Write a (detailed/comprehensive/short/brief) note on X"
        Regex("^write\\s+a\\s+(?:detailed\\s+|comprehensive\\s+|brief\\s+|short\\s+)?note\\s+on\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic پر ایک مفصل نوٹ تحریر کریں۔"
            }

        // 9. "Write the summary of X"
        Regex("^write\\s+(?:the\\s+)?summary\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کا خلاصہ لکھیں۔"
            }

        // 10. "Describe/Explain the structure and function of X"
        Regex("^(?:describe|explain|write)\\s+the\\s+structure\\s+and\\s+function\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی ساخت اور فعل تحریر کیجیے۔"
            }

        // 11. "Explain/Describe the structure of X with the help of a (labelled) diagram"
        Regex("^(?:explain|describe)\\s+the\\s+structure\\s+of\\s+(.+?)\\s+with\\s+the\\s+help\\s+of\\s+a\\s+(?:labelled\\s+)?diagram$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی ساخت لیبل شدہ ڈایا گرام کی مدد سے واضح کریں۔"
            }

        // 12. "Describe/Explain the structure of X"
        Regex("^(?:describe|explain)\\s+the\\s+structure\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کی ساخت بیان کریں۔"
            }

        // 13. "What is the (approximate )?length of (the )?X"
        Regex("^what\\s+is\\s+the\\s+(approximate\\s+)?length\\s+of\\s+(?:the\\s+)?(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val isApprox = m.groupValues[1].isNotBlank()
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return if (isApprox) {
                    "$topic کی لمبائی تقریباً کتنی ہوتی ہے؟"
                } else {
                    "$topic کی لمبائی کتنی ہوتی ہے؟"
                }
            }

        // 14. "What is the (SI )?unit of X"
        Regex("^what\\s+is\\s+the\\s+(si\\s+)?unit\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return "$topic کا یونٹ کیا ہے؟"
            }

        // 15. "What is the formula of X"
        Regex("^what\\s+is\\s+the\\s+(?:chemical\\s+)?formula\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کا فارمولا کیا ہے؟"
            }

        // 16. "What is the function/role/benefit of X" / "What role does X play"
        Regex("^(?:what\\s+is\\s+the\\s+(?:function(?:\\s+and\\s+role)?|role|benefit|importance)\\s+of\\s+(.+)|what\\s+role\\s+does\\s+(?:the\\s+)?(.+?)\\s+play)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val rawTopic = m.groupValues[1].ifBlank { m.groupValues[2] }
                val topic = translateEnglishClauseOrTermToUrdu(rawTopic, isQuestion = false)
                return "$topic کیا کردار ادا کرتا ہے؟"
            }

        // 17. "Compare X"
        Regex("^compare\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                return "$topic کا موازنہ کیجیے۔"
            }

        // 18. "State and explain X" / "State X" / "Explain X" / "Describe X"
        Regex("^(state\\s+and\\s+explain|state|explain|describe)\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val verb = m.groupValues[1].lowercase()
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return when {
                    verb.startsWith("state and") -> "$topic بیان کریں اور وضاحت کریں۔"
                    verb == "state" -> "$topic بیان کریں۔"
                    else -> "$topic کی وضاحت کریں۔"
                }
            }

        // 19. "Write (any )?(two|three|four) (uses|functions|properties|characteristics|examples) of X"
        Regex("^write\\s+(?:any\\s+|down\\s+)?(two|three|four|five|\\d+)\\s+(uses|functions|properties|characteristics|examples|types|advantages)\\s+of\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val countUr = translateEnglishClauseOrTermToUrdu(m.groupValues[1], isQuestion = false)
                val propUr = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                val topicUr = translateEnglishClauseOrTermToUrdu(m.groupValues[3], isQuestion = false)
                return "$topicUr کے $countUr $propUr تحریر کریں۔"
            }

        // 20. "What is/are X"
        Regex("^what\\s+(is|are)\\s+(?:a\\s+|an\\s+|the\\s+)?(.+)$", RegexOption.IGNORE_CASE)
            .find(s)?.let { m ->
                val isPlural = m.groupValues[1].equals("are", ignoreCase = true)
                val topic = translateEnglishClauseOrTermToUrdu(m.groupValues[2], isQuestion = false)
                return if (isPlural) "$topic کیا ہیں؟" else "$topic کیا ہے؟"
            }

        return translateEnglishClauseOrTermToUrdu(s, isQuestion = true)
    }

    private fun translateEnglishClauseOrTermToUrdu(raw: String, isQuestion: Boolean): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        // Pure number check (e.g. "12", "9.8", "3.14")
        if (trimmed.matches(Regex("^[+\\-]?\\d+(?:\\.\\d+)?%?\$"))) {
            return trimmed
        }

        // Numeric + SI unit check (e.g. "10 cm", "12 cm", "98 g/mol")
        Regex("^([+\\-]?\\d+(?:\\.\\d+)?)\\s*([A-Za-z°³/0-9]+)\$").find(trimmed)?.let { m ->
            val num = m.groupValues[1]
            val unitRaw = m.groupValues[2].lowercase()
            val unitUr = unitEnToUrMap[unitRaw]
            if (unitUr != null) {
                return "$num $unitUr"
            }
        }

        var working = " ${trimmed.lowercase()} "
        // Replace multi-word English phrases with Urdu phrases first
        for ((enPhrase, urPhrase) in phraseGlossaryEnToUr) {
            if (working.contains(enPhrase)) {
                working = working.replace(enPhrase, " $urPhrase ")
            }
        }

        val stopWords = setOf(
            "the", "a", "an", "of", "to", "in", "on", "at", "by", "for", "with", "from",
            "is", "are", "was", "were", "be", "been", "being", "do", "does", "did",
            "has", "have", "had", "that", "this", "these", "those", "it", "its"
        )

        val tokens = working.trim()
            .split(' ', '\t', '\n', '\r', ',', ';', ':', '?', '.', '!', '(', ')', '[', ']', '"', '\'')
            .filter { it.isNotBlank() }
        val outTokens = mutableListOf<String>()

        for (tok in tokens) {
            if (BulkQuestionParser.containsUrdu(tok)) {
                outTokens.add(tok)
                continue
            }
            if (tok.matches(Regex("^\\d+(?:\\.\\d+)?\$"))) {
                outTokens.add(tok)
                continue
            }
            val lower = tok.lowercase()
            if (lower in stopWords) {
                when (lower) {
                    "in" -> outTokens.add("میں")
                    "from" -> outTokens.add("سے")
                    "of" -> outTokens.add("کا")
                }
                continue
            }
            if (lower == "and") {
                outTokens.add("اور")
                continue
            }
            if (lower == "or") {
                outTokens.add("یا")
                continue
            }
            val mapped = unitEnToUrMap[lower]
                ?: exactOptionsEnToUr[normalizeEnKey(lower)]
                ?: wordGlossaryEnToUr[lower]
                ?: transliterateEnglishWordToUrdu(lower)
            if (mapped.isNotBlank()) {
                outTokens.add(mapped)
            }
        }

        val joined = outTokens
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val result = joined.ifBlank { transliterateEnglishWordToUrdu(trimmed) }
        return if (isQuestion && !result.endsWith("۔") && !result.endsWith("؟") && !result.endsWith(":")) {
            if (raw.trim().endsWith("?") || raw.lowercase().startsWith("what") || raw.lowercase().startsWith("why") || raw.lowercase().startsWith("how") || raw.lowercase().startsWith("which") || raw.lowercase().startsWith("where") || raw.lowercase().startsWith("who")) {
                "$result؟"
            } else {
                "$result۔"
            }
        } else {
            result
        }
    }

    /**
     * Converts any remaining English/scientific word into phonetic Urdu script
     * so Urdu Medium papers are always rendered 100% in Right-to-Left Urdu script.
     */
    private fun transliterateEnglishWordToUrdu(englishWord: String): String {
        var w = englishWord.lowercase().trim()
        if (w.isBlank()) return ""
        if (BulkQuestionParser.containsUrdu(w)) return w

        val digraphs = listOf(
            "tion" to "شن",
            "sion" to "ژن",
            "ph" to "ف",
            "sh" to "ش",
            "ch" to "چ",
            "kh" to "خ",
            "gh" to "غ",
            "th" to "تھ",
            "ck" to "ک",
            "qu" to "کو",
            "oo" to "و",
            "ee" to "ی",
            "ea" to "ی",
            "ai" to "ے",
            "ay" to "ے",
            "ou" to "اؤ",
            "ow" to "و"
        )
        for ((enSeq, urSeq) in digraphs) {
            w = w.replace(enSeq, urSeq)
        }

        val sb = StringBuilder()
        for (i in w.indices) {
            val ch = w[i]
            if (BulkQuestionParser.containsUrdu(ch.toString()) || ch.isDigit()) {
                sb.append(ch)
                continue
            }
            when (ch) {
                'a' -> sb.append(if (i == 0) "ا" else "ا")
                'b' -> sb.append("ب")
                'c' -> sb.append("ک")
                'd' -> sb.append("ڈ")
                'e' -> sb.append(if (i == 0) "ای" else if (i == w.lastIndex) "ہ" else "ی")
                'f' -> sb.append("ف")
                'g' -> sb.append("گ")
                'h' -> sb.append("ہ")
                'i' -> sb.append(if (i == 0) "آئی" else "ی")
                'j' -> sb.append("ج")
                'k' -> sb.append("ک")
                'l' -> sb.append("ل")
                'm' -> sb.append("م")
                'n' -> sb.append("ن")
                'o' -> sb.append(if (i == 0) "او" else "و")
                'p' -> sb.append("پ")
                'q' -> sb.append("ق")
                'r' -> sb.append("ر")
                's' -> sb.append("س")
                't' -> sb.append("ٹ")
                'u' -> sb.append(if (i == 0) "یو" else "و")
                'v' -> sb.append("و")
                'w' -> sb.append("و")
                'x' -> sb.append("کس")
                'y' -> sb.append("ی")
                'z' -> sb.append("ز")
            }
        }
        return sb.toString().ifBlank { "سوال" }
    }

    // ========================================================================
    // 5. BI-DIRECTIONAL ENTITY ENRICHMENT (OFFLINE & ONLINE SUSPEND)
    // ========================================================================

    /**
     * Synchronously fills in missing English (from Urdu) AND missing Urdu (from English)
     * on a [QuestionEntity] so every question works seamlessly in English Medium, Urdu Medium,
     * and Bilingual modes regardless of which single language the admin uploaded.
     */
    fun autoFillMissingLanguage(q: QuestionEntity): QuestionEntity {
        val rawEn = q.resolvedQuestionEn()
        val rawUr = q.resolvedQuestionUr()

        val cleanEn = when {
            rawEn.isNotBlank() && !BulkQuestionParser.containsUrdu(rawEn) -> rawEn
            rawUr.isNotBlank() -> translateUrduToEnglishOffline(rawUr, isOption = false)
            else -> q.questionEn
        }

        val cleanUr = when {
            rawUr.isNotBlank() && BulkQuestionParser.containsUrdu(rawUr) -> rawUr
            cleanEn.isNotBlank() -> translateEnglishToUrduOffline(cleanEn, isOption = false)
            else -> q.questionUr
        }

        if (q.type != QuestionType.MCQ.code) {
            return q.copy(
                questionEn = cleanEn,
                questionUr = cleanUr
            )
        }

        val optAEn = q.resolvedOptionAEn().takeIf { it.isNotBlank() && !BulkQuestionParser.containsUrdu(it) }
            ?: translateUrduToEnglishOffline(q.optionAUr.ifBlank { q.optionAEn }, isOption = true)
        val optBEn = q.resolvedOptionBEn().takeIf { it.isNotBlank() && !BulkQuestionParser.containsUrdu(it) }
            ?: translateUrduToEnglishOffline(q.optionBUr.ifBlank { q.optionBEn }, isOption = true)
        val optCEn = q.resolvedOptionCEn().takeIf { it.isNotBlank() && !BulkQuestionParser.containsUrdu(it) }
            ?: translateUrduToEnglishOffline(q.optionCUr.ifBlank { q.optionCEn }, isOption = true)
        val optDEn = q.resolvedOptionDEn().takeIf { it.isNotBlank() && !BulkQuestionParser.containsUrdu(it) }
            ?: translateUrduToEnglishOffline(q.optionDUr.ifBlank { q.optionDEn }, isOption = true)

        val optAUr = q.resolvedOptionAUr().takeIf { it.isNotBlank() && BulkQuestionParser.containsUrdu(it) }
            ?: translateEnglishToUrduOffline(optAEn, isOption = true)
        val optBUr = q.resolvedOptionBUr().takeIf { it.isNotBlank() && BulkQuestionParser.containsUrdu(it) }
            ?: translateEnglishToUrduOffline(optBEn, isOption = true)
        val optCUr = q.resolvedOptionCUr().takeIf { it.isNotBlank() && BulkQuestionParser.containsUrdu(it) }
            ?: translateEnglishToUrduOffline(optCEn, isOption = true)
        val optDUr = q.resolvedOptionDUr().takeIf { it.isNotBlank() && BulkQuestionParser.containsUrdu(it) }
            ?: translateEnglishToUrduOffline(optDEn, isOption = true)

        return q.copy(
            questionEn = cleanEn,
            questionUr = cleanUr,
            optionAEn = optAEn,
            optionBEn = optBEn,
            optionCEn = optCEn,
            optionDEn = optDEn,
            optionAUr = optAUr,
            optionBUr = optBUr,
            optionCUr = optCUr,
            optionDUr = optDUr
        )
    }

    suspend fun translateUrduToEnglishSuspend(rawUrdu: String, isOption: Boolean = false): String {
        val cleaned = if (isOption) {
            BulkQuestionParser.cleanOptionPrefix(rawUrdu)
        } else {
            stripLeadingQuestionNumber(rawUrdu)
        }.trim()
        if (cleaned.isBlank()) return ""
        if (!BulkQuestionParser.containsUrdu(cleaned)) return cleaned

        val normKey = normalizeKey(cleaned)
        if (isOption) {
            exactOptionsUrToEn[normKey]?.let { return it }
            exactQuestionsUrToEn[normKey]?.let { return it }
        } else {
            exactQuestionsUrToEn[normKey]?.let { return it }
            exactOptionsUrToEn[normKey]?.let { return it }
        }

        val online = fetchOnlineTranslation(cleaned, sourceLang = "ur", targetLang = "en")
        if (!online.isNullOrBlank() && !BulkQuestionParser.containsUrdu(online)) {
            return online
        }
        return translateUrduToEnglishOffline(cleaned, isOption = isOption)
    }

    suspend fun translateEnglishToUrduSuspend(rawEnglish: String, isOption: Boolean = false): String {
        val cleaned = if (isOption) {
            BulkQuestionParser.cleanOptionPrefix(rawEnglish)
        } else {
            stripLeadingQuestionNumber(rawEnglish)
        }.trim()
        if (cleaned.isBlank()) return ""
        if (BulkQuestionParser.containsUrdu(cleaned) && !BulkQuestionParser.containsEnglish(cleaned)) {
            return cleaned
        }

        val normKey = normalizeEnKey(cleaned)
        if (isOption) {
            exactOptionsEnToUr[normKey]?.let { return it }
            exactQuestionsEnToUr[normKey]?.let { return it }
        } else {
            exactQuestionsEnToUr[normKey]?.let { return it }
            exactOptionsEnToUr[normKey]?.let { return it }
        }

        val online = fetchOnlineTranslation(cleaned, sourceLang = "en", targetLang = "ur")
        if (!online.isNullOrBlank() && BulkQuestionParser.containsUrdu(online)) {
            return BulkQuestionParser.stripEnglishGlossFromUrdu(online)
        }
        return translateEnglishToUrduOffline(cleaned, isOption = isOption)
    }

    suspend fun autoTranslateQuestionsSuspend(questions: List<QuestionEntity>): List<QuestionEntity> =
        withContext(Dispatchers.IO) {
            questions.map { q ->
                val base = autoFillMissingLanguage(q)
                val urClean = base.questionUr.trim()
                val enClean = base.questionEn.trim()
                val normUrKey = normalizeKey(urClean)
                val normEnKeyVal = normalizeEnKey(enClean)

                val isExactMatch = exactQuestionsUrToEn.containsKey(normUrKey) ||
                    exactQuestionsEnToUr.containsKey(normEnKeyVal)

                if (isExactMatch) {
                    base
                } else {
                    val wasEnAutoFromUr = urClean.isNotBlank() &&
                        enClean == translateUrduToEnglishOffline(urClean, isOption = false)
                    val wasUrAutoFromEn = enClean.isNotBlank() &&
                        urClean == translateEnglishToUrduOffline(enClean, isOption = false)

                    when {
                        wasEnAutoFromUr -> {
                            val liveQEn = translateUrduToEnglishSuspend(urClean, isOption = false).ifBlank { base.questionEn }
                            if (base.type == QuestionType.MCQ.code) {
                                base.copy(
                                    questionEn = liveQEn,
                                    optionAEn = translateUrduToEnglishSuspend(base.optionAUr, isOption = true).ifBlank { base.optionAEn },
                                    optionBEn = translateUrduToEnglishSuspend(base.optionBUr, isOption = true).ifBlank { base.optionBEn },
                                    optionCEn = translateUrduToEnglishSuspend(base.optionCUr, isOption = true).ifBlank { base.optionCEn },
                                    optionDEn = translateUrduToEnglishSuspend(base.optionDUr, isOption = true).ifBlank { base.optionDEn }
                                )
                            } else {
                                base.copy(questionEn = liveQEn)
                            }
                        }
                        wasUrAutoFromEn -> {
                            val liveQUr = translateEnglishToUrduSuspend(enClean, isOption = false).ifBlank { base.questionUr }
                            if (base.type == QuestionType.MCQ.code) {
                                base.copy(
                                    questionUr = liveQUr,
                                    optionAUr = translateEnglishToUrduSuspend(base.optionAEn, isOption = true).ifBlank { base.optionAUr },
                                    optionBUr = translateEnglishToUrduSuspend(base.optionBEn, isOption = true).ifBlank { base.optionBUr },
                                    optionCUr = translateEnglishToUrduSuspend(base.optionCEn, isOption = true).ifBlank { base.optionCUr },
                                    optionDUr = translateEnglishToUrduSuspend(base.optionDEn, isOption = true).ifBlank { base.optionDUr }
                                )
                            } else {
                                base.copy(questionUr = liveQUr)
                            }
                        }
                        else -> base
                    }
                }
            }
        }

    private suspend fun fetchOnlineTranslation(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val outer = JSONArray(body)
                val sentences = outer.optJSONArray(0) ?: return@withContext null
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val seg = sentences.optJSONArray(i)
                    val part = seg?.optString(0).orEmpty()
                    sb.append(part)
                }
                sb.toString().trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
    }
}
